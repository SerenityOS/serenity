# Patches for ffmpeg on SerenityOS

## `0001-Assume-that-EDOM-exists.patch`

Assume that EDOM exists

Since errno values are not numeric constants on serenity, this won't
work in cpp, assume that it exists.

## `0002-Adapt-to-the-Serenity-thread-name-interface.patch`

Adapt to the Serenity thread name interface


