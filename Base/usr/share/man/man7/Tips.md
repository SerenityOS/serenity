# Tips and Tricks

This is a list of useful tips and tricks to help you make the most out of SerenityOS

Related: [Keyboard Shortcuts](help://man/7/KeyboardShortcuts)

## General
* Bold text in context menus hints at the default behavior of a double-click
* Holding `Ctrl` accelerates mouse wheel interaction with sliders and spin boxes
* Holding `Ctrl` while activating a menu item prevents that menu from closing
* Pressing `Ctrl+Shift+A` on a focused widget or application activates the command palette, a searchable list of available actions
* Default file and protocol associations can be changed in `~/.config/LaunchServer.ini`
* Applications can be viewed with Inspector by including `'MAKE_INSPECTABLE=1'` in their environment

### Window Management
* Double clicking a window's title bar maximizes it; double clicking its icon will close it
* If you double-click on the edge of an application window, it will maximize in that direction
* Middle clicking a window's maximize button extends that window vertically
* Resizable windows can be snapped to all sides of the screen. Drag a window to an edge or press `Super+Left, Right or Up` while it has focus
* Focus can be cycled between windows by pressing and holding `Super+Tab`. `Shift` reverses the order
* Windows can be dragged from any visible point by holding `Super+Left-click`. `Super+Right-click` begins resizing them
* Workspaces can be switched by pressing `Ctrl+Alt+Arrows`. `Shift` brings the active window along

### Fun
* It can help to get a second pair of `$ Eyes` on a problem. Or fifty: `$ Eyes -n 100`

## Applications

### Assistant
* Assistant can help you quickly find files and applications by pressing `Super+Space`

### Browser
* Browser has built-in ad blocking. Filter content by adding new domains to `~/.config/BrowserContentFiltering.txt`

### Keymapper
* Custom keymaps can be created and edited with `$ KeyboardMapper`

### Run
* The Run dialog accepts all Shell command language

### Terminal
* Highlighted text in Terminal can be launched or right-clicked for more context
* Many Serenity applications already have convenient aliases. `$ cat /etc/shellrc` to view them
* Supplying `# profile` with a PID of `-1` as root enables systemwide profiling

### Text Editor
* Text files can be dragged directly from Terminal and dropped on Text Editor to open them
* Text Editor has multiple viewing modes; edit and preview HTML and Markdown in real time