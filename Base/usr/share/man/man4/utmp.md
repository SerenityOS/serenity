## Name

utmp - Track logged-in users

## Description

`/var/run/utmp` is a JSON file, managed by [`utmpupdate`(1)](help://man/1/utmpupdate), keeping track
of currently logged-in users. It maps terminal devices (e.g. `/dev/tty1`, `/dev/pts/3`) to an object
containing the following properties:
- `pid`: The process ID on which the user is logged-in, usually the Shell's.
- `uid`: The logged-in user ID.
- `from`: The parent application on which the user is logged-in, usually "Terminal".
- `logged_at`: The date represented as a UNIX timestamp.

## Files

* /var/run/utmp

## See also

* [`utmpupdate`(1)](help://man/1/utmpupdate)

