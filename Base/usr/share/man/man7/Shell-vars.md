## Name

Shell Variables - Special local and environment variables used by the Shell

## Description

The Shell uses various variables to allow for customisations of certain behavioral or visual things.
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
