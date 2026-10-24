# Patches for openrct2 on SerenityOS

## `0001-Add-SerenityOS-platform-detection.patch`

Add SerenityOS platform detection

We teach OpenRCT2 about the name of our platform.

## `0002-Read-the-executable-path-from-proc-self-exe.patch`

Read the executable path from /proc/self/exe

When looking for the executable path, we behave exactly like Linux.

## `0003-Add-compile-options-to-CMakeLists.txt.patch`

Add compile options to CMakeLists.txt

This irons out some compiler warnings that turned into errors when compiling a release build.

## `0004-Disable-locale-detection-for-writing-the-default-con.patch`

Disable locale detection for writing the default config.ini

At the time of writing, locale support in Serenity is not great. We always returned the "C" locale, which this code interpreted wrong. Since this is just used for writing a default value to the game config (which can be changed later), we just default to English.

## `0005-Add-SerenityOS-support-to-bundled-QuickJS.patch`

Add SerenityOS support to bundled QuickJS


## `0006-Include-alloca.h-on-SerenityOS.patch`

Include alloca.h on SerenityOS


## `0007-Use-the-SDL-software-renderer.patch`

Use the SDL software renderer


## `0008-Fix-black-screen-with-16-bit-SDL-textures.patch`

Fix black screen with 16-bit SDL textures

`CopyBitsToTexture` calculated row padding assuming 4-byte pixels. The SDL software renderer selects an RGB565 texture, so no copy loop matched and the screen stayed black. Use the texture's actual bytes per pixel and write one mapped palette colour per pixel.


