# Patches for glu on SerenityOS

## `0001-Build-Manually-find-LibGL.patch`

Build: Manually find LibGL

We don't have a working pkgconfig to find it, so get rid of it and
manually find the library using the C compiler.

## `0002-Build-Do-not-hide-symbols-by-default.patch`

Build: Do not hide symbols by default

For some reason, the functions glu exports end up as LOCAL entries in
the shared library. Remove this default visibility to set them to
GLOBAL.

