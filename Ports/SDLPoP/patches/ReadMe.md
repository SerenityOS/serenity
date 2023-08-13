# Patches for SDLPoP on SerenityOS

## `0001-Disable-some-extra-features.patch`

Disable some extra features

This just disables some extra features the game has such as screenshots, hardware acceleration, etc.

## `0002-Use-the-correct-include-paths-for-SDL.patch`

Use the correct include paths for SDL

The SDL port is installed into /usr/local, and its headers are
accessible as <SDL2/...>.

## `0003-Fix-SDL2-include-path.patch`

Fix SDL2 include path

SDL2 headers are installed into /usr/local under SDL2, make it so
they're found.

