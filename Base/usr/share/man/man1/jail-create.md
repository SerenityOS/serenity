## Name

jail-create - create a new jail

## Synopsis

```**sh
$ jail-create <name>
```

## Description

`jail-create` creates a new jail, with a specified name

## Options

* `-p`, `--pid-isolation`: Use PID-isolation (as a custom isolation option)
* `-l`, `--unveil-isolation`: Use unveil-isolation (as a custom isolation option)
* `-u`, `--path`: Unveil a path, with the format of `permissions,path`

## Examples

```sh
# Create jail with the name "test-jail", with no PID isolation
$ jail-create test-jail

# Create jail with the name "test-jail", with PID isolation
$ jail-create -p test-jail

# Create jail with the name "test-jail", with many unveiled paths (and enforcing them on all processes in jail)
$ jail-create -l --path=r,/etc/timezone --path=r,/usr/lib --path=r,/sys/ --path=r,/etc/passwd --path=r,/etc/group test-jail
```
