## Name

grep

## Synopsis

```sh
$ grep [--recursive] [--extended-regexp] [--fixed-strings] [--regexp Pattern] [--file File] [-i] [--line-numbers] [--invert-match] [--quiet] [--no-messages] [--binary-mode ] [--text] [-I] [--color WHEN] [--no-hyperlinks] [--count] [file...]
```

## Options

-   `-r`, `--recursive`: Recursively scan files
-   `-E`, `--extended-regexp`: Extended regular expressions
-   `-F`, `--fixed-strings`: Treat pattern as a string, not a regexp
-   `-e Pattern`, `--regexp Pattern`: Pattern
-   `-f File`, `--file File`: Read patterns from a file
-   `-i`: Make matches case-insensitive
-   `-n`, `--line-numbers`: Output line-numbers
-   `-v`, `--invert-match`: Select non-matching lines
-   `-q`, `--quiet`: Do not write anything to standard output
-   `-s`, `--no-messages`: Suppress error messages for nonexistent or unreadable files
-   `--binary-mode`: Action to take for binary files ([binary], text, skip)
-   `-a`, `--text`: Treat binary files as text (same as --binary-mode text)
-   `-I`: Ignore binary files (same as --binary-mode skip)
-   `--color WHEN`: When to use colored output for the matching text ([auto], never, always)
-   `--no-hyperlinks`: Disable hyperlinks
-   `-c`, `--count`: Output line count instead of line contents

## Arguments

-   `file`: File(s) to process

<!-- Auto-generated through ArgsParser -->
