# Patches for glu on SerenityOS

## `0001-Build-Remove-pkgconfig.patch`

Build: Remove pkgconfig

We can't use pkgconfig here since the OpenGL dependency we find is
incompatible with it as an argument.

## `0002-Build-Do-not-hide-symbols-by-default.patch`

Build: Do not hide symbols by default

For some reason, the functions glu exports end up as LOCAL entries in
the shared library. Remove this default visibility to set them to
GLOBAL.

