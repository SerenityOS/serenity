## Name

base64 - encode and decode to base64

## Synopsis

```**sh
$ base64 [--decode] [--wrap column] [file]
```

## Description

`base64` encodes or decodes to base64 the data in file `file` or from stdin if
file is not specified or file is `-`.

## Options

-   `-d`, `--decode`: Decode data
-   `-w column`, `--wrap column`: When encoding, wrap output after `column` characters

## Examples

```sh
# base64 encode the text 'A'
$ echo 'A' | base64
# base64 encode the content of hi.txt
$ base64 hi.txt
# base64 encode the content of baz.txt and wrap after 4 columns
$ base64 -w 4 baz.txt
# base64 decode the text 'Zm9v'
$ echo 'Zm9v' | base64 -d
# base64 decode the content of foo.txt
$ base64 -d foo.txt
```
