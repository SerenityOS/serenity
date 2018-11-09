#!/bin/sh
tmp=$(mktemp)
nm -C kernel > $tmp
perl -lpe '$_=hex' $tmp | paste -d" " - $tmp | sort -n | cut -d" " -f 2- > kernel.map.tmp
printf "%08x\n" $(wc -l kernel.map.tmp | cut -f1 -d' ') > kernel.map
cat kernel.map.tmp >> kernel.map
rm -f kernel.map.tmp
rm $tmp
