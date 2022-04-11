# Patches for angband

## `disable-accelerated-rendering.patch`

Disable SDL2 hardware acceleration as we do not support that.

## `fix-sdl-prefix-handling.patch`

Fix up some copy-paste and logic mistakes in the configure script that
prevent us from setting a prefix for the SDL installation.
