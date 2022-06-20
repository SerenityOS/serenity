# Patches for xash3d-fwgs on SerenityOS

## `0001-Build-Add-SerenityOS-to-list-of-compatible-systems.patch`

Build: Add SerenityOS to list of compatible systems

This is required by the build system to spit out a library with
the correct name/platform.

## `0002-Engine-Keep-HTTP-from-endlessly-formatting-NaN-value.patch`

Engine: Keep HTTP from endlessly formatting NaN values

For whatever reason, our progress count for HTTP downloads stays at 0.
This results in the engine calculating a NaN progress value many times
each frame, which results in a significant performance hit.

