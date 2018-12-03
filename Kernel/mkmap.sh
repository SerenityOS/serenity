#!/bin/sh
tmp=$(mktemp)
nm -nC kernel | uniq > $tmp
printf "%08x\n" $(wc -l $tmp | cut -f1 -d' ') > kernel.map
cat $tmp >> kernel.map
rm -f $tmp
