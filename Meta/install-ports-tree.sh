#!/usr/bin/env bash

DESTDIR="${SERENITY_SOURCE_DIR}/Build/${SERENITY_ARCH}/Root/usr"

git ls-files --full-name "${SERENITY_SOURCE_DIR}/Ports" | \
  rsync -raHL \
    --chown=0:0 --inplace --update \
    --files-from=- \
    --exclude="Ports/.hosted_defs.sh" \
    "${SERENITY_SOURCE_DIR}" "${DESTDIR}"
