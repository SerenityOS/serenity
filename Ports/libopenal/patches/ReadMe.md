# Patches for libopenal on SerenityOS

## `0001-Use-int-for-errno-value.patch`

Use int for errno value

On serenity ENOTSUP is provided via `enum ErrnoCode` instead of as a
macro, so `auto` infers the wrong type.

