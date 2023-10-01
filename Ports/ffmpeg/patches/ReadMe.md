# Patches for ffmpeg on SerenityOS

## `0001-Assume-that-EDOM-exists.patch`

Assume that EDOM exists

Since errno values are not numeric constants on serenity, this won't
work in cpp, assume that it exists.

## `0002-Adapt-to-the-Serenity-thread-name-interface.patch`

Adapt to the Serenity thread name interface


## `0003-avcodec-x86-mathops-clip-constants-used-with-shift-i.patch`

avcodec/x86/mathops: clip constants used with shift instructions within inline assembly

Fixes assembling with binutil as >= 2.41

Signed-off-by: James Almer <jamrial@gmail.com>

