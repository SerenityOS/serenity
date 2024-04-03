## Name

losetup - manage loop devices

## Synopsis

```**sh
$ losetup [options...] [path]
```

## Description

losetup is a utility for managing loop devices. It can create new devices or delete
existing ones.

## Options

* `-d`, `--delete`: Remove an existing loop device based on the `path` argument.
* `-c`, `--create`: Create a new loop device based on the `path` argument.

## Files

* `/dev/devctl` - device control device inode, which allows creating and deleting loop devices.

## Examples

```sh
$ losetup -c test.img
Created new device at /dev/loop/0
$ losetup -d /dev/loop/0
```
