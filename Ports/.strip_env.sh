#!/usr/bin/env bash

exec env -i SERENITY_STRIPPED_ENV=1 \
    HOME="${HOME}" \
    USER="${USER}" \
    TERM="${TERM}" \
    PATH="${PATH}" \
    EDITOR="${EDITOR:-}" \
    MAKEJOBS="${MAKEJOBS:-}" \
    IN_SERENITY_PORT_DEV="${IN_SERENITY_PORT_DEV:-}" \
    SERENITY_ARCH="${SERENITY_ARCH:-}" \
    SERENITY_TOOLCHAIN="${SERENITY_TOOLCHAIN:-}" \
    "${@}"
