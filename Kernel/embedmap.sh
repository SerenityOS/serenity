#!/bin/sh
tmp=$(mktemp)
(cat kernel.map; printf '%b' '\0') > "$tmp"
objcopy --update-section .ksyms="$tmp" Kernel
rm -f "$tmp"
