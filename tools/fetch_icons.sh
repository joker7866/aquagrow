#!/usr/bin/env bash
# Downloads Material Symbols (outlined) as Android vector drawables into app/src/main/res/drawable.
# Usage: tools/fetch_icons.sh water_drop set_meal ...     -> ic_water_drop.xml, ...
#        append ":fill" to also fetch the filled variant  -> ic_<name>_filled.xml
set -euo pipefail
cd "$(dirname "$0")/.."
BASE=https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android
OUT=app/src/main/res/drawable
for arg in "$@"; do
  name=${arg%%:*}
  variants=("${name}_24px:ic_${name}")
  [[ $arg == *:fill ]] && variants+=("${name}_fill1_24px:ic_${name}_filled")
  for v in "${variants[@]}"; do
    src=${v%%:*}; dst=${v##*:}
    curl -sf "$BASE/$name/materialsymbolsoutlined/$src.xml" \
      | perl -0pe 's/\n\s*android:tint="[^"]*"(>?)/$1/' \
      > "$OUT/$dst.xml" || { echo "missing: $name"; rm -f "$OUT/$dst.xml"; }
  done
done
