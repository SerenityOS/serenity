#!/bin/bash

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibGfx/
cp *.h $SERENITY_ROOT/Root/usr/include/LibGfx/
