#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export PATH="$DIR/Local/bin:$PATH"
export TOOLCHAIN="$DIR"
export SERENITY_ROOT="$DIR/.."
echo "$PATH"
