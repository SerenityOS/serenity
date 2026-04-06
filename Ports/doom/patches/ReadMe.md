# Patches for doom on SerenityOS

## `0001-Update-the-makefile-for-serenity.patch`

Update the makefile for serenity

This lets the build system choose the compiler. Also, `-std=c17` is
required as newer version are not compatible with custom definition of
bool, true and false. Finally, it seems that newer versions of GCC
report an error for incompatible-pointer-types, so turn this into a
warning.

## `0002-Use-SDL-software-renderer.patch`

Use SDL software renderer


