# Patches for gltron on SerenityOS

## `0001-Build-Allow-CFLAGS-env-var-to-be-set.patch`

Build: Allow `CFLAGS` env var to be set


## `0002-Build-Replace-lGL-with-lgl-to-reference-our-LibGL.patch`

Build: Replace `-lGL` with `-lgl` to reference our LibGL


## `0003-Build-Remove-ansi-build-argument.patch`

Build: Remove `-ansi` build argument


## `0004-Build-Fix-char-vs.-const-char-arguments.patch`

Build: Fix `char*` vs. `const char*` arguments

These arguments are of the wrong constness, which will trip our
compiler.

## `0005-Scripting-Fix-default-keybindings.patch`

Scripting: Fix default keybindings

These constants referred to the wrong keys.

## `0006-SDL-Convert-SDL1-to-SDL2.patch`

SDL: Convert SDL1 to SDL2


