# Patches for scummvm on SerenityOS

## `0001-Do-not-include-malloc.h-on-SerenityOS.patch`

SerenityOS provides the standard allocation functions through `stdlib.h` and does not have `malloc.h`.
