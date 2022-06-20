# Patches for joe on SerenityOS

## `0001-Define-__USE_MISC-in-checkwidths.c.patch`

Define __USE_MISC in checkwidths.c

Define `__USE_MISC` manually in `checkwidths.c` for `ECHOCTL` and `ECHOKE`,
see `Kernel/API/POSIX/termios.h`.

## `0002-Remove-the-sys-prefix-for-the-fcntl-include.patch`

Remove the sys/ prefix for the fcntl include

Serenity does not have the header `fcntl.h` in `/usr/include/sys`.

## `0003-Undefine-TERMINFO-in-termcap.c.patch`

Undefine TERMINFO in termcap.c

Leaving it defined will lead to a crash.

