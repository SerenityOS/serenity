## Name

grep - Find lines in files

## Synopsis

```**sh
$ grep [options...] <pattern> [files...]
```

## Description

`grep` searches in `files` (or standard input) for lines containing `pattern`.
`pattern` is a POSIX basic regular expression. For extended regular expressions, use `-E`.

## Options

* `-r`, `--recursive`: Recursively scan files
* `-E`, `--extended-regexp`: Use extended regular expressions
* `-e pattern`, `--regexp pattern`: pattern
* `-i`: Make matches case-insensitive
* `-n`, `--line-numbers`: Output line-numbers
* `-v`, `--invert-match`: Select non-matching lines
* `-q`, `--quiet`: Do not write anything to standard output
* `-s`, `--no-messages`: Suppress error messages for nonexistent or unreadable files
* `--binary-mode`: Action to take for binary files: binary (default), text, skip
* `-a`, `--text`: Treat binary files as text (same as `--binary-mode text`)
* `-I`: Ignore binary files (same as `--binary-mode skip`)
* `--color WHEN`: When to use colored output for the matching text: auto (default), never, always
* `-c`, `--count`: Output line count instead of line contents

## Arguments

* `files`: Files to process, or standard input

## Examples

Grep lines containing "FIXME":
```sh
$ grep FIXME Source/serenity
```

Grep lines not starting with "bool " in the current directory:
```sh
$ grep -rv '^bool '
```

Count comments in the Kernel:
```sh
grep -rc ^// Kernel/
```

## See also

* [POSIX Regular Expressions](https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap09.html)

