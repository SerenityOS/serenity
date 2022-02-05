#!/usr/bin/env bash

set -e

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "${script_path}/.." || exit 1

ports=true
if [ "$1" == "--no-ports" ]; then
  ports=false
  shift
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

FAILURES=0

set +e

for cmd in \
        Meta/check-ak-test-files.sh \
        Meta/check-debug-flags.sh \
        Meta/check-markdown.sh \
        Meta/check-newlines-at-eof.py \
        Meta/check-style.py \
        Meta/lint-executable-resources.sh \
        Meta/lint-keymaps.py \
        Meta/lint-shell-scripts.sh \
        Meta/lint-gml-format.sh \
        Meta/lint-prettier.sh \
        Meta/lint-python.sh; do
    echo "Running ${cmd}... "
    if "${cmd}" "$@"; then
        echo -e "[${GREEN}OK${NC}]: ${cmd}"
    else
        echo -e "[${RED}FAIL${NC}]: ${cmd}"
        ((FAILURES+=1))
    fi
done

if [ -x ./Build/lagom/Tools/IPCMagicLinter/IPCMagicLinter ]; then
    if git ls-files '*.ipc' | xargs ./Build/lagom/Tools/IPCMagicLinter/IPCMagicLinter; then
        echo -e "[${GREEN}OK${NC}]: IPCMagicLinter (in Meta/lint-ci.sh)"
    else
        echo -e "[${RED}FAIL${NC}]: IPCMagicLinter (in Meta/lint-ci.sh)"
        ((FAILURES+=1))
    fi
else
    echo -e "[${GREEN}SKIP${NC}]: IPCMagicLinter (in Meta/lint-ci.sh)"
fi

echo "Running Meta/lint-clang-format.sh"
if Meta/lint-clang-format.sh --overwrite-inplace "$@" && git diff --exit-code; then
    echo -e "[${GREEN}OK${NC}]: Meta/lint-clang-format.sh"
else
    echo -e "[${RED}FAIL${NC}]: Meta/lint-clang-format.sh"
    ((FAILURES+=1))
fi

# lint-ports.py is handled separately as it scans all Ports/ all the time.
# This is fine when running lint-ci.sh from the PR validation workflow.
# However when running from the pre-commit workflow it takes an excessive
# amount of time. This condition allows the pre-commit program to detect
# when Ports/ files have changed and only invoke lint-ports.py when needed.
#
if [ "$ports" = true ]; then
    if Meta/lint-ports.py; then
        echo -e "[${GREEN}OK${NC}]: Meta/lint-ports.py"
    else
        echo -e "[${RED}FAIL${NC}]: Meta/lint-ports.py"
        ((FAILURES+=1))
    fi
fi

echo "(Not running lint-missing-resources.sh due to high false-positive rate.)"
echo "(Also look out for check-symbols.sh, which can only be executed after the build!)"

exit "${FAILURES}"
