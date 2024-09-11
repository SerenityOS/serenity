## Name

usermod - modify a user account

## Synopsis

```sh
$ usermod [--append] [--uid uid] [--gid group] [--groups groups] [--lock] [--remove] [--unlock] [--home new-home] [--move] [--shell path-to-shell] [--gecos general-info] <username>
```

## Description

This program modifies an existing user account.
This program must be run as root.

## Options

-   `--help`: Display help message and exit
-   `--version`: Print version
-   `-a`, `--append`: Append the supplementary groups specified with the -G option to the user
-   `-u uid`, `--uid uid`: The new numerical value of the user's ID
-   `-g group`, `--gid group`: The group name or number of the user's new initial login group
-   `-G groups`, `--groups groups`: Set the user's supplementary groups. Groups are specified with a comma-separated list. Group names or numbers may be used
-   `-L`, `--lock`: Lock password
-   `-r`, `--remove`: Remove the supplementary groups specified with the -G option from the user
-   `-U`, `--unlock`: Unlock password
-   `-d new-home`, `--home new-home`: The user's new login directory
-   `-m`, `--move`: Move the content of the user's home directory to the new location
-   `-s path-to-shell`, `--shell path-to-shell`: The name of the user's new login shell
-   `-n general-info`, `--gecos general-info`: Change the GECOS field of the user

## Arguments

-   `username`: Username of the account to modify

## See also

-   [`userdel`(8)](help://man/8/userdel)
-   [`useradd`(8)](help://man/8/useradd)
