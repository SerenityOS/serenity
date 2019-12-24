#!/bin/sh

if [ ! -n "$SERENITY_ROOT" ]
then echo "Serenety root not set."
fi

cd "$SERENITY_ROOT" || exit 1
find . -name '*.ipc' -or -name '*.cpp' -or -name '*.c' -or -name '*.h' -or -name '*.S' -or -name '*.css' | grep -Fv Patches/ | grep -Fv Root/ | grep -Fv Ports/ | grep -Fv Toolchain/ | grep -Fv Base/ > serenity.files
