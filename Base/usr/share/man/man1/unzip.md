## Name

unzip - extract files from a ZIP archive

## Synopsis

```**sh
$ unzip file.zip [files...]
```

## Description

unzip will extract files from a zip archive to the current directory.

The program is compatible with the PKZIP file format specification.

The optional [files] argument can be used to only extract specific files within the archive (using wildcards) during the unzip process. A `_` can be used as a single-character wildcard, and `*` can be used as a variable-length wildcard.

## Options

-   `-d path`, `--output-directory path`: Directory to receive the archive output
-   `-q`, `--quiet`: Be less verbose

## Examples

```sh
# Unzip the contents from archive.zip
$ unzip archive.zip
Archive: archive.zip
 extracting: file1.txt
 extracting: file2.png
```

```sh
# Unzip select files from archive.zip, according to a filter
$ unzip archive.zip "*.tx_"
Archive: archive.unzip
 extracting: file1.txt
```

## See also

-   [`zip`(1)](help://man/1/zip)
-   [`tar`(1)](help://man/1/tar)
