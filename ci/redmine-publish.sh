#!/bin/sh
# Publishes build outputs to a Redmine project, so the team downloads them where they already work
# (Redmine and Redmine Board: project → Files / Roadmap / News). Works for any app: it only looks at
# the files in PUBLISH_DIR.
#
# Release (tag pipeline)  → Redmine version "v1.2.0": files attached to it, version closed (Roadmap
#                           shows it as released), one News entry with the changelog, and a note on
#                           every issue the commits mention (#123).
# Nightly (other pipeline) → Redmine version "Nightly": each file replaces the previous build of the
#                           same slot (e.g. luna-dev-*.apk), so it never piles up.
# Safe to re-run: existing release files, the News entry and issue notes are not duplicated.
#
# Needs: curl, jq, git.  Variables:
#   REDMINE_API_KEY   key of a CI user that is a member of the project (role with "Manage files",
#                     "Manage versions", "Manage news", "Add notes")          — required
#   REDMINE_PROJECT   project identifier, e.g. zebra-scanner                   — required
#   REDMINE_URL       default https://projects.solutions-apps.com
#   PUBLISH_DIR       default dist
#   APP_NAME          shown in titles, default CI_PROJECT_TITLE
#   APP_VERSION_NAME  version in file names; files are named <slot>-<version>.<ext>
set -eu

REDMINE_URL="${REDMINE_URL:-https://projects.solutions-apps.com}"
PUBLISH_DIR="${PUBLISH_DIR:-dist}"
APP_NAME="${APP_NAME:-${CI_PROJECT_TITLE:-App}}"
VERSION="${APP_VERSION_NAME:?APP_VERSION_NAME is not set}"
TAG="${CI_COMMIT_TAG:-}"
SHA="${CI_COMMIT_SHORT_SHA:-$(git rev-parse --short HEAD)}"

