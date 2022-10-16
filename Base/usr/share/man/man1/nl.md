## Name

nl - Number lines of files

## Synopsis

```sh
$ nl [options]... [files]...
```

## Options:

* `-b style`, `--body-numbering style`: Line numbering style: 't' for non-empty lines, 'a' for all lines, 'n' for no lines
* `-i number`, `--increment number`: Line count increment
* `-s string`, `--separator string`: Separator between line numbers and lines
* `-v number`, `--startnum number`: Initial line number
* `-w number`, `--width number`: Number width

## Arguments:

* `files`: Files to process, or standard input

## Examples

Number lines in README.md:
```sh
$ nl README.md
```

Number lines, but start at 42:
```sh
$ nl -v 42 README.md
```

Separate numbers from lines with an arrow:
```sh
$ echo "Well\nHello\nFriends!" | nl -s " --> "
```
