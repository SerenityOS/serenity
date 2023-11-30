# Patches for imgcat on SerenityOS

## `0001-Remove-an-include-of-err.h.patch`

Remove an include of `err.h`

`err.h` appears to be BSD-specific, and we don't support it. Luckily,
imgcat compiles just fine even when removing it.

## `0002-Remove-the-dependency-on-termcap.patch`

Remove the dependency on termcap