fail() { echo "ERROR: $*" >&2; exit 1; }
[ -n "${REDMINE_API_KEY:-}" ] || fail "REDMINE_API_KEY is not set (GitLab → Settings → CI/CD → Variables, masked)"
[ -n "${REDMINE_PROJECT:-}" ] || fail "REDMINE_PROJECT is not set (the Redmine project identifier)"
ls "$PUBLISH_DIR"/* >/dev/null 2>&1 || fail "nothing to publish in $PUBLISH_DIR/"

# api METHOD PATH [JSON]  → response body; fails with Redmine's own error text
api() {
  out=$(curl -sS -w '\n%{http_code}' -X "$1" "$REDMINE_URL$2" \
    -H "X-Redmine-API-Key: $REDMINE_API_KEY" -H 'Content-Type: application/json' ${3:+--data "$3"}) \
    || fail "cannot reach $REDMINE_URL"
  code=$(printf '%s' "$out" | tail -n1)
  body=$(printf '%s' "$out" | sed '$d')
  case "$code" in
    2??) printf '%s' "$body" ;;
    401) fail "$1 $2 → 401: REDMINE_API_KEY is wrong, or REST API is off (Administration → Settings → API)" ;;
    403) fail "$1 $2 → 403: the CI user lacks a permission in project $REDMINE_PROJECT" ;;
    404) fail "$1 $2 → 404: project '$REDMINE_PROJECT' not found, or its Files/News module is off" ;;
    *)   fail "$1 $2 → $code: $body" ;;
  esac
}

project=$(api GET "/projects/$REDMINE_PROJECT.json?include=enabled_modules")
PROJECT_ID=$(printf '%s' "$project" | jq -r .project.id)
printf '%s' "$project" | jq -e '.project.enabled_modules | map(.name) | index("files")' >/dev/null \
  || fail "the Files module is off in project $REDMINE_PROJECT (Settings → Modules)"
echo "Redmine project: $(printf '%s' "$project" | jq -r .project.name) (#$PROJECT_ID)"

# ---------- version ----------
if [ -n "$TAG" ]; then VERSION_NAME="v$VERSION"; else VERSION_NAME="Nightly"; fi
VERSION_ID=$(api GET "/projects/$PROJECT_ID/versions.json" \
  | jq -r --arg n "$VERSION_NAME" '[.versions[] | select(.name == $n and .project.id == '"$PROJECT_ID"')][0].id // empty')
if [ -z "$VERSION_ID" ]; then
  if [ -n "$TAG" ]; then desc="$APP_NAME $VERSION"; else desc="Latest build of every branch push; replaced on each build"; fi
  VERSION_ID=$(api POST "/projects/$PROJECT_ID/versions.json" \
    "$(jq -n --arg n "$VERSION_NAME" --arg d "$desc" '{version: {name: $n, description: $d, sharing: "none"}}')" \
    | jq -r .version.id)
  echo "Created version $VERSION_NAME (#$VERSION_ID)"
fi
# Nightly stays open while it is being written to; it is never closed
[ -n "$TAG" ] || api PUT "/versions/$VERSION_ID.json" '{"version":{"status":"open"}}' >/dev/null

existing=$(api GET "/projects/$PROJECT_ID/files.json" | jq -c --argjson v "$VERSION_ID" '[.files[] | select(.version.id == $v)]')

# ---------- files ----------
for f in "$PUBLISH_DIR"/*; do
  [ -f "$f" ] || continue
  name=$(basename "$f")
  ext="${name##*.}"
  slot="${name%-"$VERSION".*}"          # luna-dev-1.2.0-dev.7.apk → luna-dev
  [ "$slot" != "$name" ] || slot="${name%.*}"

  if printf '%s' "$existing" | jq -e --arg n "$name" 'map(.filename) | index($n)' >/dev/null; then
    echo "= $name already published, skipped"
    continue
  fi

  token=$(curl -sS -X POST "$REDMINE_URL/uploads.json?filename=$name" \
    -H "X-Redmine-API-Key: $REDMINE_API_KEY" -H 'Content-Type: application/octet-stream' \
    --data-binary "@$f" -w '\n%{http_code}')
  code=$(printf '%s' "$token" | tail -n1)
  case "$code" in
    201) token=$(printf '%s' "$token" | sed '$d' | jq -r .upload.token) ;;
    422) fail "upload of $name refused: $(du -h "$f" | cut -f1) is over Redmine's limit (Administration → Settings → Files → Maximum attachment size)" ;;
    *)   fail "upload of $name → $code: $(printf '%s' "$token" | sed '$d')" ;;
  esac

  # slot · version · commit — what people read in the Files list
  api POST "/projects/$PROJECT_ID/files.json" "$(jq -n --arg t "$token" --arg n "$name" --argjson v "$VERSION_ID" \
    --arg d "$slot · $VERSION · ${ext} · commit $SHA" \
    '{file: {token: $t, filename: $n, version_id: $v, description: $d}}')" >/dev/null
  echo "+ $name"

  # Nightly: drop the older builds of this slot now that the new one is in
  if [ -z "$TAG" ]; then
    printf '%s' "$existing" | jq -r --arg s "$slot-" --arg n "$name" \
      '.[] | select((.filename | startswith($s)) and .filename != $n) | .id' \
    | while read -r old; do api DELETE "/attachments/$old.json" >/dev/null && echo "- old build #$old"; done
  fi
done

[ -n "$TAG" ] || { echo "Nightly published: $REDMINE_URL/projects/$REDMINE_PROJECT/files"; exit 0; }

# ---------- release only: changelog, issues, news ----------
TITLE="$APP_NAME $VERSION_NAME released"
if api GET "/projects/$PROJECT_ID/news.json?limit=100" | jq -e --arg t "$TITLE" '.news | map(.title) | index($t)' >/dev/null; then
  echo "News '$TITLE' already exists; changelog and issue notes were done by an earlier run"
  exit 0
fi

PREV=$(git describe --tags --abbrev=0 "$TAG^" 2>/dev/null || true)
RANGE="${PREV:+$PREV..}$TAG"
CHANGES=$(git log --no-merges --format='* %s' "$RANGE" | head -60)
ISSUES=$(git log --format='%s %b' "$RANGE" | grep -oE '#[0-9]+' | tr -d '#' | sort -un || true)

for id in $ISSUES; do
  # Only issues of this project (or ones the CI user can see); others are skipped quietly
  curl -sf -o /dev/null -X PUT "$REDMINE_URL/issues/$id.json" -H "X-Redmine-API-Key: $REDMINE_API_KEY" \
    -H 'Content-Type: application/json' \
    --data "$(jq -n --arg n "Released in *$APP_NAME $VERSION_NAME* — \"download\":$REDMINE_URL/versions/$VERSION_ID" '{issue: {notes: $n}}')" \
    && echo "noted #$id" || echo "skipped #$id (not visible to the CI user)"
done

FILES=$(api GET "/projects/$PROJECT_ID/files.json" | jq -r --argjson v "$VERSION_ID" \
  '.files[] | select(.version.id == $v) | "* \"\(.filename)\":\(.content_url)"')
BODY=$(printf 'h3. Downloads\n\n%s\n\nh3. Changes%s\n\n%s\n' "$FILES" "${PREV:+ since $PREV}" "${CHANGES:-* (no commits)}")
api POST "/projects/$PROJECT_ID/news.json" "$(jq -n --arg t "$TITLE" --arg s "Version $VERSION, commit $SHA" --arg d "$BODY" \
  '{news: {title: $t, summary: $s, description: $d}}')" >/dev/null

# Closed + due today: Roadmap lists it as released, and new issues can no longer target it
api PUT "/versions/$VERSION_ID.json" "$(jq -n --arg d "$(date +%F)" '{version: {status: "closed", due_date: $d}}')" >/dev/null
echo "Release published: $REDMINE_URL/versions/$VERSION_ID"
