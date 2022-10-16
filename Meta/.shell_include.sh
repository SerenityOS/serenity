#!/bin/sh
# shellcheck disable=SC2034
# SC2034: "Variable appears unused. Verify it or export it."
#         Those are intentional here, as the file is meant to be included elsewhere.

SUDO="sudo"

if [ "$(uname -s)" = "SerenityOS" ]; then
    SUDO="pls"
fi

die() {
    echo "die: $*"
    exit 1
}
