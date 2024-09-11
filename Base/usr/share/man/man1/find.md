## Name

find - recursively search for files

## Synopsis

```**sh
$ find [-L] [root-paths...] [commands...]
```

## Description

`find` recursively traverses the file hierarchy starting at the given root paths
(or at the current working directory if no root paths have been specified), and
evaluates the given commands for each found file. The commands can be used to
both filter the set of files and to perform actions on them.

If no _action command_ (`-print`, `-print0`, or `-exec`) is found among the
specified commands, a `-print` command is implicitly appended.

## Options

-   `-L`: Follow symlinks

## Commands

-   `-maxdepth n`: Do not descend more than `n` levels below each path given on
    the command line. Specifying `-maxdepth 0` has the effect of only evaluating
    each command line argument.
-   `-mindepth n`: Descend `n` levels below each path given on the command line
    before executing any commands. Specifying `-mindepth 1` has the effect of
    processing all files except the command line arguments.
-   `-type t`: Checks if the file is of the specified type, which must be one of
    `b` (for block device), `c` (character device), `d` (directory), `l` (symbolic
    link), `p` (FIFO), `f` (regular file), and `s` (socket).
-   `-links [-|+]number`: Checks if the file has the given number of hard links.
-   `-user name`: Checks if the file is owned by the given user. Instead of a user
    name, a numerical UID may be specified.
-   `-group name`: Checks if the file is owned by the given group. Instead of a
    group name, a numerical GID may be specified.
-   `-size [-|+]number[bcwkMG]`: Checks if the file uses the specified `n` units of
    space rounded up to the nearest whole unit.

    The '+' and '-' prefixes denote greater than and less than, i.e an exact size
    of `n` units doesn't match. Sizes are always rounded up to the nearest unit,
    empty files, while the latter will match files from 0 to 1,048,575 bytes.

    The unit of space may be specified by any of these suffixes:

    -   `b`: 512-byte blocks. This is the default unit if no suffix is used.
    -   `c`: bytes
    -   `w`: two-byte words
    -   `k`: kibibytes (1024 bytes)
    -   `M`: mebibytes (1024 kibibytes)
    -   `G`: gibibytes (1024 mebibytes)

-   `-path pattern`: Checks if the full file path matches the given global-style
    pattern. This check matches against the full file name, starting from one of
    the start points given on the command line. This means that using an absolute
    path only makes sense in the case where the start point given on the command
    line is an absolute path. For example, the following command will never match
    anything:

    `find bar -ipath '/foo/bar/test_file' -print`

    The given path is compared against the current directory concatenated with the
    basename of the current file. Because such a concatenation can never end in a
    '/', specifying path argument that ends with a '/' will never match anything.

-   `-ipath pattern`: Functions identically to `-path` but is case-insensitive.
-   `-name pattern`: Checks if the file name matches the given global-style
    pattern (case sensitive).
-   `-empty`: File is either an empty regular file or a directory containing no
    files.
-   `-iname pattern`: Checks if the file name matches the given global-style
    pattern (case insensitive).
-   `-readable`: Checks if the file is readable by the current user.
-   `-writable`: Checks if the file is writable by the current user.
-   `-executable`: Checks if the file is executable, or directory is searchable,
    by the current user.
-   `-newer file`: Checks if the file last modification time is greater than that
    of the specified reference file. If `file` is a symbolic link and the `-L`
    option is in use, then the last modification time of the file pointed to by
    the symbolic link is used.
-   `-anewer file`: Checks if the file last access time is greater than that of
    the specified reference file. If `file` is a symbolic link and the `-L`
    option is in use, then the last access time of the file pointed to by the
    symbolic link is used.
-   `-cnewer file`: Checks if the file creation time is greater than that of
    the specified reference file. If `file` is a symbolic link and the `-L`
    option is in use, then the creation time of the file pointed to by the
    symbolic link is used.
-   `-gid [-|+]number`: Checks if the file is owned by a group with an ID less
    than, greater than or exactly `number`.
-   `-uid [-|+]number`: Checks if the file is owned by a user with an ID less
    than, greater than or exactly `number`.
-   `-print`: Outputs the file path, followed by a newline. Always evaluates to
    true.
-   `-print0`: Outputs the file path, followed by a zero byte. Always evaluates to
    true.
-   `-exec command... ;`: Executes the given command with any arguments provided,
    substituting the file path for any arguments specified as `{}`. The list of
    arguments must be terminated by a semicolon. Checks if the command exits
    successfully.
-   `-ok command... ;`: Behaves identically to the `-exec` command, but will
    prompt the user for confirmation before executing the given command. An
    affirmative response is any response that begins with the 'y' character.
    Any non-affirmative response will cause the command to not be executed and
    the value returned by `-ok` to be false.

The commands can be combined to form complex expressions using the following
operators:

-   `! command`: Logical NOT.
-   `command1 -o command2`: Logical OR.
-   `command1 -a command2`, `command1 command2`: Logical AND.
-   `( command )`: Groups commands together for operator priority purposes.

Commands which take a numeric argument `n` (`-links` and `-size` for example),
may be prefixed by a plus sign ('+') or a minus sign ('-'). A plus sign means
"grater than `n`", while a minus sign means "less than `n`". A numeric argument
with no prefix means "exactly equal".

## Examples

```sh
# Output a tree of paths rooted at the current directory:
$ find
# Output only directories:
$ find -type d
# Remove all sockets and any files owned by anon in /tmp:
$ find /tmp "(" -type s -o -user anon ")" -exec rm "{}" ";"
# Concatenate files with weird characters in their names:
$ find -type f -print0 | xargs -0 cat
# Find files with the word "config" in their name:
$ find -name \*config\*
```

## See also

-   [`xargs`(1)](help://man/1/xargs)
