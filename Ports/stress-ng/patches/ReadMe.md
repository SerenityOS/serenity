# Patches for stress-ng on SerenityOS

## `0001-serenity-Disable-linux-scheduler-integration-on-Sere.patch`

serenity: Disable linux scheduler integration on Serenity

Follow the path of other platforms, and make this code nop
when compiling for serenity.

## `0002-serenity-Disable-itimer-testing-when-compiling-for-S.patch`

serenity: Disable itimer testing when compiling for Serenity

The itimer APIs are not implemented in serenity, so just disable
these tests.

## `0003-serenity-Fix-duplicate-definition-of-ALWAYS_INLINE-o.patch`

serenity: Fix duplicate definition of ALWAYS_INLINE on serenity


## `0004-serenity-ifdef-out-key_t-type-usage-which-serenity-d.patch`

serenity: ifdef out key_t type usage, which serenity does not have


## `0005-serenity-Mark-hsearch-stressor-as-not-implemented-on.patch`

serenity: Mark hsearch stressor as not implemented on Serenity


## `0006-serenity-Disable-signal-code-validation-for-Serenity.patch`

serenity: Disable signal code validation for Serenity

We don't currently have a definition for SEGV_ACCERR, so
this validation is meaningless for SerenityOS.

## `0007-serenity-Disable-rand48-cpu-stressor-it-s-not-implem.patch`

serenity: Disable rand48 cpu stressor, it's not implemented for Serenity


## `0008-serenity-Make-lsearch-stressor-a-nop-on-Serenity.patch`

serenity: Make lsearch stressor a nop on Serenity


## `0009-serenity-Fake-O_SYNC-for-serenity-so-iomix-stressor-.patch`

serenity: Fake O_SYNC for serenity so iomix stressor compiles

Serenity doesn't yet support O_SYNC, so just make it compile.

## `0010-serenity-Disable-lrand48-zlib-stress-it-is-not-imple.patch`

serenity: Disable lrand48 zlib stress, it is not implemented for Serenity


## `0011-serenity-Fix-stress_get_prime64-variable-type-uint-u.patch`

serenity: Fix stress_get_prime64 variable type, uint -> uint64_t

Serenity doesn't have a definition for unit.

## `0012-Makefile-Install-to-usr-local.patch`

Makefile: Install to /usr/local


