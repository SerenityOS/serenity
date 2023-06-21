#!/bin/sh
tmp=$(mktemp)
(cat kernel.map; printf '%b' '\0') > "$tmp"
OBJCOPY="${OBJCOPY:-objcopy}"
"$OBJCOPY" --update-section .ksyms="$tmp" Kernel
rm -f "$tmp"
