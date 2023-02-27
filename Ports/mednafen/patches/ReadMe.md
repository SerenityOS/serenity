# Patches for mednafen on SerenityOS

## `0001-Make-mednafen-compile-with-PIC-PIE.patch`

Make mednafen compile with PIC/PIE

We currently don't support copy relocations and mednafen compiles with
PIC/PIE disabled for performance reasons. This re-enables it and
disables the compiler warning it emits for having it enabled.

## `0002-Replace-PTHREAD_MUTEX_ERRORCHECK-with-PTHREAD_MUTEX_.patch`

Replace PTHREAD_MUTEX_ERRORCHECK with PTHREAD_MUTEX_NORMAL

We currently don't support the PTHREAD_MUTEX_ERRORCHECK mutex type.

