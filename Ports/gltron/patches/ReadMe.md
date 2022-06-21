# Patches for gltron on SerenityOS

## `0001-Build-Replace-lGL-with-lgl-to-reference-our-LibGL.patch`

Build: Replace `-lGL` with `-lgl` to reference our LibGL


## `0002-Build-Remove-ansi-build-argument.patch`

Build: Remove `-ansi` build argument


## `0003-Build-Fix-char-vs.-const-char-arguments.patch`

Build: Fix `char*` vs. `const char*` arguments

These arguments are of the wrong constness, which will trip our
compiler.

## `0004-Build-Disable-nebu-using-SDL-s-glext.h-constants.patch`

Build: Disable nebu using SDL's glext.h constants

SerenityOS provides glext.h definitions inside GL/gl.h, but the
build process thinks that glext.h doesn't exist, therefore it attempts
to use SDL's definitions, which leads to a conflict. Therefore, disable
use of said definitions.

