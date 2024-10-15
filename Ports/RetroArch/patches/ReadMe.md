# Patches for RetroArch on SerenityOS

## `0001-Add-SerenityOS-platform.patch`

Add SerenityOS platform


## `0002-Add-strlcat.patch`

Add strlcat()


## `0003-Disable-pthread_attr_setschedpolicy.patch`

Disable pthread_attr_setschedpolicy()


## `0004-Use-SDL-software-instead-of-hardware-rendering.patch`

Use SDL software instead of hardware rendering


## `0005-Set-default-options-for-SerenityOS.patch`

Set default options for SerenityOS

Set libretro cores path to `/usr/lib/libretro`
Set video and audio driver to sdl2
Disable vsync
The libretro core won't keep running in the background when we are in the menu.
Don't pause gameplay when window focus is lost
Set some config paths to home directory

## `0006-Enable-unix-platform.patch`

Enable unix platform

This will make arguments from shell (like `--help`) work

