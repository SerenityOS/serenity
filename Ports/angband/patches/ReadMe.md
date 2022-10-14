# Patches for angband on SerenityOS

## `0001-Disable-hardware-acceleration.patch`

Disable hardware acceleration

We don't support this, so disable it.

## `0002-Fix-up-SDL-path-handling.patch`

Fix up SDL path handling

Fix up some copy-paste and logic mistakes in the configure script that
prevent us from setting a prefix for the SDL installation.

