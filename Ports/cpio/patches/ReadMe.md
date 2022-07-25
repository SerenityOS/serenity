# Patches for cpio on SerenityOS

## `0001-Use-global-program_name-variable-from-gnu-dir.patch`

Use global program_name variable from gnu dir

Without this patch being applied, there would be a conflict between the
variables between the 2 locations, so it will not compile otherwise.
