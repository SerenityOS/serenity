#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibIPC/
cp ./*.h $SERENITY_ROOT/Root/usr/include/LibIPC/
cp libipc.a $SERENITY_ROOT/Root/usr/lib/
