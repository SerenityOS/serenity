#!/bin/sh

set -e

SCRIPT_DIR="$(dirname "${0}")"

# https://www.shellcheck.net/wiki/SC1090 No need to shellcheck private config.
# shellcheck source=/dev/null
[ -x "$SCRIPT_DIR/../run-local.sh" ] && . "$SCRIPT_DIR/../run-local.sh"

#export SERENITY_PACKET_LOGGING_ARG="-object filter-dump,id=hue,netdev=breh,file=e1000.pcap"

"$SCRIPT_DIR/run.py"
