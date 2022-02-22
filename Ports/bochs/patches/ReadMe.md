# Patches for bochs on SerenityOS

## `use-pkg_config-sdl2.patch`

Modifies configure to use `pkg_config` to detect SDL2 installed on
SerenityOS.

Without this patch, configure uses `sdl2_config` and will detect SDL2
installed on the host system.
