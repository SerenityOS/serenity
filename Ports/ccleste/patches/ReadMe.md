# Patches for ccleste on SerenityOS

## `fix-data-paths.patch`

Fix data paths to be generated as absolute paths instead of relative ones.
The port assumes that the executable will be run from the folder containing
data files, so it generates `data/*` relative paths.
