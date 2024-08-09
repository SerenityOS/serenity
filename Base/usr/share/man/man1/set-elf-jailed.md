## Name

set-elf-jailed - change an ELF executable to be auto-jailed

## Synopsis

```**sh
$ set-elf-jailed [path]
```

## Description

`set-elf-jailed` changes a dynamically linked ELF executable's dynamic loader to `/usr/lib/ldjail.so`,
so when running the ELF executable it would be auto-jailed.

## Arguments

* `path`: Path of a dynamically linked ELF executable

## Examples

```sh
# Set /bin/ls to be auto-jailed
$ set-elf-jailed /bin/ls
# Set /bin/BuggieBox to be auto-jailed
$ set-elf-jailed /bin/BuggieBox
```
