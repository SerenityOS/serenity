#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/sys/
mkdir -p $SERENITY_ROOT/Root/usr/lib/
cp ./*.h $SERENITY_ROOT/Root/usr/include/
cp libpthread.a $SERENITY_ROOT/Root/usr/lib/
