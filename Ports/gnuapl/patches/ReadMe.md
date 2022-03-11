# Patches for GNU APL on SerenityOS

## `fix-common-includes.patch`

`fcntl.h` was included as `sys/fcntl.h`, which is not where this lives in Serenity.

Also `sys/select.h` is included here.

## `stub-performance-macro.patch`

The Macro for performance reporting was throwing compile errors, so we just stub it out.

## `stub-sbrk.patch`

Again, for performance reporting the function `sbrk` is needed which we don't have. We just stub it out.

## `sub-config.patch`

The default change to `config.sub`: Add `serenity` as a valid target.
