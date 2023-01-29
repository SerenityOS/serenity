# Patches for pfetch on SerenityOS

## `0001-Use-bin-bash-as-the-shebang.patch`

Use /bin/bash as the shebang

/bin/sh is a symlink to Shell, which is not sh-compatible.

