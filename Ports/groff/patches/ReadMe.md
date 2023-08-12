# Patches for groff on SerenityOS

## `0001-Include-config.h-in-math.h.patch`

Include config.h in math.h

Otherwise the build will fail due to #define(s) not being included.

## `0002-Don-t-redeclare-signbit-function.patch`

Don't redeclare signbit function

We have it already implemented so simply use what we have.

