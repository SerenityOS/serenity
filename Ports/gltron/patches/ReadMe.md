# Patches for gltron on SerenityOS

## `0001-Build-Remove-ansi-build-argument.patch`

Build: Remove `-ansi` build argument


## `0002-Build-Fix-char-vs.-const-char-arguments.patch`

Build: Fix `char*` vs. `const char*` arguments

These arguments are of the wrong constness, which will trip our
compiler.

## `0003-System-Make-sure-to-exit-the-loop-on-receiving-SDL_Q.patch`

System: Make sure to exit the loop on receiving SDL_QUIT

This is fixed in more modern adaptations, as can be seen here:

https://github.com/laanwj/gltron/blob/336dbbb75afe0aed1d9faaa5bbaa867b2b13d10b/nebu/base/system.c#L135

Since we work with the original source material, we better patch this
ourselves.

