## Name

tar - file archiving utility

## Synopsis

```**sh
$ tar [--create] [--extract] [--list] [--verbose] [--gzip] [--file FILE] [PATHS...]
```

## Description

tar is an archiving utility designed to store multiple files in an archive file
(tarball).

Files may also be compressed and decompressed using GNU Zip (GZIP) compression.

## Options

* `-c`, `--create`: Create archive
* `-x`, `--extract`: Extract archive
* `-t`, `--list`: List contents
* `-v`, `--verbose`: Print paths
* `-z`, `--gzip`: compress or uncompress file using gzip
* `-f`, `--file`: Archive file

## Examples

```sh
# List the contents of archive.tar
$ tar -t -f archive.tar

# Extract the contents from archive.tar.gz
$ tar -x -z -f archive.tar.gz

# Extract the contents from archive.tar
$ tar -x -f archive.tar
```

## See also

* [`unzip`(1)](unzip.md)
