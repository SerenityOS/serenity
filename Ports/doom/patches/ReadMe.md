# Patches for doom on SerenityOS

## `0001-Add-SerenityOS-platform-support.patch`

Add SerenityOS platform support

Add a menubar and allow switching in/out of fullscreen mode
Support the ALT key for strafing :^)


## `0002-Use-an-8-bit-palette-indexed-GraphicsBitmap-for-the-.patch`

Use an 8-bit palette-indexed GraphicsBitmap for the game screen buffer

This allows us to use the optimized upscaling code in Serenity instead
of the non-optimized code in DOOM.

Remove an unnecessary extra screen buffer copy

We can just draw the game directly into the DG_ScreenBuffer and save
ourselves a 64000-byte memcpy() per rendered frame.

## `0003-Integrate-better-with-Serenity-s-LibGUI-event-loop.patch`

Integrate better with Serenity's LibGUI event loop

Let the DOOM event loop drive the Serenity event loop, and let DOOM's
I_FinishUpdate drive repaints.

The one weird case is "screen wipes", where we have to manually pump
the event loop a little extra, to make sure the wipe effect shows up
on screen.

This patch makes the game way more fluid than before. :^)

## `0004-Actually-exit-after-showing-an-error-message-instead.patch`

Actually exit after showing an error message (instead of infinite loop)

Fixes SerenityOS/serenity #1527.

