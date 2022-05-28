# Patches for tuxracer on SerenityOS

## `0001-Fix-config-macro-reference-syntax.patch`

Fix macro definitions using old syntax for referring to struct fields

## `0002-Disable-full-screen.patch`

Disable full screen by default (start in windowed mode)

## `0003-Exit-event-loop-on-SDL_QUIT-event.patch`

Check for SDL_QUIT event and exit on its reception
Allows the game to close from the window button instead from the menu only
