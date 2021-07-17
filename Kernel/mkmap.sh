#!/bin/sh
tmp=$(mktemp)
nm -n Kernel | grep -vE \\.Lubsan_data | awk '{ if ($2 != "a") print; }' | uniq > "$tmp"
printf "%08x\n" "$(wc -l "$tmp" | cut -f1 -d' ')" > kernel.map
c++filt < "$tmp" >> kernel.map
rm -f "$tmp"
