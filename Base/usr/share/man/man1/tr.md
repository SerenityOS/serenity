## Name

tr - Replace characters

## Synopsis

```**sh
$ tr [-d chars]|chars replacement [options]... [files]...
```

## Options

* `-c`, `--complement`: Take the complement of the first set
* `-d`, `--delete`: Delete characters instead of replacing
* `-s`, `--squeeze-repeats`: Omit repeated characters listed in the last given set from the output

## Arguments

* `from`: Set of characters to translate from
* `to`: Set of characters to translate to

## Examples

Remove "a"s from standard input:
```sh
$ echo "Weaaaaaaaaaaaaall Hello Friends" | tr -d a
Well Hello Friends
```

Remove newlines from a file:
```sh
$ tr -d \\n files_with_many_lines_but_i_only_wanted_one.txt
```

Replace all "e"s and "i"s with "o"s from "SerenityOS":
```sh
$ echo SerenityOS | tr ai o
SoronotyOS
```

Replace all letters which are not in the given set:
(here, we replace the "m" and the "t")
```sh
$ echo -n demostanis | tr -c eoasdni w
dewoswanis
```

