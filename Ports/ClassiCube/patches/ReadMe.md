# Patches for ClassiCube on SerenityOS

## `0001-Add-support-for-SerenityOS.patch`

Add support for SerenityOS


## `0002-HACK-Disable-interrupt-hooks.patch`

HACK: Disable interrupt hooks

This allows crashes to be handled by Serenity's crash handler, which
automatically displays debugging information such as the backtrace.

