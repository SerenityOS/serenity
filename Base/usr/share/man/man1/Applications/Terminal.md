## Name

![Icon](/res/icons/16x16/app-terminal.png) Terminal - Serenity terminal emulator

[Open](file:///bin/Terminal)

## Synopsis

```**sh
$ Terminal [options]
```

## Description

Terminal is a terminal emulator application for Serenity.

It can be launched from the System Menu or the quick access icon to its right, via the `Open in Terminal` action in File Manager and on the Desktop. You can also click on the `Open` link above to launch Terminal.

Select `File → Terminal Settings` to launch the Terminal Settings dialog and display user configurable application properties. This dialog box contains two tabs: View and Terminal.

The _View_ tab provides the most frequently sought options:

-   Adjust the Terminal font (turn off `Use system default` to select a custom font.
-   Specify background opacity, i.e. the amount to which the Terminal's background is transparent, displaying what's underneath.
-   Change the shape of the cursor from Block, to Underscore or to Vertical bar. You can also opt to enable or disable cursor's blink property.
-   To enable or disable the display of terminal scrollbar.

The _Terminal_ tab gives less frequently used options:

-   To either enable System beep, or use Visual bell or disable bell mode altogether.
-   To change Terminal's exit behavior

Clicking on the _Apply_ button will cause the currently selected options to take effect immediately.

You can toggle Fullscreen mode by pressing F11.

## Options

-   `--help`: Display help message and exit
-   `--version`: Print version
-   `-e`: Execute this command inside the terminal
-   `-k`: Keep the terminal open after the command has finished executing

## Examples

```sh
$ Terminal -e Shell
$ Terminal -k -e Browser
```
