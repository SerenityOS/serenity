# Patches for perl5 on SerenityOS

## `0001-pp_sys-Avoid-redefining-h_errno.patch`

pp_sys: Avoid redefining `h_errno`


## `0002-Makefile-Don-t-run-lib-unicore-mktables-with-maketes.patch`

Makefile: Don't run `lib/unicore/mktables` with -maketest

This argument causes the interpreter to hang and consume huge amounts
of RAM.

## `0003-Disable-nanosleep.patch`

Disable nanosleep


## `0004-configure-Add-hint-for-serenity.patch`

configure: Add hint for `serenity`


