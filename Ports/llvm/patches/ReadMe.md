# Patches for LLVM on SerenityOS

## `remove-wstring.patch`

Removes `wstring`s from the source code, as SerenityOS doesn't support them yet.

## `insert-ifdef-serenity.patch`

This patch adds several defines in order to omit things not supported by SerenityOS.
