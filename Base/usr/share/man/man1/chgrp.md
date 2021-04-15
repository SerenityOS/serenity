## Name

chgrp - Change file group

## Synopsis

```**sh
$ chgrp <gid> <path>
$ chgrp <name> <path>
```

## Description

`chgrp` called as root or as file owner will change owning group of specified `path` to `gid` or `name`.

## Options

* `-m`: Mute all audio.
* `-M`: Unmute all audio.

## Examples

```sh
# Change group of README.md to 111
# chgrp 111 README.md

# Change group of README.md to root
# chgrp root README.md
```

## See also

* [`chmod`(1)](chmod.md)
* [`chown`(1)](chown.md)
