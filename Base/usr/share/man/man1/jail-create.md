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

## Examples

```sh
# Create jail with the name "test-jail", with no PID isolation
$ jail-create test-jail

# Create jail with the name "test-jail", with PID isolation
$ jail-create -p test-jail
```

## See also
* [`jail-attach`(1)](help://man/1/jail-attach)
* [`lsjails`(1)](help://man/1/lsjails)
