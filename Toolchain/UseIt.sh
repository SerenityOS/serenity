#!/bin/bash
DIR=$( dirname $( readlink -e "$0" ) )
export PATH="$DIR/Local/bin:$PATH"
export TOOLCHAIN="$DIR"
export SERENITY_ROOT="$DIR/.."
echo "$PATH"
