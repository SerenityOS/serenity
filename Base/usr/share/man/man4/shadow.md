## Name

shadow - Users' password information

## Description

`/etc/shadow` is a text file similar to `/etc/passwd` where each line represents
colon-separated password information of a user on the system.
- The first field is the username.
- The second field is the SHA256-encrypted password. If it starts with a `!`, the account is locked.
- The third field is the date at which the password was last changed, as a timestamp. `useradd` currently hardcodes this to 18727.
The rest of the fields are unused.

## Examples

* `root::18727::::::`: The default fields for root. It doesn't have a password.
* `demostanis:!$5$zFiQBeTD88m/mhbU$ecHDSdRd5yNV45BzIRXwtRpxJtMpVI5twjRRXO8X03Q=:18727::::::`: A locked user with a password.

## Files

* /etc/shadow

## See also

* [`passwd`(1)](help://man/1/passwd)
* [`passwd`(4)](help://man/4/passwd)
