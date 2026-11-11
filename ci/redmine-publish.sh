#!/bin/sh
# Publishes build outputs to a Redmine project, so the team downloads them where they already work
# (Redmine and Redmine Board: project → Files / Roadmap / News; Board → Apps). Works for any app:
# it only looks at the files in PUBLISH_DIR, named <slot>-<version>.<ext> (e.g. luna-prod-1.2.0.apk).
#
# Release (tag vX.Y.Z or <client>-vX.Y.Z) → a Redmine version named after the tag: files attached,
#   version closed (Roadmap shows it released), one News entry with the changelog, and a note on
#   the issues of THIS project that the commits mention (#123).
# Anything else → the "Nightly" version: each file replaces the previous build of the same slot.
#
# What it never does: touch versions it did not create (only "[CI]" ones are reopened or closed),
# delete a file it did not publish (only "Nightly" files whose description names the same slot),
# write to issues of other projects, or delete anything before the new file is safely in.
# Safe to re-run: published files, the News entry and issue notes are not duplicated.
#
# Needs: curl, jq, git.  Variables:
#   REDMINE_API_KEY   key of a CI user that is a member of the project only where it publishes (role
#                     with "Manage files", "Manage versions", "Manage news", "View issues", "Add notes")
#   REDMINE_PROJECT   project identifier, e.g. zebra-scanner
#   REDMINE_URL       default https://projects.solutions-apps.com
#   PUBLISH_DIR       default dist
#   APP_NAME          shown in titles, default CI_PROJECT_TITLE
#   APP_VERSION_NAME  the version in the file names
set -eu
set -o pipefail 2>/dev/null || true

REDMINE_URL="${REDMINE_URL:-https://projects.solutions-apps.com}"
PUBLISH_DIR="${PUBLISH_DIR:-dist}"
APP_NAME="${APP_NAME:-${CI_PROJECT_TITLE:-App}}"
TAG="${CI_COMMIT_TAG:-}"
SHA="${CI_COMMIT_SHORT_SHA:-$(git rev-parse --short HEAD 2>/dev/null || echo unknown)}"
CI_MARK="[CI]"          # description prefix of the versions this script owns
MAX_ISSUE_NOTES=50

fail() { echo "ERROR: $*" >&2; exit 1; }
VERSION="${APP_VERSION_NAME:-}"
[ -n "$VERSION" ] || fail "APP_VERSION_NAME is not set"
[ -n "${REDMINE_API_KEY:-}" ] || fail "REDMINE_API_KEY is not set (GitLab → Settings → CI/CD → Variables, masked)"
[ -n "${REDMINE_PROJECT:-}" ] || fail "REDMINE_PROJECT is not set (the Redmine project identifier)"
for tool in curl jq git; do command -v "$tool" >/dev/null || fail "$tool is not installed"; done
case "$TAG" in
  ''|v[0-9]*|*-v[0-9]*) ;;
  *) fail "tag '$TAG' is not vX.Y.Z or <client>-vX.Y.Z" ;;
esac

