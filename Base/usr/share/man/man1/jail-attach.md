## Name

jail-attach - attach a new process to existing jail

## Synopsis

```**sh
$ jail-attach <jail index> <command>
```

## Description

`jail-attach` attaches a new process by specifying a command, to an existing jail, with a
specified jail index.

## Examples

```sh
# Attach the command "ps -ef" to a jail with the index 0
$ jail-attach 0 ps -ef
```
