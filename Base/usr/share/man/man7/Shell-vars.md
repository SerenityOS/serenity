## Name

Shell Variables - Special local and environment variables used by the Shell

## Description

The Shell uses various variables to allow for customisations of certain behavioural or visual things.
Such variables can be changed or set by the user to tweak how the shell presents things.

## Behavioural

1. Output interpretations

`IFS` (local)

The value of this variable is used to join lists or split strings into lists, its default value is a newline (`\\n`).


## Visual

2. Prompting

`PROMPT` (environment)

The value of this variable is used to generate a prompt, the following escape sequences can be used literally inside the value, and they would expand to their respective values:
- `\\u` : the current username
- `\\h` : the current hostname
- `\\w` : a collapsed path (relative to home) to the current directory
- `\\p` : the string '$' (or '#' if the user is 'root')

Any other escaped character shall be ignored.


`PROMPT_EOL_MARK` (environment)

The value of this variable is used to denote the ends of partial lines (lines with no newline), its default value is '%'.

