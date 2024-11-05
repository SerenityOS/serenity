## Name

readelf

## Synopsis

```sh
$ readelf [--all] [--file-header] [--program-headers] [--section-headers] [--headers] [--syms] [--dyn-syms] [--dynamic] [--notes] [--relocs] [--unwind] [--checksec] [--string-dump section-name] <path>
```

## Options

-   `-a`, `--all`: Display all
-   `-h`, `--file-header`: Display ELF header
-   `-l`, `--program-headers`: Display program headers
-   `-S`, `--section-headers`: Display section headers
-   `-e`, `--headers`: Equivalent to: -h -l -S -s -r -d -n -u -c
-   `-s`, `--syms`: Display the symbol table
-   `--dyn-syms`: Display the dynamic symbol table
-   `-d`, `--dynamic`: Display the dynamic section
-   `-n`, `--notes`: Display core notes
-   `-r`, `--relocs`: Display relocations
-   `-u`, `--unwind`: Display unwind info
-   `-c`, `--checksec`: Display security hardening info
-   `-p section-name`, `--string-dump section-name`: Display the contents of a section as strings

## Arguments

-   `path`: ELF path

<!-- Auto-generated through ArgsParser -->
