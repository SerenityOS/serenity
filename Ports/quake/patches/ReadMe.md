# Patches for quake on SerenityOS

## `0001-Replace-MAXHOSTNAMELEN-references-with-HOST_NAME_MAX.patch`

Replace MAXHOSTNAMELEN references with HOST_NAME_MAX

MAXHOSTNAMELEN is not defined in SerenityOS, but HOST_NAME_MAX is
(under limits.h). Therefore, change all relevant references to point
to HOST_NAME_MAX.

## `0002-Link-against-SerenityOS-SDL2-and-LibGL.patch`

Link against SerenityOS SDL2 and LibGL

When specifying TARGET_OS=UNIX and TARGET_UNIX=serenity, use the
SerenityOS library paths for SDL2 and LibGL.

## `0003-Remove-non-existent-header-sys-ipc.h.patch`

Remove non-existent header sys/ipc.h

sys/ipc.h is not available in SerenityOS. Commenting it out fixes the
build and doesn't seem to cause any obvious errors.

