## Name

drain - Print file to stdout, while progressively deleting read segments

## Synopsis

```**sh
$ drain [file]
```

## Description

drain is a utility for simultaneously reading and deleting a file. It is
useful, for example, when working with large files on systems with low disk space.
drain will read blocks from the provided file, and write each block to
the standard output stream before removing that block from the file.
The output may be redirected to another file or utility for further processing.

## Options

-   `-b`, `--block-size`: Base block size [in KiB] to be used during the utility operation, default is 256 KiB

## Arguments

-   `file`: File to be read

## Warning

In order to progressively delete the file, drain reverses the file in-place to truncate blocks after they are read.
Thus, it is implicitly unsafe to interrupt the utility. If the operation is interrupted, or otherwise fails,
the input file is unlikely to be recoverable.

## Examples

```sh
$ drain my-big-tar-file.tar | tar -x -C my-big-tar-file-extracted
$ drain -b 1 my-small-tar-file.tar | tar -x -C my-small-tar-file-extracted
```
