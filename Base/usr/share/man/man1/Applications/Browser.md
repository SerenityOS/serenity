## Name

![Icon](/res/icons/16x16/app-browser.png) Browser - Serenity WWW Browser

[Open](file:///bin/Browser)

## Synopsis

```**sh
$ Browser [options] [urls]
```

## Description

Browser is an application used to view the World Wide Web. It is built on top of Serenity's own `LibWeb` and `LibJS` engines, allowing it to render HTML, CSS, and JavaScript.

## Options

-   `--help`: Display help message and exit
-   `--version`: Display version number and exit

## Arguments

-   `urls`: A list of urls to open, one per tab. If none are specified, then the homepage will be opened instead.

## Examples

```**sh
$ Browser
$ Browser --help
$ Browser https://serenityos.org/
$ Browser https://serenityos.org/ /res/html/misc/welcome.html github.com/serenityos/serenity
```
