# Patches for python3 on SerenityOS

## `0001-Enforce-UTF-8-as-the-locale-encoding.patch`

Enforce UTF-8 as the locale encoding

By defining `_Py_FORCE_UTF8_LOCALE` as some other platforms already do,
we can enforce UTF-8 as the encoding.

## `0002-Tweak-configure.patch`

Tweak configure

As usual, make the `configure` script recognize Serenity. Also set
`MACHDEP` (which is used for `sys.platform`) to a version-less
`serenityos`, even when not cross-compiling.


## `0003-Include-sys-uio.h-in-socketmodule.c.patch`

Include `sys/uio.h` in `socketmodule.c`

This is to ensure that `struct iovec` is defined, which is required by
the `socket` module.

## `0004-Include-sys-time.h-in-pycore_time.h.patch`

Include `sys/time.h in `pycore_time.h`

Our version/configuration of GCC also complains about this, and various
other things end up complaining about the size of `struct timeval` being
unknown.

## `0005-Don-t-include-sys-syscall.h-in-mimalloc.patch`

Don't include `sys/syscall.h` in mimalloc


## `0006-Force-disable-pyrepl.patch`

Force-disable pyrepl

We are lacking termios support for this leading to a non-functional
modern REPL. Force-disable it in the source instead of requiring users
to set PYTHON_BASIC_REPL=1 to work around the issue.

