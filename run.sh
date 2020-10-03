#!/bin/sh
#
# Script to make it easy to build and run Serenity OS for the first time.
# Old friends already know what to do.
#
set -e
cd Toolchain
./BuildIt.sh
cd ../Build
cmake -GNinja ..
ninja
ninja install
ninja image
ninja run
