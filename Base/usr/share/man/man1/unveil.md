## Name

unveil - unveil certain paths when running a command

## Synopsis

```**sh
$ unveil [--path] [command...]
```

## Description

Run a command under certain path restrictions by using [`unveil`(2)](help://man/2/unveil).

## Options

-   `-u`, `--path`: Unveil a path, with the format of `permissions,path`

## Examples

Run `ls -la /sys/kernel` with restricted access to certain paths:

```sh
$ unveil --path=r,/etc/timezone --path=r,/usr/lib --path=r,/sys/ --path=r,/etc/passwd --path=r,/etc/group ls -la /sys/kernel
```

Run `ps -ef` with restricted access to certain paths:

```sh
$ unveil --path=r,/etc/timezone --path=r,/usr/lib --path=r,/sys/ --path=r,/etc/passwd --path=r,/etc/group ps -ef
```

## See also

-   [`pledge`(2)](help://man/2/pledge)
-   [`unveil`(2)](help://man/2/unveil)
-   [`Mitigations`(7)](help://man/7/Mitigations)
