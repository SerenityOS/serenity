## Name

jail-attach - attach a new process to existing jail

## Synopsis

```**sh
$ jail-attach <jail index> <command>
```

## Description

`jail-attach` attaches a new process by specifying a command, to an existing jail, with a
specified jail index.

## Options

* `-E`, `--preserve-env`: Preserve user environment when running command
* `-i`, `--jail-index`: Use an already existing jail with its index
* `-n`, `--jail-name`: Create a new jail with a provided name

## Examples

```sh
# Attach the command "ps -ef" to an already existing jail with the index 0
$ jail-attach -i 0 ps -ef
```

```sh
# Attach the command "/bin/Shell" to a new jail with the name "test jail"
$ jail-attach -n "test jail" /bin/Shell
```

## See also
* [`jail-create`(1)](help://man/1/jail-create)
* [`lsjails`(1)](help://man/1/lsjails)
