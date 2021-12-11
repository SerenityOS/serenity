# Patches for Python 3.9 on SerenityOS

## `define-have-sigset-t.patch`

Ensures `HAVE_SIGSET_T` is defined, as we *do* have `sigset_t` but it's not detected properly due to some related functions being missing.

## `include-sys-uio.patch`

Ensures `struct iovec` is defined, required by the socket module.

## `define-py-force-utf8-locale.patch`

Enforce UTF-8 as encoding by defining `_Py_FORCE_UTF8_LOCALE`.

## `fix-autoconf.patch`

As usual, make the `configure` script recognize Serenity. Also set `MACHDEP` (which is used for `sys.platform`) to a version-less `serenityos`, even when not cross-compiling.
