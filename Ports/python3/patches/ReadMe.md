# Patches for Python 3.9 on SerenityOS

## `define-have-sigset-t.patch`

Ensures `HAVE_SIGSET_T` is defined, as we *do* have `sigset_t` but it's not detected properly due to some related functions being missing.

## `include-sys-uio.patch`

Ensures `struct iovec` is defined, required by the socket module.

## `define-py-force-utf8-locale.patch`

Enforce UTF-8 as encoding by defining `_Py_FORCE_UTF8_LOCALE`.

## `fix-autoconf.patch`

As usual, make the `configure` script recognize Serenity. Also set `MACHDEP` (which is used for `sys.platform`) to a version-less `serenityos`, even when not cross-compiling.

## `tweak-setup-py.patch`

Make some tweaks to Python's `setup.py` files:

- Add `/usr/local/lib` / `/usr/local/include` to the system lib / include dirs, relative to the sysroot when crosscompiling. These are by default only included when not crosscompiling for some reason.
- Add `/usr/local/include/ncurses` to the curses include paths so it can build the `_curses` module. This is by default included for a bunch of extensions, but not `_curses`.
