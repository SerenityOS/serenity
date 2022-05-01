# Patches for halflife

## `fwgs-add-serenity.patch`

Add SerenityOS to the supported architectures of FWGS.

## `fwgs-dont-format-nan-loop.patch`

This keeps FWGS from formatting a NaN value multiple times each frame,
which would otherwise result in a big performance hit.

## `hlsdk-add-serenity.patch`

Add SerenityOS to the supported architectures of hlsdk.

## `hlsdk-strings-compat.patch`

This bypasses a bunch of `str[n]cmpcase` errors that occur due to weird LibC compatibility problems.

