#!/usr/bin/env bash

SERENITY_PORTS_DIR="${SERENITY_ROOT}/Build/${SERENITY_ARCH}/Root/usr/Ports"

for file in $(git ls-files "${SERENITY_ROOT}/Ports"); do
    if [ "$(basename "$file")" != ".hosted_defs.sh" ]; then
        target=${SERENITY_PORTS_DIR}/$(realpath --relative-to="${SERENITY_ROOT}/Ports" "$file")
        mkdir -p "$(dirname "$target")" && cp "$file" "$target"
    fi
done
