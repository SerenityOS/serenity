## Name

shuf - shuffle input lines randomly

## Synopsis

```sh
$ shuf [--head-count count] [--repeat] [--zero-terminated] [file]
```

## Options

-   `-n count`, `--head-count count`: Output at most "count" lines
-   `-r`, `--repeat`: Pick lines at random rather than shuffling. The program will continue indefinitely if no `-n` option is specified
-   `-z`, `--zero-terminated`: Split input on \0, not newline

## Arguments

-   `file`: File
