#!/bin/bash

cd $SERENITY_ROOT
find . -name '*.ipc' -or -name '*.cpp' -or -name '*.h' -or -name '*.S' | grep -Fv Patches/ | grep -Fv Root/ | grep -Fv Ports/ | grep -Fv Toolchain/ > serenity.files
