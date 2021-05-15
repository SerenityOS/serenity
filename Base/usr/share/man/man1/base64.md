## Name

base64 - encode and decode to base64

## Synopsis

```**sh
$ base64 [-d|--decode] [file]
```

## Description

`base64` encodes or decodes to base64 the data in file `file` or from stdin if
file is not specified or file is `-`.

## Options

* `-d|--decode`: Decode data

## Examples

```sh
# base64 encode the text 'foo'
$ echo 'A' | base64
# base64 encode the content of foo.txt
$ base64 hi.txt
# base64 decode the text 'Zm9v'
$ echo 'Zm9v' | base64 -d
# base64 decode the content of foo.txt
$ base64 -d foo.txt
```
