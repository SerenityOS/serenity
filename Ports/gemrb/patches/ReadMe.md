# Patches for gemrb on SerenityOS

## `0001-Hard-code-some-paths-for-runtime-purposes.patch`

Hard-code some paths for runtime purposes

GemRB uses the paths where its libraries and data are copied to during
installation at runtime by generating a header. This does not work for
us, as our paths change from compilation to runtime. The easiest fix for
this is to hard-code these paths in the header file.

## `0002-Create-SDL2-renderer-as-unaccelerated.patch`

Create SDL2 renderer as unaccelerated


## `0003-Get-rid-of-swscanf-usage.patch`

Get rid of swscanf() usage

This function is currently not implemented in our LibC.

## `0004-Be-a-bit-more-lenient-with-matching-savegame-directo.patch`

Be a bit more lenient with matching savegame directories

Our sscanf() implementation failed to match this case. Making it more
inclusive should be fine, since invalid savegame directories probably
won't contain the right files anyway.

