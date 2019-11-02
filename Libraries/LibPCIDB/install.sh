#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibPCIDB/
cp ./*.h $SERENITY_ROOT/Root/usr/include/LibPCIDB/
cp libpcidb.a $SERENITY_ROOT/Root/usr/lib/
