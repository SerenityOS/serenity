## Name

![Icon](/res/icons/16x16/app-text-editor.png) TextEditor - SerenityOS text editor

[Open](file:///bin/TextEditor)

## Synopsis

```**sh
$ TextEditor [--preview-mode mode] [file[:line[:column]]]
```

## Description

TextEditor is a text document editor for SerenityOS featuring a preview mode
which allows automatic live rendering of HTML and Markdown documents.

## Options

-   `--preview-mode mode`: Preview mode, one of 'none', 'html', 'markdown', 'auto'

## Arguments

-   `file[:line[:column]]`: File to edit, with optional starting line and column number

## Examples

```sh
$ TextEditor /home/anon/Documents/emoji.txt
$ TextEditor /home/anon/Documents/emoji.txt:5:12
```
