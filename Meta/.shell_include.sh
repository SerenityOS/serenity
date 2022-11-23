#!/bin/sh
# shellcheck disable=SC2034
# SC2034: "Variable appears unused. Verify it or export it."
#         Those are intentional here, as the file is meant to be included elsewhere.

# NOTE: If using another privilege escalation binary make sure it is configured or has the appropiate flag
#       to keep the current environment variables in the launched process (in sudo's case this is achieved
#       through the -E flag described in sudo(8).
SUDO="sudo -E"

if [ "$(uname -s)" = "SerenityOS" ]; then
    SUDO="pls -E"
fi

die() {
    echo "die: $*"
    exit 1
}
