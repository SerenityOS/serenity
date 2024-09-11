## Name

![Icon](/res/icons/16x16/app-mail.png) Mail - Serenity e-mail client

[Open](file:///bin/Mail)

## Synopsis

```**sh
$ Mail
```

## Description

Mail is an e-mail client for Serenity. It can connect to real e-mail servers.
Currently, a configuration file is required. This must be stored in `~/.config/Mail.ini`
See the Examples section for an example configuration file.

## Examples

`~/.config/Mail.ini`:

```ini
[Connection]
Server=email.example.com
Port=993
TLS=true

[User]
Username=test@example.com
Password=Example1
```

```sh
$ Mail
```
