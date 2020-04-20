#!/bin/sh

script_path=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
cd "$script_path" || exit 1
mkdir -p build
cd build || exit 1
cmake ..
make js
