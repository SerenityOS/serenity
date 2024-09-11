## Name

chmod - change file mode

## Synopsis

```**sh
$ chmod <octal-mode> <path...>
$ chmod <mode> <path...>
```

## Description

`chmod` changes mode of all files specified in `path` to `octal-mode` or using symbolic representation.

The symbolic representation format is `[[ugoa][+-=][rwx...],...]`. Multiple symbolic can be given, separated by commas.

The letters `[ugoa]` controls which users' access will be changes: `u` means file owner, `g` file owning group, `o` others, and `a` - all users. If no letter is given, `a` is assumed.

The letters `[+-=]` controls which action will be taken: `+` sets the permission, `-` removes the permission, and `=` sets the mentioned permissions and unsets the other permissions.

The letters `[rwx]` controls which permission will be changes: `r` is read, `w` is write and `x` is execute.

A numeric mode is combination of 1 to 4 numbers. Omitted digits are assumed to be leading zeros. The first digit select the set user ID (4), set group ID (2) and restricted deletion / sticky (1) attributes. The second, third and fourth digit controls permissions of each user group: owner, owning group and others (not owner or owning group), respectively: read (4), write (2) and execute (1).

## Options

-   `-R`, `--recursive`: Change file modes recursively

## Examples

```sh
# Allow full access for owner, read-execute for group, and no access for others, of 'README.md':
$ chmod 750 README.md

# Change '/bin/su' to be set-uid, read-write-execute for owner, only execute for others:
# chmod 4711 /bin/su

# Add read access for others to all files in 'Source' directory:
$ chmod o+r Source/*

# Deny read and write access for non-owners, and allow full access for owners of 'script.sh':
$ chmod o-rw,g-rw,u+rwx script.sh

# Set group access to only read of 'script.sh':
$ chmod g=r script.sh
```

## See also

-   [`chgrp`(1)](help://man/1/chgrp)
-   [`chown`(1)](help://man/1/chown)
