# Patches for bash on SerenityOS

## `0001-accept.c-Include-sys-select.h.patch`

accept.c: Include sys/select.h

This is transitively pulled in by other headers in some systems,
serenity is not one of them.

## `0002-Remove-unsupported-examples.patch`

Remove unsupported examples

- getconf: we are missing libintl.h and don't support multiple needed
  syscalls (from around 300 total syscalls)
- strptime: no strptime() in time.h
- fltexpr: 'implicit declaration of function' build error

