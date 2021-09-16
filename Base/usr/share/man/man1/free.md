## Name

free - display the used and available memory on the system

## Synopsis

```**sh
$ free [-m]
```

## Description

This program displays information about the used and available memory on the system. When invoked, it will print the statistics in bytes by default.

## Options

-   `-m`, `--megabytes`: Display values in megabytes

## Examples

```sh
# Display the available memory
$ free
               total        used   available
Mem:       491301728    64666464   426635264

# Display the available memory in megabytes
$ free -m
               total        used   available
Mem:             468          61         406
```
