## Name

echo - print the given text

## Synopsis

```**sh
$ echo [-ne] [text...]
```

## Description

Print the given `text` to the standard output. If multiple `text`s are provided, they will be joined with a space character. If no `text` is provided, an empty line will be printed.

Character escape sequences and their meanings are as follows:

`\\a` - `<alert>`

`\\b` - `<backspace>`

`\\c` - Suppress the output of all remaining characters, including the trailing newline.

`\\e` - The escape character (`\\033`).

`\\f` - `<form-feed>`

`\\n` - `<newline>`

`\\r` - `<carriage-return>`

`\\t` - `<tab>`

`\\v` - `<vertical-tab>`

`\\\\` - The backslash character (`\\`).

`\\0ooo` - A byte whose value is a zero, one, two, or three-digit octal number.

`\\xHH` - A byte whose value is a two-digit hexadecimal number.

`\\uHHHH` - An unicode code point whose value is a four-digit hexadecimal number.

## Options

-   `-n`: Do not output a trailing newline
-   `-e`: Interpret backslash escapes

## Examples

```sh
$ echo hello friends!
hello friends!
$ echo -ne '\x68\x65\x6c\x6c\x6f' 'friends\041\n'
hello friends!
```
