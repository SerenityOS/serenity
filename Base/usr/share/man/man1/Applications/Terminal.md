## Name

![Icon](/res/icons/16x16/app-terminal.png) Terminal - Serenity terminal emulator

[Open](file:///bin/Terminal)

## Synopsis

```**sh
$ Terminal [options]
```

## Description

Terminal is a terminal emulator application for Serenity.

It will generally be launched from Serenity menu, an on-screen menu, or the `Open in Terminal` action in File Manager and on the Desktop. You can also launch the current help item from within the help document, click on the `Open` link above to launch Terminal.

Selecting `File->Terminal Settings` will launch Terminal Settings dialog and display user configurable application properties. This dialog box contains two tabs namely Terminal tab and View tab.

The _Settings Terminal_ tab shows the option to either enable System beep, or use Visual bell or disable bell altogether. It can also enable or disable the display of terminal scrollbar and to change its exit behavior.

The _Settings View_ tab shows the option of specifying background opacity. Opacity is the amount in which the Terminal's background is transparent, displaying what's underneath. Set the opacity level to your taste while maintaining readability balance.

You also have the option of using Terminal's system default font or to select a new one by clicking on the ellipsis. Remember to uncheck `Use system default` checkbox before doing so.

You can change the shape of the cursor from block type, to underline or to bar type. You can also opt to enable or disable cursor's blink property.

You can select to change the Terminal's color scheme. Just click on the drop-down list and pick a color scheme. Examples of color schemes are Dracula, Monokai, Solarized and Zenburn.

Clicking on the _Apply_ button will cause the currently selected options to take effect immediately.

You can press the shortcut key F11 to display Terminal in fullscreen mode. Press F11 again to revert back to normal window display mode.

## Options

* `--help`: Display help message and exit
* `--version`: Print version
* `-e`: Execute this command inside the terminal
* `-k`: Keep the terminal open after the command has finished executing

## Examples

```sh
$ Terminal -e Shell
$ Terminal -k -e Browser
```
