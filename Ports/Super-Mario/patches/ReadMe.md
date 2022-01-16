# Patches for Super-Mario

## `cwd.patch`

`chdir()`s to the installed directory before execution.

## `gcc-11-static-initializers.patch`

Removes global static initializers.
Presumably not needed anymore.

## `fix_cmakelists.patch`

Use `FindPkgConfig` instead of `find_package()` to locate SDL2.

## `fix_fireball_header.patch`

Fixes a header include name.

## `disable_graphic_acceleration.patch`

Disables SDL2 hardware acceleration as we don't support that.

