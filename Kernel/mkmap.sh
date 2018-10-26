#!/bin/sh
tmp=$(mktemp)
nm -C kernel > $tmp
perl -lpe '$_=hex' $tmp | paste -d" " - $tmp | sort -n | cut -d" " -f 2- > kernel.map
rm $tmp
