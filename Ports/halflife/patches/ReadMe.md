# Patches for halflife on SerenityOS

## `fwgs-add-serenity.patch`

Build: Add SerenityOS to list of compatible systems

This is required by the build system to spit out a library with
the correct name/platform.

## `fwgs-dont-format-nan-loop.patch`

Engine: Keep HTTP from endlessly formatting NaN values

For whatever reason, our progress count for HTTP downloads stays at 0.
This results in the engine calculating a NaN progress value many times
each frame, which results in a significant performance hit.

## `hlsdk-add-serenity.patch`

Build: Add SerenityOS to list of compatible systems

This is required by the build system to spit out a library with
the correct name/platform.

## `hlsdk-strings-compat.patch`

Build: Add `__STRINGS_H_COMPAT_HACK` macro

This bypasses a bunch of `str[n]cmpcase` errors that occur due to weird
LibC compatibility problems.

