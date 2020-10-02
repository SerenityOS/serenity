## Name

`Shell` - Shell, the command language interpreter

## Synopsis

```**sh
$ Shell [--skip-shellrc] [--live-formatting]
$ Shell [--skip-shellrc] command_file [arguments...]
$ Shell [--skip-shellrc] -c command_string [arguments...]
$ Shell [--skip-shellrc] --format command_file
```

## Description

The `Shell` utility is a command language interpreter, which reads commands from either a command string, a specified file, or the standard input.
The command language shall be described in [`Shell`(5)](../man5/Shell.md), _The Shell Command Language_.

Any extra arguments passed into `arguments` are placed in the local variable `$ARGV` and can also be accessed through the special variable `$*`.

**NOTE**:

The `Shell` utility does not promise POSIX `sh` interoperability.

## Options

* `-c`, `--command-string`: Executes the given string as a command and exits
* `--skip-shellrc`: Skips running the initialisation file (at `~/.shellrc`)
* `--format`: Format shell code from the given file and print the result to standard output
* `-f`, `--live-formatting`: Enable live formatting of the line editor buffer (in REPL mode)

## Examples

```sh
# Start an interactive REPL, ignoring the shellrc
Shell --skip-shellrc

# Execute a given string
Shell -c 'rm foo*'

# Execute the contents of a file with some arguments
Shell foo a b c
```

## See also

* [`Shell-vars`(7)](../man7/Shell-vars.md) For details on local and environment variables used by the shell
