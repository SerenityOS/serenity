#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibCore/
cp *.h $SERENITY_ROOT/Root/usr/include/LibCore/
cp libcore.a $SERENITY_ROOT/Root/usr/lib/
