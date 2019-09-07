#!/bin/bash

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibDraw/
cp *.h $SERENITY_ROOT/Root/usr/include/LibDraw/
cp libdraw.a $SERENITY_ROOT/Root/usr/lib/
