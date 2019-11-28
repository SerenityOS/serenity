#!/bin/sh
tmp=$(mktemp)
nm -n kernel | awk '{ if ($2 != "a") print; }' | uniq > $tmp
printf "%08x\n" $(wc -l $tmp | cut -f1 -d' ') > kernel.map
cat $tmp >> kernel.map
rm -f $tmp
