# Patches for quake3 on SerenityOS

## `0001-Meta-Refactor-Makefile-to-support-Serenity.patch`

Meta: Refactor Makefile to support Serenity


## `0002-Engine-Add-Serenity-so-q_platform.h.patch`

Engine: Add Serenity so `q_platform.h`


## `0003-Engine-Add-sys-select.h-include-for-Serenity.patch`

Engine: Add `<sys/select.h>` include for Serenity

QuakeIII makes extensive use of the `select()` syscall for its' netcode.
It seems that Linux has this in a header that isn't `<sys/select.h>`
like us, which results in an implicit declaration error.

## `0004-Meta-Add-ldl-library-for-Serenity-target.patch`

Meta: Add `-ldl` library for Serenity target


## `0005-Engine-Move-ifdef-to-more-sensible-location.patch`

Engine: Move `#ifdef` to more sensible location

No linker errors in this dojo!

## `0006-Meta-Add-ARCH-to-TOOLS_CFLAGS.patch`

Meta: Add ARCH to TOOLS_CFLAGS


## `0007-Meta-Remove-extension-from-main-game-exe.patch`

Meta: Remove extension from main game exe


## `0008-Engine-Use-Serenity-style-PROT_EXEC-mmap.patch`

Engine: Use Serenity style PROT_EXEC mmap

The code for this was really old and crusty, so let's `ifdef` it out and
use some more friendly Serenity style memory code.

