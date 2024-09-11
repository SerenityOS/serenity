## Name

printf - format and print data

## Synopsis

```**sh
$ printf <format> [arguments...]
```

## Description

`printf` formats _argument_(s) according to _format_ and prints the result to standard output.

_format_ is similar to the C printf format string, with the following differences:

-   The format specifier `b` (`%b`) is not supported.
-   The format specifiers that require a writable pointer (e.g. `n`) are not supported.
-   The format specifier `q` (`%q`) has a different behavior, where it shall print a given string as a quoted string, which is safe to use in shell inputs.
-   Common escape sequences are interpreted, namely the following:

| escape | description            |
| :----: | :--------------------- |
| `\\\\` | literal backslash      |
| `\\"`  | literal double quote   |
| `\\a`  | alert (BEL)            |
| `\\b`  | backspace              |
| `\\c`  | Ends the format string |
| `\\e`  | escape (`\\x1b`)       |
| `\\f`  | form feed              |
| `\\n`  | newline                |
| `\\r`  | carriage return        |
| `\\t`  | tab                    |
| `\\v`  | vertical tab           |

The _format_ string is reapplied until all arguments are consumed, and a missing argument is treated as zero for numeric format specifiers, and an empty string for string format specifiers.

## Examples

```sh
# print 64 as a hexadecimal number, with the starting '0x'
$ printf '%#x' 64

# prints "a0\n", ignoring everything after '\c'
$ printf '%s%d\n\caaaa' a

# prints "123400", as 'x' is an invalid number, and the missing argument for the last '%d' is treated as zero.
$ printf '%d%d%d' 1 2 3 4 x
```

## See also

-   [`echo`(1)](help://man/1/echo)
