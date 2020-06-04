## Name

find - recursively search for files

## Synopsis

```**sh
$ find [-L] [root-path] [commands...]
```

## Description

`find` recursively traverses the file hierarchy starting at the given root path
(or at the current working directory if the root path is not specified), and
evaluates the given commands for each found file. The commands can be used to
both filter the set of files and to perform actions on them.

If no *action command* (`-print`, `-print0`, or `-exec`) is found among the
specified commands, a `-print` command is implicitly appended.

## Options

* `-L`: Follow symlinks

## Commands

* `-type t`: Checks if the file is of the specified type, which must be one of
  `b` (for block device), `c` (character device), `d` (directory), `l` (symbolic
  link), `p` (FIFO), `f` (regular file), and `s` (socket).
* `-links number`: Checks if the file has the given number of hard links.
* `-user name`: Checks if the file is owned by the given user. Instead of a user
  name, a numerical UID may be specified.
* `-group name`: Checks if the file is owned by the given group. Instead of a
  group name, a numerical GID may be specified.
* `-size number[c]`: Checks if the file has the given size in 512-byte blocks,
  rounded up. If the size is followed by the `c` character, checks if the file
  has the given size in bytes.
* `-print`: Outputs the file path, followed by a newline. Always evaluates to
  true.
* `-print0`: Outputs the file path, followed by a zero byte. Always evaluates to
  true.
* `-exec command... ;`: Executes the given command with any arguments provided,
  substituting the file path for any arguments specified as `{}`. The list of
  arguments must be terminated by a semicolon. Checks if the command exits
  successfully.

The commands can be combined to form complex expressions using the following
operators:

* `command1 -o command2`: Logical OR.
* `command1 -a command2`, `command1 command2`: Logical AND.
* `( command )`: Groups commands together for operator priority purposes.

## Examples

```sh
# Output a tree of paths rooted at the current directory:
$ find
# Output only directories:
$ find -type d
# Remove all sockets and any files owned by anon in /tmp:
$ find /tmp "(" -type s -o -user anon ")" -exec rm "{}" ";"
# Concatenate files with weird characters in their names:
$ find -type f -print0 | xargs -0 cat
```

## See also

* [`xargs`(1)](xargs.md)
