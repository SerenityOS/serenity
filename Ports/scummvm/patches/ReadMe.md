# Patches for scummvm on SerenityOS

## `0001-Prevent-call-to-glGetIntegerv-without-context.patch`

Prevent call to `glGetIntegerv` without context

This call to `SDL_GL_GetAttribute` happens when switching from the
launcher to the game, when no GL context may exist. This caused Grim
Fandango to crash almost immediately.

Since this is for MSAA which we do not yet support, patch it out.

## `0002-Teach-configure-about-serenity.patch`

Teach configure about serenity


