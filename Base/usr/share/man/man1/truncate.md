## Name

truncate - Expand or shrink a file

## Synopsis

```**sh
$ truncate [-d size|-r reference] [file]
```

## Description

`truncate` truncate `file` to the size of `reference`, or to `size`.
`size` may be prefixed with a `+` or a `-`, to expand or to shrink by `size`, respectively.
To increase the size of `file`, zeroes are appended to it.

## Options

* `-s size`, `--size size`: Resize the target file to (or by) this size
* `-r file`, `--reference file`: Resize the target file to match the size of this one

## Arguments

* `file`: File path

## Examples

Create a 1KiB file:
```sh
$ truncate -s 1024 file
```

Remove 10MB at the end of `file.dat`:
```sh
$ truncate -s-1000000 file.dat
```

Truncate `100-bytes.txt` so that it has the same size as `10-bytes.txt`:
```sh
$ truncate -r 10-bytes.txt 100-bytes.txt
```

