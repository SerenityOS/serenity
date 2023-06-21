# Patches for cfunge on SerenityOS

## `0001-Tell-prng.c-that-we-don-t-have-arc4random_buf.patch`

Tell prng.c that we don't have arc4random_buf

FIXME: This function does exist, perhaps outdated or some issue -
       explain the issue here if so.

## `0002-Define-MAX-inline-instead-of-using-sys-param.h.patch`

Define MAX inline instead of using sys/param.h


## `0003-define-_POSIX_MAPPED_FILES.patch`

define _POSIX_MAPPED_FILES

Serenity has a working mmap().

## `0004-Define-_POSIX_REGEXP.patch`

Define _POSIX_REGEXP

Serenity's libc does have regex.

