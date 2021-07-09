# Patches for Python 3.9 on SerenityOS

## `define-have-sigset-t.patch`

Ensures `HAVE_SIGSET_T` is defined, as we *do* have `sigset_t` but it's not detected properly due to some related functions being missing.

## `define-py-force-utf8-locale.patch`

Enforce UTF-8 as encoding by defining `_Py_FORCE_UTF8_LOCALE`.

## `fix-autoconf.patch`

As usual, make the `configure` script recognize Serenity. Also set `MACHDEP` (which is used for `sys.platform`) to a version-less `serenityos`, even when not cross-compiling.

## `webbrowser.patch`

Register the SerenityOS Browser in the [`webbrowser`](https://docs.python.org/3/library/webbrowser.html) module.

Note: This change [has been added to upstream CPython](https://github.com/python/cpython/pull/25947) and will be included in the Python 3.10 release :^)
