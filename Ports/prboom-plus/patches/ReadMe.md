# Patches for prboom-plus on SerenityOS

## `0001-Remove-WAD-data-targets-from-build.patch`

Remove WAD / data targets from build

We're crosscompiling but PRBoom+ still tries to invoke the rdatawad
tool, which we're supposed to get from a different native build.

We download the PRBoom WAD separately, so we remove it from the build.

