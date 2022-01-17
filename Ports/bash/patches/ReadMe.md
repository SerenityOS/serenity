# Patches for bash on SerenityOS

## `0001-accept.c-Include-sys-select.h.patch`

accept.c: Include sys/select.h
This is transitively pulled in by other headers in some systems,
serenity is not one of them.

