## Name

jail-configure - configure an existing jail

## Synopsis

```**sh
$ jail-configure [OPTIONS] <jail index>
```

## Description

`jail-configure` configures an existing jail, with a specified jail index.

## Options

* `-c`, `--set-clean-on-last-detach`: Set jail to clean itself on last detach (may immediately clean the jail if no process is attached)
* `-s`, `--unset-clean-on-last-detach`: Unset jail to clean itself on last detach

## Examples

```sh
# Configure jail (index 1) to not clean itself on last process detach
$ jail-configure --unset-clean-on-last-detach 1
```

```sh
# Configure jail (index 1) to clean itself on last process detach (this may clean it immediately if attach count is 0)
$ jail-configure --unset-clean-on-last-detach 1
```
