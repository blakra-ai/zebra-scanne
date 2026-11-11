#!/usr/bin/env bash
# Builds debug APKs for the requested clients/environments and copies them to dist/
# as <client>-<env>-<version>.apk.
#
#   CLIENT  = a name from clients/*.properties, or "all"   (default: all)
#   ENV     = dev | stage | prod | all                     (default: all)
#   APP_VERSION_NAME (optional) is used in the file names.
# Environments without a url in the client file are skipped.
set -euo pipefail
cd "$(dirname "$0")/.."

CLIENT="${CLIENT:-all}"
ENV="${ENV:-all}"
VERSION="${APP_VERSION_NAME:-local}"

cap() { echo "$(tr '[:lower:]' '[:upper:]' <<< "${1:0:1}")${1:1}"; }

if [ "$CLIENT" = "all" ]; then
  clients=$(ls clients/*.properties | xargs -n1 basename | sed 's/\.properties$//')
else
  [ -f "clients/$CLIENT.properties" ] || { echo "Unknown client '$CLIENT' (no clients/$CLIENT.properties)"; exit 1; }
  clients="$CLIENT"
fi
if [ "$ENV" = "all" ]; then envs="dev stage prod"; else envs="$ENV"; fi

tasks=()
built=()
for c in $clients; do
  for e in $envs; do
    url=$(grep -E "^url\.$e=" "clients/$c.properties" | head -1 | cut -d= -f2- | tr -d '[:space:]' || true)
    if [ -z "$url" ]; then
      echo "Skipping $c/$e: no url.$e in clients/$c.properties"
      continue
    fi
    tasks+=("assemble$(cap "$c")$(cap "$e")Debug")
    built+=("$c:$e")
  done
done
[ ${#tasks[@]} -gt 0 ] || { echo "Nothing to build for CLIENT=$CLIENT ENV=$ENV"; exit 1; }

echo "Building: ${tasks[*]}"
./gradlew "${tasks[@]}" --stacktrace

mkdir -p dist
for ce in "${built[@]}"; do
  c="${ce%%:*}"; e="${ce##*:}"
  apk="app/build/outputs/apk/${c}$(cap "$e")/debug/app-${c}-${e}-debug.apk"
  cp "$apk" "dist/${c}-${e}-${VERSION}.apk"
done
ls -l dist
