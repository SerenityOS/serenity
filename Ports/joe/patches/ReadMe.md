# Patches for joe's own editor

## `joe.patch`

Build a curseless joe's own editor, its only dependency is LibC.

- Add serenity to `config.sub`.
- Some hacks in joe source code to make it work.
    - Undefine the macro `TERMINFO` in `termcap.c`, or it will lead crash.
    - Define `__USE_MISC` manually in `checkwidths.c` for `ECHOCTL` and `ECHOKE`,
    see `Kernel/API/POSIX/termios.h`.
    - Remove the prefix `sys/`, because serenity does not have the header `fcntl.h`
    in `/usr/include/sys`.