# Only regular files with the version in their name; anything else in PUBLISH_DIR is a mistake
FILES=""
for f in "$PUBLISH_DIR"/*; do
  [ -f "$f" ] || continue
  case "$(basename "$f")" in
    *-"$VERSION".*) FILES="$FILES $f" ;;
    *) echo "! $(basename "$f") does not end in -$VERSION.<ext>; not published" ;;
  esac
done
[ -n "$FILES" ] || fail "nothing to publish in $PUBLISH_DIR/ (expected <slot>-$VERSION.<ext>)"

CURL="curl -sS --connect-timeout 20 --retry 3 --retry-delay 5"

# api METHOD PATH [JSON] → response body on 2xx; otherwise stops with Redmine's own message.
# GETs are retried on network errors; writes are not, so nothing is done twice.
api() {
  if [ "$1" = GET ]; then c="$CURL --max-time 60"; else c="curl -sS --connect-timeout 20 --max-time 120"; fi
  out=$($c -w '\n%{http_code}' -X "$1" "$REDMINE_URL$2" \
    -H "X-Redmine-API-Key: $REDMINE_API_KEY" -H 'Content-Type: application/json' ${3:+--data "$3"}) \
    || fail "$1 $2: cannot reach $REDMINE_URL"
  code=$(printf '%s' "$out" | tail -n1)
  body=$(printf '%s' "$out" | sed '$d')
  case "$code" in
    2??) printf '%s' "$body" ;;
    401) fail "$1 $2 → 401: REDMINE_API_KEY is wrong, or the REST API is off (Administration → Settings → API)" ;;
    403) fail "$1 $2 → 403: the CI user lacks a permission in project $REDMINE_PROJECT" ;;
    404) fail "$1 $2 → 404: not found (project '$REDMINE_PROJECT', or its Files/News module is off)" ;;
    *)   fail "$1 $2 → $code: $(printf '%s' "$body" | head -c 400)" ;;
  esac
}

project=$(api GET "/projects/$REDMINE_PROJECT.json?include=enabled_modules")
PROJECT_ID=$(printf '%s' "$project" | jq -r '.project.id // empty')
[ -n "$PROJECT_ID" ] || fail "project '$REDMINE_PROJECT' gave no id"
modules=$(printf '%s' "$project" | jq -r '[.project.enabled_modules[]?.name] | join(" ")')
case " $modules " in *" files "*) ;; *) fail "the Files module is off in project $REDMINE_PROJECT (Settings → Modules)" ;; esac
echo "Redmine project: $(printf '%s' "$project" | jq -r .project.name) (#$PROJECT_ID)"

# ---------- version ----------
if [ -n "$TAG" ]; then VERSION_NAME="$TAG"; else VERSION_NAME="Nightly"; fi
version=$(api GET "/projects/$PROJECT_ID/versions.json" \
  | jq -c --arg n "$VERSION_NAME" --argjson p "$PROJECT_ID" '[.versions[] | select(.name == $n and .project.id == $p)][0] // empty')
if [ -z "$version" ]; then
  if [ -n "$TAG" ]; then desc="$CI_MARK $APP_NAME $VERSION"; else desc="$CI_MARK Latest build of each branch push, replaced on every build"; fi
  version=$(api POST "/projects/$PROJECT_ID/versions.json" \
    "$(jq -n --arg n "$VERSION_NAME" --arg d "$desc" '{version: {name: $n, description: $d, sharing: "none"}}')" | jq -c .version)
  echo "Created version $VERSION_NAME"
fi
VERSION_ID=$(printf '%s' "$version" | jq -r .id)
OWNED=$(printf '%s' "$version" | jq -r --arg m "$CI_MARK" '(.description // "") | startswith($m)')
[ "$OWNED" = true ] || echo "Note: version $VERSION_NAME was made by a person; files are added, its status is left alone"
STATUS=$(printf '%s' "$version" | jq -r '.status // "open"')
if [ "$STATUS" != open ]; then
  [ "$OWNED" = true ] || fail "version $VERSION_NAME is $STATUS and not ours; refusing to change it"
  api PUT "/versions/$VERSION_ID.json" '{"version":{"status":"open"}}' >/dev/null   # files need an open version
fi

files_of_version() {
  api GET "/projects/$PROJECT_ID/files.json" | jq -c --argjson v "$VERSION_ID" '[.files[] | select(.version.id? == $v)]'
}
existing=$(files_of_version)

# ---------- files ----------
published=0
for f in $FILES; do
  name=$(basename "$f")
  slot="${name%-"$VERSION".*}"
  ext="${name##*.}"

  if printf '%s' "$existing" | jq -e --arg n "$name" 'any(.[]; .filename == $n)' >/dev/null; then
    echo "= $name already published"
    continue
  fi

  up=$(curl -sS --connect-timeout 20 --max-time 900 -w '\n%{http_code}' -X POST \
    "$REDMINE_URL/uploads.json?filename=$(printf '%s' "$name" | jq -sRr @uri)" \
    -H "X-Redmine-API-Key: $REDMINE_API_KEY" -H 'Content-Type: application/octet-stream' \
    --data-binary "@$f") || fail "upload of $name: cannot reach $REDMINE_URL"
  code=$(printf '%s' "$up" | tail -n1)
  case "$code" in
    201) token=$(printf '%s' "$up" | sed '$d' | jq -r .upload.token) ;;
    413|422) fail "upload of $name refused ($code): $(du -h "$f" | cut -f1) is over Redmine's limit (Administration → Settings → Files → Maximum attachment size)" ;;
    *) fail "upload of $name → $code: $(printf '%s' "$up" | sed '$d' | head -c 400)" ;;
  esac

  # "slot · version · ext · commit sha": Redmine Board and customer links read this
  api POST "/projects/$PROJECT_ID/files.json" "$(jq -n --arg t "$token" --arg n "$name" --argjson v "$VERSION_ID" \
    --arg d "$slot · $VERSION · $ext · commit $SHA" \
    '{file: {token: $t, filename: $n, version_id: $v, description: $d}}')" >/dev/null
  echo "+ $name"
  published=$((published + 1))

  # Nightly: now that the new build is in, remove older builds of exactly this slot
  if [ -z "$TAG" ]; then
    files_of_version | jq -r --arg s "$slot" --arg n "$name" \
      '.[] | select(.filename != $n and ((.description // "") | split(" · ")[0]) == $s) | .id' \
    | while read -r old; do
        api DELETE "/attachments/$old.json" >/dev/null && echo "- older $slot build (#$old)"
      done
  fi
done

if [ -z "$TAG" ]; then
  echo "Nightly: $published new file(s) → $REDMINE_URL/projects/$REDMINE_PROJECT/files"
  exit 0
fi

# ---------- release only ----------
TITLE="$APP_NAME $VERSION_NAME"
news_exists() {
  case " $modules " in *" news "*) ;; *) return 1 ;; esac
  api GET "/projects/$PROJECT_ID/news.json?limit=100" | jq -e --arg t "$TITLE" 'any(.news[]; .title == $t)' >/dev/null
}

if news_exists; then
  echo "News '$TITLE' already exists: changelog and issue notes were done by an earlier run"
else
  # The previous tag of the same kind: luna-v1.2.0 after luna-v1.1.0, v1.2.0 after v1.1.0
  case "$TAG" in *-v[0-9]*) pattern="${TAG%%-v*}-v[0-9]*" ;; *) pattern='v[0-9]*' ;; esac
  PREV=$(git describe --tags --abbrev=0 --match "$pattern" "$TAG^" 2>/dev/null || true)
  RANGE="${PREV:+$PREV..}$TAG"
  CHANGES=$(git log --no-merges --format='* %s' "$RANGE" 2>/dev/null | head -60 || true)

  # Issue notes only on open-or-closed issues of THIS project: "#12" in a commit may be a GitLab
  # issue or another project's number, and those are left alone.
  ISSUES=$(git log --no-merges --format='%s%n%b' "$RANGE" 2>/dev/null | grep -oE '(^|[^&A-Za-z0-9!])#[0-9]+' | grep -oE '[0-9]+' | sort -un | head -n "$MAX_ISSUE_NOTES" || true)
  for id in $ISSUES; do
    issue_project=$(curl -sS --max-time 30 -H "X-Redmine-API-Key: $REDMINE_API_KEY" "$REDMINE_URL/issues/$id.json" 2>/dev/null \
      | jq -r '.issue.project.id // empty' 2>/dev/null || true)
    if [ "$issue_project" != "$PROJECT_ID" ]; then
      echo "  #$id is not an issue of this project; left alone"
      continue
    fi
    note="Released in *$APP_NAME $VERSION_NAME* (\"files\":$REDMINE_URL/projects/$REDMINE_PROJECT/files)"
    if curl -sS -f -o /dev/null --max-time 30 -X PUT "$REDMINE_URL/issues/$id.json" \
        -H "X-Redmine-API-Key: $REDMINE_API_KEY" -H 'Content-Type: application/json' \
        --data "$(jq -n --arg n "$note" '{issue: {notes: $n}}')"; then
      echo "  noted #$id"
    else
      echo "  could not note #$id (workflow or permission); continuing"
    fi
  done

  case " $modules " in
    *" news "*)
      LINKS=$(files_of_version | jq -r '.[] | "* \"\(.filename)\":\(.content_url)"')
      BODY=$(printf 'h3. Downloads\n\n%s\n\nh3. Changes%s\n\n%s\n' "$LINKS" "${PREV:+ since $PREV}" "${CHANGES:-* (no commits)}")
      api POST "/projects/$PROJECT_ID/news.json" "$(jq -n --arg t "$TITLE" --arg s "Version $VERSION, commit $SHA" --arg d "$BODY" \
        '{news: {title: $t, summary: $s, description: $d}}')" >/dev/null
      echo "News posted: $TITLE" ;;
    *) echo "News module is off in this project; no News entry" ;;
  esac
fi

# Ours only: closed with today's date, so Roadmap lists it as released
if [ "$OWNED" = true ]; then
  api PUT "/versions/$VERSION_ID.json" "$(jq -n --arg d "$(date +%F)" '{version: {status: "closed", due_date: $d}}')" >/dev/null
fi
echo "Release published: $REDMINE_URL/versions/$VERSION_ID"
