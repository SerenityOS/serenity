# Patches for cavestory on SerenityOS

## `0001-Removed-references-to-fstat64.patch`

If __FreeBSD__, __APPLE__, __x86_64__, __ppc64__, __CYGWIN__, or __HAIKU__ is not defined it will use "fstat64" to get file status. Currently SerenityOS does not support fstat64, so the code was modified to not perform the check, and instead use "fstat"

The original implementation of utc_minutes_offset would not work for SerenityOS. It tries to generate the offset_seconds by using "tm_gmtoff" which is a BSD extension. This would not work on Serenity so I used the helper functions included in the source code.

## `0003-Updated-the-.gitignore-to-ignore-unpacked-game-extra.patch`

Updated the .gitignore to ignore unpacked game extract files. Removed hardware acceleration from SDL_CreateRenderer


