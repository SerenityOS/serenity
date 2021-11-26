## Name

yes - output a string repeatedly until killed

## Synopsis

```**sh
$ yes [string]
```

## Description

`yes` outputs an endless stream of specified `string` (with trailing newline) in a loop.

## Options

* `string`: String to print; defaults to "yes".

## Examples

```sh
$ yes
yes
yes
yes
yes
yes
yes
yes^C
$ yes t
t
t
t
t
t
t
t
t
t
t^C
```
