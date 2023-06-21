# Patches for Super-Mario on SerenityOS

## `0001-chdir-to-the-installed-directory-before-execution.patch`

chdir() to the installed directory before execution

The game expects its assets in the current directory, but we install
those to /opt/Super_Mario, so chdir() there at program startup to avoid
crashing.

## `0002-Disable-graphics-acceleration.patch`

Disable graphics acceleration

Disables SDL2 hardware acceleration as we don't support that.

## `0003-Use-pkgconfig-instead-of-find_package-to-look-for-de.patch`

Use pkgconfig instead of find_package() to look for dependencies


## `0004-Fix-a-header-include-path.patch`

Fix a header include path


## `0005-Remove-global-static-initializers.patch`

Remove global static initializers


