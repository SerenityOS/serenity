## Name

passwd - Users on the system

## Description

`/etc/passwd` is a text file where each line represents a user on the system, with
colon-separated fields such as the home directory or the default shell:
- The first field is the username.
- The second field is the encrypted password. It is always a `!`, to indicate that the real password is in `/etc/shadow`.
- The third field is the user ID.
- The fourth field is the main group ID.
- The fifth field is the GECOS username: a longer string to indicate general information about the user.
- The sixth field is the home directory.
- The last field is the default shell.

## Examples

* `root:!:0:0:root:/root:/bin/Shell`: The default fields for the root user. Its home directory is `/root`, and its UID and GID are both 0.
* `window:!:13:13:WindowServer,,,:/:/bin/false`: A system account for the WindowServer. Unlike normal users, it doesn't have a shell.

## Files

* /etc/passwd

## See also

* [`passwd`(1)](help://man/1/passwd)
* [`shadow`(4)](help://man/4/shadow)
