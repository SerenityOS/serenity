## Name

syscall - test a system call

## Synopsis

```**sh
$ syscall [-o] [-l] [-h] <syscall-name> <args...> [buf==BUFSIZ buffer]`
```

## Description

The `syscall` utility can be used to invoke a system call with the given arguments.

## Options

* `-o`: Output the contents of the buffer argument specified as buf to stdout.
* `-l`: Print a space separated list of all the Serenity system calls and exit. Note that not all the system calls can be invoked using this tool.
* `-h`: Print a help message and exit.

## Examples

Write a string to standard output:

```sh
$ syscall write 1 hello 5
```

Read a string from the standard input into a buffer and output the buffer contents to stdout:

```sh
$ syscall -o read 0 buf 3
```

Get the pid of the current running process:

```sh
$ syscall getpid
```

Sleep for 3 seconds:

```sh
$ syscall sleep 3
```

Create a directory:

```sh
$ syscall mkdir my-dir 0755
```

Exit the program with status 2:

```sh
$ syscall exit 2
```

## History

This is a direct port of a utility with the same name originated from the Plan 9 operating system. 

