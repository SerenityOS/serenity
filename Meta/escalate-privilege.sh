#!/bin/sh

set -e

die() {
    echo "die: $*"
    exit 1
}

if [ "$(id -u)" != 0 ]; then
    # Check for doas instead of sudo, see https://man.openbsd.org/doas.conf#keepenv
    # for the required configuration to pass through environment variables since we
    # don't have -E.
    # If we have doas and sudo installed but only want to use sudo, set the NO_DOAS
    # environment variable to anything.
    if [ -z "$NO_DOAS" ] && type doas > /dev/null 2>&1; then
        exec doas -- "$0" "$@" || die "privilege escalation failed"
    else
        exec sudo -E -- "$0" "$@" || die "privilege escalation failed"
    fi
else
    if [ -z "$NO_DOAS" ] && type doas > /dev/null 2>&1 && [ -n "$DOAS_USER" ]; then
        : "${SUDO_UID:=$(id -u "$DOAS_USER")}" "${SUDO_GID:="$(id -g "$DOAS_USER")"}"
    fi

    : "${SUDO_UID:=0}" "${SUDO_GID:=0}"
fi
