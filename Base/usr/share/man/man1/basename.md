## Name

basename - print basename of file

## Synopsis

```**sh
$ basename <path>
```

## Description

`basename` will print basename (filename, stripped of preceding directory names) of specified `path` to standard output. The path does not have to exist.

## Options

* `-l`, `--loop`: Loop playback

## Arguments

* `path`: Path to audio file

## Examples

```sh
$ basename /
/
$ basename ~
anon
$ basename Source/js/array.js
array.js
```
