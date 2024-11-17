## Name

notify - create a notification

## Synopsis

```**sh
$ notify <title> <message> [-I icon-path] [-L launch-url]
```

## Options

-   `-I`, `--icon-path`: Path to icon image file
-   `-L`, `--launch-url`: Notification launch URL

## Arguments

-   `title`: Notification title
-   `message`: Notification message

## Description

`notify` creates a WindowServer notification with the title `title` and message `message`. Optionally, you can also provide an icon path and/or a launch URL
for the notification.

## Examples

```sh
$ <command> && notify "Information" "Command succeeded" -I /res/icons/32x32/msgbox-information.png
```
