## Name

slugify - text to slug transform utility

## Synopsis

```**sh
$ slugify [--format FORMAT] [--glue GLUE] [--single-page] [INPUTS...]
```

## Description

Slugify is a simple text to slug transform utility and prints the result.

## Options

-   `-f`, `--format`: Output format to choose from 'md', 'html', 'plain'. (default: md)
-   `-g`, `--glue`: Specify delimiter (_single character only_) to join the parts. (default: -)
-   `-s`, `--single-page`: Prepends hash/pound (#) to the slugified string when set, otherwise slash (/). Useful for markdowns like in GitHub (default: false)

## Examples

```sh
$ slugify 'Serenity is a cool ### PROject123.'
```
