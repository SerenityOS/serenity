## Name

groupadd - add a new group to the system group file

## Synopsis

```**sh
# groupadd [options] <group>
```

## Description

This program adds a new group to the system.

This program must be run as root.

## Options

* `-g`, `--gid` _gid_: The group identifier for the new group. If not specified, an unused GID above `100` will be auto-generated.

## Exit Values

* 0 - Success
* 1 - Couldn't update the group file
* 3 - Invalid argument to option
* 4 - GID already in use

## Files

* `/etc/group` - new group information (such GID) is appended to this file.

## Examples

```sh
# groupadd -g 110 contributors
# groupadd maintainers
```

