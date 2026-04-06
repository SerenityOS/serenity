# Patches for editline on SerenityOS

## `0001-include-sys-select.h-in-excallback.patch`

include <sys/select.h> in excallback

Fixes an build issue when cross-compiling for SerenityOS[1]

[1] https://github.com/SerenityOS/serenity

