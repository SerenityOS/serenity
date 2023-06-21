## Name

groups - list group memberships

## Synopsis

```**sh
$ groups [username...]
```

## Description

`groups` lists group memberships.

If no username is provided group memberships are listed for current user.

## Arguments

* `username`: username to list group memberships for

## Examples

```sh
# List group memberships for current user
$ groups
# List group memberships for one user
$ groups nona
# List group memberships for multiple users
$ groups nona anon root
```

