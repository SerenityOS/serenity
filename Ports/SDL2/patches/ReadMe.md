# Patches for SDL2 on SerenityOS

## `0001-Add-SerenityOS-platform-support.patch`

Add SerenityOS platform support


## `0002-Replace-usages-of-FULL-paths-with-their-relative-cou.patch`

Replace usages of FULL paths with their relative counterparts

GNUInstallDirs currently doesn't support the seperate STAGING directory,
and will always prepend the final installation prefix to its paths.

Work around that by just using the path that is relative to the
installation directory and let CMake figure out the rest.

