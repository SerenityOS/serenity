# Patches for halflife on SerenityOS

## `0001-Build-Add-SerenityOS-to-list-of-compatible-systems.patch`

Build: Add SerenityOS to list of compatible systems

This is required by the build system to spit out a library with
the correct name/platform.

## `0002-Build-Add-__STRINGS_H_COMPAT_HACK-macro.patch`

Build: Add `__STRINGS_H_COMPAT_HACK` macro

This bypasses a bunch of `str[n]cmpcase` errors that occur due to weird
LibC compatibility problems.

