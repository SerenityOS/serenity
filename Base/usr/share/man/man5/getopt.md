## Name

getopt - command-line options

## Synopsis

```**sh
$ command -o --long-option
```

## Description

Most programs accept various *options* that configure their behavior to be
passed alongside other command-line arguments. Each program accepts its own set
of options, though many programs share options in common.

Options come in two kinds, **short** and **long**. Short options have a single
letter as their name, are preceded by a single dash, and can be grouped together
in one argument. Long options have whole strings as their names, are preceded by
a double dash, and cannot be grouped.

Each option can require (or optionally accept) a **value** (also often
confusingly called an *argument*). Generally, a value for an option, if any,
should be written after the option itself, although the exact syntax for values
of short and long options differs. In both cases, the value can be specified as
the next command-line argument after the option. For short options, the value
can also immediately follow the option as a part of the same command-line
argument. For long options, the value can follow the option as a part of the
same command-line argument, separated form it by the `=` character.

If several short options are combined into one command line argument, only the
one specified last can be provided with a value. All the characters following
the first short option to accept (optionally or not) a value are treated as a
value for that option, and not as further options.

Options can be freely mixed with non-option command-line arguments (with the
exception of the very first argument to be specified, which must be the command
itself). A special command-line argument value `--` can be specified to indicate
that all further command-line arguments are to be treated like non-option
arguments, even if they otherwise look like options. The `--` argument itself is
not considered to be either an option or a non-option argument, and is otherwise
ignored.

A special argument `-` (a single dash) is always treated as a non-option
argument.

## Examples

Short and long options, without values or non-option arguments:

```sh
$ command -o
$ command -vf -l
$ command --long-option
$ command --verbose --force --long
```

Short and long options with values:

```sh
$ command -o rw
$ command --type text/plain
```

Alternative syntaxes for values:

```sh
$ command -fttext/plain
$ command --force --type=text/plain
```

These two invocation are equivalent, provided the `-f` option has same effect as
`--force`, and the `-t` option has the same effect as `--type`.

Mixing options and non-option arguments:

```sh
$ command --force argument
$ command argument -o value another-argument
```

Using `--` to prevent arguments from being accidentally misinterpreted as
options:

```sh
$ command --force -- -argument --another-argument
```

## See also

* [`getopt`(3)](../man3/getopt.md)
