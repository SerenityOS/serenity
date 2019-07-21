#!/bin/bash

set -e
SERENITY_ROOT=../../

mkdir -p $SERENITY_ROOT/Root/usr/include/LibGUI/
cp *.h $SERENITY_ROOT/Root/usr/include/LibGUI/
cp libgui.a $SERENITY_ROOT/Root/usr/lib/
