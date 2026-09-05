# Patches for tcl on SerenityOS

## `0001-Remove-uses-of-ipv6.patch`

Remove uses of ipv6


## `0002-Work-around-broken-fd_set-detection.patch`

Work around broken fd_set detection

We have it in sys/select.h according to POSIX but the configure script
goes looking in sys/types.h (apparently for historic reasons).

