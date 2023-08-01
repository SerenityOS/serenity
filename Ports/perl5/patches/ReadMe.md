# Patches for perl5 on SerenityOS

## `0001-configure-Hardcode-random-in-place-of-drand48.patch`

configure: Hardcode `random()` in place of `drand48()`


## `0002-pp_sys-Avoid-redefining-h_errno.patch`

pp_sys: Avoid redefining `h_errno`


## `0003-Makefile-Don-t-run-lib-unicore-mktables-with-maketes.patch`

Makefile: Don't run `lib/unicore/mktables` with -maketest

This argument causes the interpreter to hang and consume huge amounts
of RAM.

## `0004-Disable-nanosleep.patch`

Disable nanosleep


## `0005-configure-Add-hint-for-serenity.patch`

configure: Add hint for `serenity`


