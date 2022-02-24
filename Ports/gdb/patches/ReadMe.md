# Patches for gdb on SerenityOS

## `0001-gdb-Disable-xmalloc-for-alternate_signal_stack-for-s.patch`

gdb: Disable xmalloc for alternate_signal_stack for serenity


## `0002-serenity-Add-basic-ptrace-based-native-target-for-Se.patch`

serenity: Add basic ptrace based native target for SerenityOS/i386


## `0003-gdb-Add-build-support-for-SerenityOS.patch`

gdb: Add build support for SerenityOS


## `0004-serenity-Fix-compiler-fpermissive-warnings-from-usin.patch`

serenity: Fix compiler -fpermissive warnings from using latest GCC


## `0005-serenity-Implement-custom-wait-override-for-the-sere.patch`

serenity: Implement custom wait override for the serenity_nat_target

While troubleshooting why gdb wasn't working when attempting to debug
serenity programs I noticed two things:

 - The contract of serenity's `waitpid(..)` appears to be slightly
   different than the generic ptrace target expects. We need to make
   sure we pass `WSTOPPED`, and it can return different errno values
   that we would want to re-try on.

-  The contract of serenity's `ptrace(..)` implementation appears to
   diverge as well, as we are expected to call `PT_ATTACH` before we
   call `PT_CONTINUE`, otherwise `ptrace(..)` will just error out.

Allow gdb to understand these differences, I've overloaded the
serenity_nat_target::wait(..) method and added the logic there.

## `0006-serenity-Implement-mourn_inferior-override-for-the-s.patch`

serenity: Implement mourn_inferior override for the serenity_nat_target

We need to pass `WNOHANG` to our `waitpid(..)` call on SerenityOS,
otherwise we will wait forever.

