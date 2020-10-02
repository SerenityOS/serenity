## Name

test - checks files and compare values

## Synopsis

```**sh
$ test expression
$ test
$ [ expression ]
$ [ ]
```

## Description

`test` takes a given expression and sets the exit code according to its truthiness, 0 if true, 1 if false.
An omitted expression defaults to false, and an unexpected error causes an exit code of 126.

If `test` is invoked as `[`, a trailing `]` is _required_ after the expression.

## Expressions

The expression can take any of the following forms:

### Grouping

* `( <expression> )` value of expression

### Boolean operations

* `! <expression>` negation of expression
* `<expression> -a <expression>` boolean conjunction of the values
* `<expression> -o <expression>` boolean disjunction of the values

### String comparison

* `<string>` whether the string is non-empty
* `-n <string>` whether the string is non-empty
* `-z <string>` whether the string is empty
* `<string> = <string>` whether the two strings are equal
* `<string> != <string>` whether the two strings not equal

### Integer comparison

* `<integer> -eq <integer>` whether the two integers are equal
* `<integer> -ne <integer>` whether the two integers are not equal
* `<integer> -lt <integer>` whether the integer on the left is less than the integer on the right
* `<integer> -gt <integer>` whether the integer on the left is greater than the integer on the right
* `<integer> -le <integer>` whether the integer on the left is less than or equal to the integer on the right
* `<integer> -ge <integer>` whether the integer on the left is greater than or equal to the integer on the right

### File comparison

* `<file> -ef <file>` whether the two files are the same (have the same inode and device numbers)
* `<file> -nt <file>` whether the file on the left is newer than the file on the right (modification date is used)
* `<file> -ot <file>` whether the file on the left is older than the file on the right (modification date is used)

### File type checks

* `-b <file>` whether the file is a block device
* `-c <file>` whether the file is a character device
* `-f <file>` whether the file is a regular file
* `-d <file>` whether the file is a directory
* `-p <file>` whether the file is a pipe
* `-S <file>` whether the file is a socket
* `-h <file>`, `-L <file>` whether the file is a symbolic link

### File permission checks

* `-r <file>` whether the current user has read access to the file
* `-w <file>` whether the current user has write access to the file
* `-x <file>` whether the current user has execute access to the file
* `-e <file>` whether the file exists


Except for `-h/-L`, all file checks dereference symbolic links.

NOTE: Your shell might have a builtin named 'test' and/or '[', please refer to your shell's documentation for further details.


## Options

None.

## Examples

```sh
# Conditionally do something based on the value of a variable
$ /bin/test "$foo" = bar && echo foo is bar
# Check some numbers
$ /bin/test \( 10 -gt 20 \) -o \( ! 10 -ne 10 \) && echo "magic numbers!"
```

## See Also

* [`find`(1)](find.md)
