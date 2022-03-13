## Name

su - run a command with substitute user

## Synopsis

```
$ su [user]
```

## Description

**su** allows commands to be run with a substitute user.

When called with no user-specified, **su** defaults to running an interactive shell as *root*. 

## Options

- **--help**

  Display help message and exit.

- **--version**

  Display version information and exit.

## Arguments

- user: User to switch to (defaults to user with UID 0)

## Examples

```
# Switch to root user
$ su

#Switch to another user
$ su nona

```

