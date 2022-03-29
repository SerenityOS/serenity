# Patches for zlib on SerenityOS

## `fix-cross-compilation.patch`

Backports an upstream fix for a bug that caused the host compiler to be used
for linking even though the cross-compiler was specified in the `CC`
environment variable.
