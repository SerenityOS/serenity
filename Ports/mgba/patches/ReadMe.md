# Patches for mGBA on SerenityOS

## `serenity-does-not-support-futimens-or-futimes.patch`

We do not currently support futimens or futimes. [futimens is a POSIX function,](https://pubs.opengroup.org/onlinepubs/9699919799/)
so this is an issue on our side.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `use-sdl-software-renderer-with-no-vsync.patch`

This makes us use the SDL software renderer with no vsync, as our SDL2 port does not currently support accelerated rendering
or vsync.

### Status
- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack
