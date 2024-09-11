## Name

zip - create ZIP archives

## Synopsis

```**sh
$ zip [--recurse-paths] [zip file] [files...]
```

## Description

zip will pack the specified files into a zip archive, compressing them when possible.

The program is compatible with the PKZIP file format specification.

## Options

-   `-r`, `--recurse-paths`: Travel the directory structure recursively
-   `-f`, `--force`: Overwrite existing zip file

## Examples

```sh
# Zip
$ zip archive.zip file1.txt file2.png
Archive: archive.zip
  adding: file1.txt
  adding: file2.png
```

## See also

-   [`unzip`(1)](help://man/1/unzip)
-   [`gzip`(1)](help://man/1/gzip)
-   [`tar`(1)](help://man/1/tar)
