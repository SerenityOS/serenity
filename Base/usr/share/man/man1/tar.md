## Name

tar - file archiving utility

## Synopsis

```**sh
$ tar [--create] [--extract] [--list] [--verbose] [--gzip] [--no-auto-compress] [--directory DIRECTORY] [--file FILE] [PATHS...]
```

## Description

tar is an archiving utility designed to store multiple files in an archive file
(tarball).

Files may also be compressed and decompressed using GNU Zip (GZIP) compression.

## Options

-   `-c`, `--create`: Create archive
-   `-x`, `--extract`: Extract archive
-   `-t`, `--list`: List contents
-   `-v`, `--verbose`: Print paths
-   `-z`, `--gzip`: Compress or decompress file using gzip
-   `--lzma`: Compress or decompress file using lzma
-   `-J`, `--xz`: Compress or decompress file using xz
-   `--no-auto-compress`: Do not use the archive suffix to select the compression algorithm
-   `-C DIRECTORY`, `--directory DIRECTORY`: Directory to extract to/create from
-   `-f FILE`, `--file FILE`: Archive file

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

-   [`unzip`(1)](help://man/1/unzip)
