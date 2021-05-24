## Name

Shell Variables - Special local and environment variables used by the Shell

## Description

The Shell uses various variables to allow for customizations of certain behavioral or visual things.
Such variables can be changed or set by the user to tweak how the shell presents things.

## Behavioral

1. Output interpretations

`IFS` (local)

The value of this variable is used to join lists or split strings into lists, its default value is a newline (`\\n`).

2. History

`HISTCONTROL` (environment)

The value of this variable is used to determine which entries are kept in the Shell's history, both regarding the current active session and when writing the history to disk on exit.

- `ignorespace`: Entries starting with one or more space characters are ignored
- `ignoredups`: Consecutive duplicate entries are ignored
- `ignoreboth`: The behavior of `ignorespace` and `ignoredups` is combined
- If the variable is unset (this is the default) or has any other value than the above, no entries will be excluded from history.

Note: This variable is respected by every program using `Line::Editor`, e.g. [`js`(1)](../man1/js.md).

`HISTFILE`  (environment)

The value of this variable is used as the Shell's history file path, both for reading history at startup and writing history on exit.
Its default value is `~/.history`.

`HISTORY_AUTOSAVE_TIME_MS` (environment)

Setting this variable to a value `t` other than zero (0) will make the shell save the history to the history file every `t` milliseconds.
If `t` is not a non-negative integer, zero will be assumed.
Note that multiple shell instances will not interfere with each other if they are to save to the same history file, instead, the entries will all be merged together chronologically.
The default value for this option is set in `/etc/shellrc`.

3. Terminal settings

`PROGRAMS_ALLOWED_TO_MODIFY_DEFAULT_TERMIOS` (local)

The list value stored in this variable is used as an allow-list to filter programs that may modify the current terminal settings (i.e. default termios configuration) of the current shell session.
By default, this list includes `stty`, making it possible for the user to modify the terminal settings from within the shell.
Note that the shell will revert any termios changes if the running program is not in this list, or if it failed to run successfully (exited with an exit code other than zero).
Also note that the line editor will re-evaluate the keybindings and sync them when such a change occurs.

## Visual

1. Prompting

`PROMPT` (environment)

The value of this variable is used to generate a prompt, the following escape sequences can be used literally inside the value, and they would expand to their respective values:
- `\\a`: bell character (behavior depends on terminal)
- `\\e`: escape character (`0x1b`)
- `\\h`: the current hostname
- `\\p`: the string '$' (or '#' if the user is 'root')
- `\\u`: the current username
- `\\w`: a collapsed path (relative to home) to the current directory
- `\\X`: reset style (foreground and background color, etc)

Any other escaped character shall be ignored.

`PROMPT_EOL_MARK` (environment)

The value of this variable is used to denote the ends of partial lines (lines with no newline), its default value is '%'.
