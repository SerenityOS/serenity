# Patches for fheroes2 on SerenityOS

## `cmakelists.patch`

Fix library detection for SDL2 dependencies in CMakeLists files.

## `add-serenity-support.patch`

Add `__serenity__` option in header files that validate current platform.

## `set-sdl-software-renderer.patch`

Use SDL Software renderer, instead of hardware-accelerated one.
