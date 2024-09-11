## Name

xargs - build and execute commandlines from input

## Synopsis

```**sh
$ xargs [options...] [command [initial-arguments...]]
```

## Description

`xargs` reads items from a stream, delimited by some blank character (`delimiter`), and executes the `command` as many times as there are items, with any processed `initial-arguments`, possibly followed by a number of items read from the input.

If a `placeholder` is explicitly specified, the `max-lines` limit is set to 1, and each argument in `initial-arguments` is processed by replacing any occurrence of the `placeholder` with the input item, and treating the entire resulting value as _one_ argument.

It is to be noted that `command` is also subject to substitution in this mode.

If no argument in `command` or `initial-arguments` contains the `placeholder`, an argument is added at the end of the list containing only the `placeholder`.

If a `placeholder` is not explicitly specified, no substitution will be performed, rather, the item(s) will be appended to the end of the command line, until either of the following conditions are met:

-   Adding another argument would overflow the system maximum command length (or the provided `max-chars` limit)
-   The number of lines used per command (`max-lines`) would be exceeded

`xargs` will read the items from standard input by default, and when data is read from standard input, the standard input of `command` is redirected to read from `/dev/null`.
The standard input is left as-is if data is read from a file.

## Options

-   `-I`, `--replace`: Set the `placeholder`, and force `max-lines` to 1
-   `-0`, `--null`: Split the items by zero bytes (null characters) instead of `delimiter`
-   `-d`, `--delimiter`: Set the `delimiter`, which is a newline (`\n`) by default
-   `-v`, `--verbose`: Display each expanded command on standard error before executing it
-   `-a`, `--arg-file`: Read the items from the specified file, `-` refers to standard input and is the default
-   `-L`, `--line-limit`: Set `max-lines`, `0` means unlimited (which is the default)
-   `-s`, `--char-limit`: Set `max-chars`, which is `ARG_MAX` (the maximum command size supported by the system) by default

## Examples

```sh
$ pro http://list-of-example-urls.com/plain | xargs -I 'URL' pro URL > concatenated-outputs
$ xargs -a list-of-files-to-delete --verbose rm
$ xargs -a list-of-moves -L 2 mv
$ xargs -a stuff --null -s 1024
```

## See also

-   [`find`(1)](help://man/1/find)
