## Name

notify - create a notification

## Synopsis

```**sh
$ notify <title> <message> [icon-path]
```

## Arguments

* `title`: The title of notification
* `message`: The message of notification
* `icon-path`: Path to icon image file

## Description

`notify` creates a WindowServer notification with title `title` and message `message`. You can also provide an icon path; by default, no icon will be used.

## Examples

```sh
$ <command> && notify "Information" "Command succeeded" /res/icons/32x32/msgbox-information.png
```
