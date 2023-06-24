# Patches for nnn on SerenityOS

## `0001-Remove-needless-include-of-ftw.patch`

Remove needless include of ftw.h

This header does not exist on Serenity, but it seems to be unused.

## `0002-Link-with-fts-library.patch`

Link with fts library

Nnn depends on the non-standard fts functions to traverse the file
system. Most BSD systems provide them out of the box and on
GNU/Linux systems, they are part of glibc.

On Serenity, they are provided by a separate library (the libfts port)
that was originally intended for musl-based Linux systems.
This patch makes nnn link against this library.

## `0003-Disable-broken-features.patch`

Disable broken features

Mouse support is currently broken and X11 does not make sense on Serenity.

