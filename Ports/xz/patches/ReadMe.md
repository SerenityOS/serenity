# Patches for xz on SerenityOS

## `0001-libtool-Enable-shared-library-support-for-SerenityOS.patch`

libtool: Enable shared library support for SerenityOS

For some odd reason, libtool handles the configuration for shared
libraries entirely statically and in its configure script. If no
shared library support is "present", building shared libraries is
disabled entirely.

Fix that by just adding the appropriate configuration options for
`serenity`. This allows us to finally create dynamic libraries
automatically using libtool, without having to manually link the
static library into a shared library.

## `0002-liblzma-Don-t-assume-getauxval-is-Linux-only.patch`

liblzma: Don't assume getauxval is Linux-only

SerenityOS doesn't expose Linux-compatible hwcaps (like HWCAP_CRC32)
in the auxiliary vector.

