# Patches for sdl12-compat on SerenityOS

## `0001-Disable-forced-fullscreen-on-logical-scaling.patch`

Disable forced fullscreen on logical scaling

sdl12-compat forces fullscreen on anything that sets video mode
and uses OpenGL logical scaling, causing rapid flickering and preventing
execution. Not sure if this is an upstream bug or intended behavior,
but disabling fullscreen at this point fixes the flickering.

