#!/usr/bin/env bash

SERENITY_PORTS_DIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}/Root/usr/Ports"

for file in $(git ls-files "${SERENITY_SOURCE_DIR}/Ports"); do
    if [ "$(basename "$file")" != ".hosted_defs.sh" ]; then
        target=${SERENITY_PORTS_DIR}/$(realpath --relative-to="${SERENITY_SOURCE_DIR}/Ports" "$file")
        mkdir -p "$(dirname "$target")" && cp "$file" "$target"
    fi
done
