# Patches for guile on SerenityOS

## `0001-Remove-contents-of-return_unused_stack_to_os.patch`

Remove contents of return_unused_stack_to_os

guile attempts to madvise(2) away parts of the stack, but serenity only
supports madvise(2) on entire mmaped regions.
