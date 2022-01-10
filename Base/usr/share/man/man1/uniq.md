## Name

uniq - filter out repeated lines

## Synopsis

```**sh
$ uniq [--version] [INPUT] [OUTPUT]
```

## Description

Filter out repeated lines from INPUT (or standard input) and write to OUTPUT (or standard output).

## Options

* `--help`: Display help message and exit
* `--version`: Print version

## Examples

```sh
# Filter out repeated lines from README.md and write to standard output
$ uniq README.md

# Filter out repeated lines from README.md and write to UNIQUE.md
$ uniq README.md UNIQUE.md
```
