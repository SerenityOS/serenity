## Name

rev - reverse lines

## Synopsis

```*sh
$ rev [file...]
```

## Description

`rev` reads the specified files line by line, and prints them to standard
output with each line being reversed characterwise. If no files are specified,
then `rev` will read from standard input. If the file `-` is specified then
`rev` also reads from standard input.

## Arguments

-   `file`: Files to print

## Examples

To print two files 'foo' and 'bar' in reverse:

```sh
$ cat foo bar
foo 1
foo 2
bar 1
bar 2
$ rev foo bar
1 oof
2 oof
1 rab
2 rab
```

To list files with their names in reverse:

```sh
$ ls
foo
bar
$ ls | rev
oof
rab
```

To print a file 'foo' in reverse followed by the output of `ls` in reverse:

```sh
$ cat foo
foo 1
foo 2
$ ls
foo
bar
$ ls | rev foo -
1 oof
2 oof
oof
rab
```
