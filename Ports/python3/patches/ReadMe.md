# Patches for python3 on SerenityOS

## `0001-Enforce-UTF-8-as-the-locale-encoding.patch`

Enforce UTF-8 as the locale encoding

By defining `_Py_FORCE_UTF8_LOCALE` as some other platforms already do,
we can enforce UTF-8 as the encoding.

## `0002-Tweak-configure.patch`

Tweak configure

Merged patches from Linus Groh, Julian Offenhäuser, Oskar Skog:

As usual, make the `configure` script recognize Serenity. Also set
`MACHDEP` (which is used for `sys.platform`) to a version-less
`serenityos`, even when not cross-compiling.

Set name of shared libpython

Compile with CCSHARED=-fPIC

## `0003-Include-sys-uio.h-in-socketmodule.c.patch`

Include `sys/uio.h` in `socketmodule.c`

This is to ensure that `struct iovec` is defined, which is required by
the `socket` module.

## `0004-Tweak-setup.py.patch`

Tweak `setup.py`

Make some tweaks to Python's `setup.py`:

- Add `/usr/local/lib` and `/usr/local/include` to the system lib and
  include dirs respectively, relative to the sysroot when
  crosscompiling. These are by default only included when not
  crosscompiling for some reason.
- Add `/usr/local/include/ncurses` to the curses include paths so it can
  build the `_curses` module. This is by default included for a bunch of
  extensions, but not `_curses`.

## `0005-Tweak-setup.py-sysroot-detection.patch`

Tweak `setup.py` sysroot detection

When crosscompiling, the Python installer expects the C compiler to
be invoked with a `--sysroot` command line option, which then is used
to find additional subdirectories containing headers and libraries.

Because there is no such option present, this is a workaround to use
the environment variable `SERENITY_INSTALL_ROOT` as a fake `--sysroot`
in the detection code.

## `0006-Workaround-for-unsupported-socket-option.patch`

Workaround for unsupported socket option

This is a workaround for ignoring the result of `setsockopt` call when
given `TCP_NODELAY` as an argument. This TCP socket option is used in
many applications (like pip and requests) for optimization purposes.
For now, it can be safely ignored until it's supported in the kernel.

