#!/bin/sh

set -e
SERENITY_ROOT=../../

for dir in . Parser DOM CSS Layout; do
    mkdir -p $SERENITY_ROOT/Root/usr/include/LibHTML/$dir
    cp $dir/*.h $SERENITY_ROOT/Root/usr/include/LibHTML/$dir/
done

cp libhtml.a $SERENITY_ROOT/Root/usr/lib/
