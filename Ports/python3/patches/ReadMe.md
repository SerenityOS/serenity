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
