#!/bin/sh

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibMarkdown/
cp *.h $SERENITY_ROOT/Root/usr/include/LibMarkdown/
cp libmarkdown.a $SERENITY_ROOT/Root/usr/lib/
