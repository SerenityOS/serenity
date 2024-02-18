# Patches for SRB2 on SerenityOS

## `0001-add-openmpt-to-build.patch`

Add OpenMPT to build

The build system doesn't give us much choice on what library to build and what library to look for.
OpenMPT is not built by default but we need it to build so this goes and change that.

## `0002-fix-libgme-include.patch`

Fix libgme include

One of the build targets is looking for the gme.h header.
It seems like it cannot automatically find it so we help it a little.

## `0003-disable-consolevar-value-checker-sad-path.patch`

Disable Console Variables value checker sad path

For some reason, the value checker for console variables seems to not behave properly even with the default console variables value.
Disabling the error path resolves this issue and the game still works fine without it.

## `0004-i_system.c-hacks.patch`

i_system.c hacks

This patch disables / removes some code to get the game to build without issues.
These don't seem to matter much anyway as the game still runs.
