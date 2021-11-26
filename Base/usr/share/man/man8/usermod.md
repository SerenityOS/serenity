## Name

usermod

## Synopsis

```sh
$ usermod [--uid uid] [--gid gid] [--lock] [--unlock] [--home new-home] [--move] [--shell path-to-shell] [--gecos general-info] <username>
```

## Description

Modify a user account

## Options:

* `--help`: Display help message and exit
* `--version`: Print version
* `-u uid`, `--uid uid`: The new numerical value of the user's ID
* `-g gid`, `--gid gid`: The group number of the user's new initial login group
* `-L`, `--lock`: Lock password
* `-U`, `--unlock`: Unlock password
* `-d new-home`, `--home new-home`: The user's new login directory
* `-m`, `--move`: Move the content of the user's home directory to the new location
* `-s path-to-shell`, `--shell path-to-shell`: The name of the user's new login shell
* `-n general-info`, `--gecos general-info`: Change the GECOS field of the user

## Arguments:

* `username`: Username of the account to modify

<!-- Auto-generated through ArgsParser -->
