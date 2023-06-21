# Patches for ccache on SerenityOS

## `0001-Do-not-define-ESTALE-in-config.h.in.patch`

Do not define ESTALE in config.h.in

In ccache, `config.h` is included by the `-include` compiler option,
which means that it is included before any libc headers. This doesn't
cause any problems on systems that redefine this macro when libc headers
are included, but on Serenity, it breaks our definition of the
`ErrnoCode` enum.

