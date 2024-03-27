# Patches for libfuse on SerenityOS

## `0001-Disable-unsupported-functionality.patch`

Disable unsupported functionality

Note that replacing the *at variants of POSIX functions with their
absolute counterparts is entirely valid here since we disable all
examples that require handling of relative paths anyway.

## `0002-Install-examples.patch`

Install examples

## `0003-Teach-the-mount-helpers-about-serenity.patch`

Teach the mount helpers about serenity

