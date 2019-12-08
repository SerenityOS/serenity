#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibAudio/
cp ./*.h $SERENITY_ROOT/Root/usr/include/LibAudio/
cp libaudio.a $SERENITY_ROOT/Root/usr/lib/
