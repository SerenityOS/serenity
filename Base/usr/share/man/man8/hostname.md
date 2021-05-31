## Name

hostname - print or set hostname

## Synopsis

```**sh
$ hostname [hostname]
```

## Description

`hostname` prints current host name. If the `hostname` argument is give and the user is a superuser, the utility sets the current host name to its value. The maximum hostname length is 64 characters. The hostname is not persisted after reboot.

## Examples

```sh
# Print current hostname
$ hostname
courage

# Set the new hostname
# hostname foo

# Print new hostname
# hostname
foo
```
