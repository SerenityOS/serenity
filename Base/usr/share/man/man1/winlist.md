## Name

winlist - List windows

## Synopsis

```**sh
$ winlist [-k] [-f|[-i] [-t]]
```

## Description

winlist asks the current window list from WindowServer
and prints them according to the specified format.

## Options

* `-f fmt`, `--format fmt`: Output format (see [OUTPUT FORMAT](#output-format)), defaults to `%i %t`
* `-t`, `--titles`: Print window titles, equivalent to `-f %t`
* `-i`, `--ids`: Print window ids, equivalent to `-f %i`
* `-k`, `--keep-alive`: Don't close connection to WindowServer; listen for new windows
* `--help`: Display help message and exit
* `--version`: Print version

If `-t` and `-i` are used altogether, the output format is `%i %t`.

## Output format

One can specify what winlist will output.
Any combinaison of a `%` symbol followed by one of `t`, `i`, `%` will be replaced by:
* `t`: The window title
* `i`: The window ID
* `%`: A percentage symbol

## Examples

Print window IDs:
```sh
$ winlist -i
```

Print window titles:
```sh
$ winlist -t
```

Print a custom format and wait for new windows:
```sh
$ winlist -f 'title="%t" id=%i' -k
```

