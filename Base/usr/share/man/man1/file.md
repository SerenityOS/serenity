## Name

file - determine type of files

## Synopsis

```**sh
$ file [options...] [files...]
```

## Description

`file` attempts to identify the type of files.

First, an attempt is made to identify a given file based on predetermined binary patterns. If this fails, `file` will fall back to determining the type based on the filename.

## Options

-   `--help`: Display this message
-   `-b`, `--brief`: Do not prepend file names to output lines
-   `-I`, `--mime-type`: Only show mime type.

## Arguments

-   `files`: Files to identify

## Examples

```sh
# Identify a file
$ file Buggie.png
Buggie.png: PNG image data, 64 x 138
# Identify all files in the current directory, and show only the mime type.
$ file -I *
```
