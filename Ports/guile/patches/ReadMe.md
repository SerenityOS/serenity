# Patches for guile on SerenityOS

## `0001-build-When-cross-compiling-get-type-sizes-of-the-tar.patch`

build: When cross-compiling, get type sizes of the target system.

Fixes <https://issues.guix.gnu.org/54198>.

As noted in the comment at the top, 'SIZEOF_TYPE' must be used instead
of 'sizeof (TYPE)' to support cross-compilation.

The regression was introduced in
5e5afde06fd9dd0992294d6c7dc9f9966c0caa37 but only became apparent with
717e787da6ae75bbaa53139c0ef3791cd758a9d8.

* libguile/gen-scmconfig.c (main): Replace uses of 'sizeof' by
references to the SIZEOF_* macros.
* configure.ac: Add 'AC_CHECK_SIZEOF' call for 'intmax_t'.

## `0002-Remove-contents-of-return_unused_stack_to_os.patch`

Remove contents of return_unused_stack_to_os

guile attempts to madvise(2) away parts of the stack, but serenity only
supports madvise(2) on entire mmaped regions.

