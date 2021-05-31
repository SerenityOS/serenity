## Name

basename - strip directory names from path

## Synopsis

```**sh
$ basename <path>
```

## Description

`basename` prints basename (filename, stripped of preceding directory names) of specified `path` to standard output. The path does not have to exist.

## Arguments

* `path`: The path which we want to get basename of

## Examples

```sh
$ basename /
/
$ basename ~
anon
$ basename Source/js/array.js
array.js
```
