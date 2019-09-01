#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibThread/
cp *.h $SERENITY_ROOT/Root/usr/include/LibThread/
cp libthread.a $SERENITY_ROOT/Root/usr/lib/
