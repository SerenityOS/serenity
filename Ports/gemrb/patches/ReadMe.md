# Patches for gemrb on SerenityOS

## `0001-Hard-code-some-paths-for-runtime-purposes.patch`

Hard-code some paths for runtime purposes

GemRB uses the paths where its libraries and data are copied to during
installation at runtime by generating a header. This does not work for
us, as our paths change from compilation to runtime. The easiest fix for
this is to hard-code these paths in the header file.

## `0002-Create-SDL2-renderer-as-unaccelerated.patch`

Create SDL2 renderer as unaccelerated


