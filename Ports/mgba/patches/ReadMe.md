# Patches for mgba on SerenityOS

## `0001-Use-SDL-software-renderer-with-no-vsync.patch`

Use SDL software renderer with no vsync

This makes us use the SDL software renderer with no vsync, as our SDL2 port does not currently support accelerated rendering
or vsync.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

