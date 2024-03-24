# Patches for openrct2 on SerenityOS

## `0001-Add-SerenityOS-platform-detection.patch`

Add SerenityOS platform detection

We teach OpenRCT2 about the name of our platform.

## `0002-Read-the-executable-path-from-proc-self-exe.patch`

Read the executable path from /proc/self/exe

When looking for the executable path, we behave exactly like Linux.

## `0003-Remove-use-of-strptime.patch`

Remove use of strptime()

This is a hack to patch out strptime() from duktape, which is not being used by this project.

## `0004-Add-compile-options-to-CMakeLists.txt.patch`

Add compile options to CMakeLists.txt

This irons out some compiler warnings that turned into errors when compiling a release build.

## `0005-Disable-locale-detection-for-writing-the-default-con.patch`

Disable locale detection for writing the default config.ini

At the time of writing, locale support in Serenity is not great. We always returned the "C" locale, which this code interpreted wrong. Since this is just used for writing a default value to the game config (which can be changed later), we just default to English.

## `0006-Disable-g2.dat-target.patch`

Disable g2.dat target

Normally, the build system uses one of the compiled binaries to pack assets into `g2.dat`. However, since we cross-compile this binary for Serenity, we can't do this on the host system. Instead, we download the latest Linux build of OpenRCT2 and copy its `g2.dat` into the disk image.

