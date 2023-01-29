# Patches for e2fsprogs on SerenityOS

## `0001-Include-sys-time.h-in-various-places.patch`

Include `sys/time.h` in various places

Apparently, certain definitions from `sys/time.h` are supposed to be
available from `sys/types.h` as well, but there isn't anything to verify
that, so just add the inclusions manually for now.

