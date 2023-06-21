# Patches for serious-sam-classic on SerenityOS

## `0001-CMake-Remove-march-native.patch`

CMake: Remove `-march=native`

The compiler complains about this:

  error: bad value 'native' for '-march=' switch

However, in the list of supported options, `native` is included - so
there's something funky going on. Leaving this option out lets the game
compile successfully.

## `0002-CMake-Set-the-install-prefix-to-our-staging-prefix.patch`

CMake: Set the install prefix to our staging prefix

This makes sure the binaries are installed to `/usr/local`.

## `0003-SamTFE-Force-__linux__-macro-to-be-1.patch`

SamTFE: Force __linux__ macro to be 1

We are compatible with the Linux version, and setting this macro allows
us to identify as Linux and compile successfully.

## `0004-Engine-Remove-malloc.h.patch`

Engine: Remove malloc.h

This seems to be a non-standard header. The API used by the game already
finds its way through the inclusion of other headers, so leave this out.

## `0005-Engine-Remove-static_assert-on-pthread_t.patch`

Engine: Remove static_assert on pthread_t

In x86_64 Serenity, `sizeof(size_t) == 8` and `sizeof(pthread_t) == 4`.
The game seems to run just fine without this assertion, and changing
`pthread_t` is quite the yak hole.

## `0006-Engine-Add-missing-include-for-sys-select.h.patch`

Engine: Add missing include for <sys/select.h>


## `0007-Engine-Support-running-from-usr-local.patch`

Engine: Support running from `/usr/local`

A lot of logic is hardcoded to `/usr`, so we push it in the right
direction to allow it to run from `/usr/local` instead.

