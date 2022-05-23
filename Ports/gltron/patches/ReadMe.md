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


## `0007-SDL-Fix-2x-audio-rate-issue.patch`

SDL: Fix 2x audio rate issue

By not passing in an `obtained` struct into `SDL_OpenAudio`, we ask SDL
to perform any sample rate and/or format conversion for us. Previously
GLTron would simply ignore the result in `obtained`, causing the audio
to be played back at 2x speed.

