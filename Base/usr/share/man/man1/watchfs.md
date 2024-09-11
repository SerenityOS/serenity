## Name

watchfs - watch a file or directory being changed

## Synopsis

```**sh
$ watchfs [options...] [path...]
```

## Description

`watchfs` watches files and directories being changed.

## Options

-   `--help`: Display this message
-   `-E`, `--exit-after-change`: Wait for first change and exit
-   `-a`, `--watch-all-events`: Watch all types of events
-   `-d`, `--watch-delete-events`: Watch file deletion events
-   `-m`, `--watch-file-modify-events`: Watch file content being modified
-   `-M`, `--watch-file-metadata-events`: Watch file metadata being modified
-   `-c`, `--watch-directory-child-creation-events`: Watch directory child creation events
-   `-D`, `--watch-directory-child-deletion-events`: Watch directory child deletion events

## Arguments

-   `path`: Files and/or directories to watch

## Examples

```sh
# watch /tmp with all events being handled (child creation and deletion)
$ watchfs -a /tmp/
# watch /tmp with child creation events being handled
$ watchfs -c /tmp/
# watch /tmp with child creation events being handled
$ watchfs -D /tmp/

# watch /tmp with all events being handled (child creation and deletion) and exit after first change
$ watchfs -E /tmp/

# watch /tmp/test_file with all events being handled (file being deleted, metadata being modified or content modified)
$ watchfs -a /tmp/test_file
# watch /tmp/test_file being deleted
$ watchfs -d /tmp/test_file
# watch /tmp/test_file being metadata-modified
$ watchfs -M /tmp/test_file
# watch /tmp/test_file being content-modified
$ watchfs -m /tmp/test_file
```

## See also

-   [`listdir`(1)](help://man/1/listdir) to list directory entries
-   [`ls`(1)](help://man/1/ls) to list directory contents
