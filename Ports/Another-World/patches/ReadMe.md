# Patches for Another-World on SerenityOS

## `0001-Skip-using-find_package-for-SDL2.patch`

Skip using find_package() for SDL2

We pass the include directory via cmake flags, so we can just use that
and link against sdl2 normally.

