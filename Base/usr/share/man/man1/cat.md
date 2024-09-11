## Name

cat - concatenate files to stdout

## Synopsis

```**sh
$ cat [file...]
```

## Description

This program passes contents of specified `files` to standard output, in the specified order. If no `file` is specified, or it is `-`, it defaults to standard input.

## Arguments

-   `file`: Files to print

## Examples

```sh
# Display a single file
$ cat README.md
# SerenityOS

Graphical Unix-like operating system for x86 computers.
...

# Display standard input
$ cat
aaa
aaa
bbb
bbb^C

# Display ls output where each file is in separate line
$ ls | cat
Desktop
Documents
Downloads
README.md
Source
js-tests
tests
web-tests

# Display multiple files
$ echo 123 > test.txt
$ echo 456 > test2.txt
cat test.txt test2.txt
123
456
```

## See Also

-   [`head`(1)](help://man/1/head)
-   [`tail`(1)](help://man/1/tail)
-   [`cut`(1)](help://man/1/cut)
