# Patches for Python 3.9 on SerenityOS

## `define-have-sigset-t.patch`

Ensures `HAVE_SIGSET_T` is defined, as we *do* have `sigset_t` but it's not detected properly due to some related functions being missing.

## `define-py-force-utf8-locale.patch`

Enforce UTF-8 as encoding by defining `_Py_FORCE_UTF8_LOCALE`.

## `disable-setrlimit.patch`

Disables check for `RLIMIT_CORE` and subsequent `setrlimit()` call. Would be enabled otherwise as we *do* have `<sys/resource.h>` and therefore `HAVE_SYS_RESOURCE_H`.

## `fix-autoconf.patch`

As usual, make the `configure` script recognize Serenity.

## `remove-setlocale-from-preconfig.patch`

Our stub implementation of `setlocale()` always returns `nullptr`, which the interpreter considers critical enough to exit right away.

## `webbrowser.patch`

Register the SerenityOS Browser in the [`webbrowser`](https://docs.python.org/3/library/webbrowser.html) module.
