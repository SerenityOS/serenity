# Patches for dungeonrush on SerenityOS

## `0001-chdir-to-the-resource-install-path-at-program-startu.patch`

chdir() to the resource install path at program startup

The game tries to open its resource files using relative paths, and we
install them into /opt, so chdir() there.

## `0002-Make-it-use-software-rendering.patch`

Make it use software rendering


