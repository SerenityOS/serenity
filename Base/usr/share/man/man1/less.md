## Name

`less` - A full-featured terminal pager.

## Synopsis

```**sh
$ cat file | less [-PXNem]
$ less [-PXNem] [file]
$ cat file | more
$ more [file]
```

## Description

`less` is a terminal pager that allows backwards movement. It is inspired by,
but largely incompatible with
[GNU less](https://www.greenwoodsoftware.com/less/index.html).

## Options

-   `-P`, `--prompt`: Set the prompt format string. See [Prompts](#prompts) for more details.
-   `-X`, `--no-init`: Don't switch to the xterm alternate buffer on startup.
-   `-N`, `--line-numbers`: Show line numbers before printed lines.
-   `-e`, `--quit-at-eof`: Immediately exit less when the last line of the document is reached.
-   `-F`, `--quit-if-one-screen`: Exit immediately if the entire file can be displayed on one screen.
-   `-m`, `--emulate-more`: Apply `-Xe`, set the prompt to `--More--`, and disable
    scrollback. This option is automatically applied when `less` is executed as `more`

## Commands

Commands may be preceded by a decimal number `N`. Currently, this feature
does not exist, and no command use `N`.

| Command                       | Description              |
| ----------------------------- | ------------------------ |
| `q`                           | Exit less.               |
| `j` or `DOWNARROW` or `ENTER` | Go to the next line.     |
| `k` or `UPARROW`              | Go to the previous line. |
| `f` or `SPACE`                | Go to the next page.     |
| `b`                           | Go to the previous page. |

## Prompts

`less` accepts a special prompt string with the `-P` option. prompts accept a
variety of format specifiers so that they adapt to `less`'s state.

A `%` followed by a letter will be replaced with the associated variable. If
such a variable does not exist, or currently has no value, it will be replaced
with a `?`.

| Variable | Description                                |
| -------- | ------------------------------------------ |
| `%f`     | Replaced with the name of the current file |
| `%l`     | Replaced with the current line number      |

A `?` followed by a letter acts as an if expression on the associated
condition. `:` then acts as the else, and `.` acts as the end token. for example
if you wanted to print 'true' if you are at the end of the file and 'false'
otherwise, your prompt would be `?etrue:false.`. These expressions are
arbitrarily nestable. If a condition does not exist, then it will always
evaluate it's false branch.

| Condition | Description                                     |
| --------- | ----------------------------------------------- |
| `?f`      | True when reading from a file other than stdin. |
| `?e`      | True when at the end of a file.                 |

A `\\` followed by any character will be replaced with the literal value of the
character. For instance, `\\%l` would render as `%l`.

All other characters are treated normally.

#### Examples

`less`'s current default prompt: `'?f%f :.(line %l)?e (END):.'`

## See Also

-   [`more`(1)](help://man/1/more) For a simpler pager that less implements.
-   [`man`(1)](help://man/1/man) For serenity's manual pager, that uses less.
