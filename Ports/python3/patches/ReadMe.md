# Patches for python3 on SerenityOS

## `0001-Enforce-UTF-8-as-the-locale-encoding.patch`

Enforce UTF-8 as the locale encoding

By defining `_Py_FORCE_UTF8_LOCALE` as some other platforms already do,
we can enforce UTF-8 as the encoding.

## `0002-Tweak-configure-and-configure.ac.patch`

Tweak configure and configure.ac

As usual, make the `configure` script recognize Serenity. Also set
`MACHDEP` (which is used for `sys.platform`) to a version-less
`serenityos`, even when not cross-compiling.

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

