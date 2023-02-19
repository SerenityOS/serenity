![Welcome Icon](/res/icons/32x32/app-welcome.png)

## Name
Tips and Tricks

## Description
This is a list of useful tips and tricks to help you make the most out of SerenityOS.

## General
* When on the Desktop or in File Manager, start typing and the nearest match will be selected.
* Bold text in context menus hints at the default behavior of a double-click.
* Hold `Ctrl` to accelerate mouse wheel interaction with sliders and spin boxes.
* Hold `Ctrl` while activating a menu item to prevent that menu from closing.
* Many applications will open a compatible file if you drag-and-drop it into their window.
* Default file and protocol associations can be changed in `~/.config/LaunchServer.ini`.

### Window Management
* Double-click a window's title bar to maximize it.
* Click on a window's icon to open the context menu.
* Double-click on the edge of an application's window to maximize it in that direction.
* Middle-click on a window's maximize button to extend the window vertically (this can be undone in the same way).
* Resizable windows can be dragged to all sides or corners of the screen, where they will automatically resize to fill half or one quarter of the screen.
* Right-click on the Workspace Picker applet and select 'Workspace Settings' to easily customize the number and layout of Workspaces (virtual desktops).

### Fun
* It can help to get a second pair of `$ Eyes` on a problem. Or fifty: `$ Eyes -n 100`.

## Applications

### [Assistant](help://man/1/Applications/Assistant)
* Assistant can help you quickly find files and applications by pressing `Super+Space`.
* You can enter a URL for opening in the Browser.

### [Browser](help://man/1/Applications/Browser)
* Browser has built-in ad blocking. Filter content by adding new domains to `~/.config/BrowserContentFiltering.txt`.

### Keyboard Mapper
* Custom keymaps can be created and edited with `$ KeyboardMapper`.

### Run
* The Run dialog accepts all [Shell](help://man/5/Shell) commands.

### [Terminal](help://man/1/Applications/Terminal)
* Bold or underlined text in Terminal can be double-clicked to open or right-clicked for additional actions.
* Many Serenity applications already have convenient aliases. Use `$ cat /etc/shellrc` to view them.

### [Text Editor](help://man/1/Applications/TextEditor)
* Text files can be dragged directly from Terminal and dropped on Text Editor to open them.
* Text Editor has multiple viewing modes. You can edit HTML or Markdown and live preview at the same time.

## Development
* Supplying `# profile` with a PID of `-1` as root enables systemwide profiling.
* Applications can be viewed with [Inspector](help://man/1/Applications/Inspector), which facilitates process inspection via Remote Procedure Call (RPC), by including `MAKE_INSPECTABLE=1` in their environment. For example, to make the Eyes application inspectable you would enter the following Shell commands:
```sh
$ export MAKE_INSPECTABLE=1
$ Eyes &
```

## See also
* [Keyboard Shortcuts](help://man/7/KeyboardShortcuts)