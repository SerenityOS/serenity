## Name

zip - create ZIP archives

## Synopsis

```**sh
$ zip [--recurse-paths] [zip file] [files...]
```

## Description

zip will pack the specified files into a zip archive, compressing them when possible.

The program is compatible with the PKZIP file format specification.

## Examples

```sh
# Zip
$ zip archive.zip file1.txt file2.png
Archive: archive.zip
  adding: file1.txt
  adding: file2.png
```
