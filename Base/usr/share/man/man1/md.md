## Name

md - render markdown documents

## Synopsis

```**sh
$ md [--html] [input-file.md]
```

## Description

Read a Markdown document and render it using either terminal
escape sequences (the default) or HTML. If a file name is given,
`md` reads the document from that file; by default it reads its
standard input.

## Options

* `--html`: Render the document into HTML.

## Examples

Here's how you can render this man page into HTML:

```sh
$ md --html /usr/share/man/man1/md.md
```
