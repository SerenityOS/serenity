![Welcome Icon](/res/icons/32x32/app-welcome.png)

## Name

Tips and Tricks

## Description

This is a list of useful tips and tricks to help you make the most out of SerenityOS.

## General

-   When on the Desktop or in File Manager, start typing the name of an item to select it.
-   Bold text in context menus hints at the default behavior of a double-click.
-   Hold `Ctrl` to accelerate mouse wheel interaction with sliders and spin boxes.
-   Hold `Ctrl` while activating a menu item to prevent that menu from closing.
-   Many applications can open a compatible file if you drag-and-drop it into their window.
-   Change default file and protocol associations in `~/.config/LaunchServer.ini`.

### Window Management

-   Double-click a window's title bar to maximize it.
-   Click on a window's icon to open the window's context menu.
-   Double-click on the edge of an application's window to maximize it in that direction.
-   Middle-click on a window's maximize button to extend the window vertically (this can be undone in the same way).
-   Drag resizable windows to any side or corner of the screen to automatically resize them to fill half or one-quarter of the screen.
-   Right-click on the Workspace Picker applet and select 'Workspace Settings' to easily customize the number and layout of Workspaces (virtual desktops).

### Fun

-   It can help to get a second pair of `$ Eyes` on a problem… or fifty: `$ Eyes -n 100`.

## Applications

### [Assistant](help://man/1/Applications/Assistant)

-   Assistant can help you to quickly find files and launch applications. Open it with `Super+Space`.
-   Enter a URL to open it in the web browser, e.g. `serenityos.org`.
-   Perform quick calculations by typing the equal sign (=) followed by a mathematical expression, e.g. `=22*101`. Press Return to copy the result.
-   Run commands in the terminal by prefixing them with a dollar sign ($), e.g. `$ uname -a`.

### [Browser](help://man/1/Applications/Browser)

-   Browser has built-in content filtering, which can be used for ad blocking. Update the filters in `~/.config/BrowserContentFiltering.txt`.

### Keyboard Mapper

-   Create and edit custom keymaps with `$ KeyboardMapper`.

### Run

-   The Run dialog accepts all [Shell](help://man/5/Shell) commands.

### [Terminal](help://man/1/Applications/Terminal)

-   Some of the bold or underlined text in Terminal can be double-clicked to open or right-clicked for additional actions.
    -   For example, right-click on a file or folder and select 'Copy URL' to copy the path.
-   Many Serenity applications already have convenient aliases. Use `$ cat /etc/shellrc` to view them.

### [Text Editor](help://man/1/Applications/TextEditor)

-   Text files can be dragged directly from Terminal and dropped on Text Editor to open them.
-   Text Editor has multiple viewing modes. You can edit HTML or Markdown and live preview it at the same time.

## Development

-   Supplying `# profile` with a process identifier (PID) of `-1` as root enables systemwide profiling.
-   Easily transfer files from QEMU to your host machine by using the built-in web server. In the terminal enter `ws .` to start a WebServer instance for the current working directory. Then open `localhost:8000` on your host machine.

## See also

-   [Keyboard Shortcuts](help://man/7/KeyboardShortcuts)
