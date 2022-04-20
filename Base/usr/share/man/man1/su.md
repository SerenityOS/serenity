## Name

su - switch to another user

## Synopsis

```sh
$ su <user>
```

## Description

`su`: Switch to another user.

When called with no user-specified, `su` defaults to switch to the *root* user. Need to enter the password if the user switch to has one.

## Arguments

* `user`: User to switch to (defaults to the user with UID 0)

## Examples

Switch to root user

```sh
$ su
```

Switch to another user

```sh
$ su nona
```
