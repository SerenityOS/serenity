## Name

tree - List files as a tree

## Synopsis

```**sh
$ tree [options]... [directories...]
```

## Description

`tree` lists directories and files in specified `directories`, in a colored tree view.
It also counts them and prints a summary at the bottom.
If `directories` is unspecified, it defaults to `.`.

## Options

* `-a`, `--all`: Show hidden files
* `-d`, `--only-directories`: Show only directories
* `-L level`, `--maximum-depth level`: Maximum depth of the tree

## Arguments

* `directories`: Directories to print (or the current directory)

## Examples

List files in the current directory:
```sh
$ tree
```

List all files on the system:
```sh
$ tree -a /
```

