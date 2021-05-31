## Name

copy - copy text to the clipboard

## Synopsis

```**sh
$ copy [options...] [text...]
```

## Description

`copy` copies text from stdin or command-line argument (`text`) to the system clipboard. The clipboard is managed by WindowServer.

## Options

* `-t type`, `--type type`: MIME type of data stored in clipboard. The default type is `text/plain`.
* `-c`, `--clear`: Clear the clipboard instead of copying.

## Examples

```sh
# Copy some image to clipboard
$ cat image.png | copy -t image/png

# Place text 'foo' in clupboard
$ copy foo
```
