# Patches for mgba on SerenityOS

## `0001-Remove-use-of-futime-n-s.patch`

Remove use of futime(n)s

We do not currently support futimens or futimes. [futimens is a POSIX function,](https://pubs.opengroup.org/onlinepubs/9699919799/)
so this is an issue on our side.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

## `0002-Use-SDL-software-renderer-with-no-vsync.patch`

Use SDL software renderer with no vsync

This makes us use the SDL software renderer with no vsync, as our SDL2 port does not currently support accelerated rendering
or vsync.

- [ ] Local?
- [ ] Should be merged to upstream?
- [X] Resolves issue(s) with our side of things
- [ ] Hack

