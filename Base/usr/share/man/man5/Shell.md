## Name

The Shell Command Language

## Introduction

The shell operates according to the following general steps:

-   Some string is read from a source, be it a file, the standard input, or a command string (see [`Shell`(1)](help://man/1/Shell))
-   The shell parses the input to an abstract syntax tree
-   The shell performs various expansions and/or resolutions on the nodes
-   The shell performs various type checks and syntactic checks
-   The shell interprets the AST, evaluating commands as needed
-   For each given command, the shell flattens all the string/list arguments
-   For each given command, the shell records the applicable redirections
-   Should a command be executed, the shell applies the redirections, and executes the command with the flattened argument list
-   Should a command need waiting, the shell shall wait for the command to finish, and continue execution

Any text below is superseded by the formal grammar defined in the _formal grammar_ section.

## General Token Recognition

This section describes the general tokens the language accepts, it should be noted that due to nature of the language, some tokens are valid only in a specific context.

##### Bareword

String of characters that are not _Special_ or _Syntactic Elements_

##### Glob

String of characters containing at least one of `*?` in _bareword_ position

##### History Events

A _designator_ starting with `!` in _bareword_ position that describes a word or a range of words from a previously entered command.
Please look at the section named 'History Event Designators' for a more thorough explanation. Only allowed in interactive mode.

##### Single Quoted String

Any sequence of characters between two single quotes (`'`)

##### Double Quoted String

Any sequence of _Double Quoted String Part_ tokens:

-   Barewords
-   Single Quotes
-   Variable References
-   History Events
-   Evaluate expressions
-   Escaped sequences

##### Heredocs

Heredocs are made in two parts, the _initiator_ and the _contents_, the _initiator_ may be used in place of a string (i.e. wherever a string is allowed to be used), with the constraint that the _contents_ must follow the _sequence_ that the _initiator_ is used in.

There are four different _initiators_:

-   `<<-token`: The _contents_ may contain interpolations, and are terminated with a line containing only whitespace and then _token_
-   `<<-'token'`: The _contents_ may _not_ contain interpolations, but otherwise is the same as `<<-token`
-   `<<~token`: Similar to `<<-token`, but the starting whitespace of the lines in the _contents_ is stripped, note that this happens after any and all expansions/interpolations are done.
-   `<<~'token'`: Dedents (i.e. strips the initial whitespace) like `<<~token`, and disallows interpolations like `<<-'token'`.

Note that heredocs _must_ be listed in the same order as they are used after a sequence that has been terminated with a newline.

##### Variable Reference

Any sequence of _Identifier_ characters, or a _Special Variable_ following a `$`.
Variables may be followed by a _Slice_ (see [Slice](#slice))

##### Slice

Variables may be sliced into, which will allow the user to select a subset of entries in the contents of the variable.
An expression of the form $_identifier_[_slice-contents_] can be used to slice into a variable, where _slice-contents_ has semantics similar to _Brace Expansions_, but it may only evaluate to numeric values, that are used to index into the variable being sliced.
Negative indices are allowed, and will index the contents from the end. It should be noted that the shell will always perform bounds-checking on the indices, and raise an error on out-of-bound accesses. Slices can slice into both lists and strings.

For example, `$lst[1..-2]` can be used to select a permutation of a 4-element list referred to by the variable `lst`, as the slice will evaluate to the list `(1 0 -1 -2)`, which will select the indices 1, 0, 3, 2 (in that order).

##### Immediate Expressions

An expression of the form '${identifier expression...}', such expressions are expanded to other kinds of nodes before resolution, and are internal functions provided by the shell.
Currently, the following functions are exposed:

-   ${length (string|list)? _expression_}
    Finds the length of the given _expression_. if either `string` or `list` is given, the shell will attempt to treat _expression_ as that type, otherwise the type of _expression_ will be inferred.

-   ${length*across (string|list) \_expression*}
    Finds the lengths of the entries in _expression_, this requires _expression_ to be a list.
    If either `string` or `list` is given, the shell attempts to treat the elements of _expression_ as that type, otherwise the types are individually inferred.

-   ${split _delimiter_ _string_}
    Splits the _string_ with _delimiter_, and evaluates to a list.
    Both _string_ and _delimiter_ must be strings.

-   ${remove*suffix \_suffix* _string_}
    Removes the suffix _suffix_ (if present) from the given _string_.

-   ${remove*prefix \_prefix* _string_}
    Removes the prefix _prefix_ (if present) from the given _string_.

-   ${concat*lists \_list*...}
    Concatenates all the given expressions as lists, and evaluates to a list.

-   ${regex*replace \_pattern* _replacement-template_ _string_}
    Replaces all occurrences of the regular expression _pattern_ in the given _string_, using the given _replacement-template_.
    Capture groups in _pattern_ can be referred to as `\<group_number>` in the _replacement template_, for example, to reference capture group 1, use `\1`.

##### Evaluate expression

Any expression following a `$` that is not a variable reference:

-   Inline execution: A _syntactic list_ following a `$`:
-   Dynamic evaluation: Any other expression following a `$`

##### Lists

Any two expressions joined by the Join operator (` ` [whitespace]), or a _variable reference_ referring to a list value

-   Syntactic Lists: Any _list_ enclosed in parentheses (`(` and `)`)

##### Comments

Any text following and including that in a word starting with `#`, up to but not including a newline

##### Keywords

The following tokens:

-   `for` in command name position
-   `in` as a syntactic element of a `for` expression
-   `if` in command name position, or after the `else` keyword
-   `else` after a partial `if` expression
-   `match` in command name position
-   `as` as part of a `match` expression

##### Special characters

Any of the following:

-   `;` in bareword position
-   `\\n` (a newline) in _bareword_ position
-   Any of `(){}`
-   Any of `*?` not in _glob_ position

##### Tilde

Any initial path segment starting with the character `~` in _bareword_ position, Optionally followed by a _bareword_ for the username

## Redirections

The shell can create various redirections to file descriptors of a command before executing it, the general syntax for redirections is an optional file descriptor, followed by a redirection operator, followed by a destination.

There are four redirection operators corresponding to various file descriptor open modes: `Read`, `Write`, `WriteAppend` and `ReadWrite`, respectively `<`, `>`, `>>` and `<>`.

A special syntactic element `&fd` can reference a file descriptor as a destination.

Redirections take two main forms, Read/Write redirections, and fd closure redirections.

##### Read/Write

-   Allowed operators: all
-   Allowed destinations: file paths (any shell _expression_) and _file descriptor references_

##### Close

-   Allowed operators: `Write` (`>`)
-   Allowed destinations: the special "close" reference `&-`

#### Examples

```sh
# Redirect the standard error to a file, and close the standard input
$ 2> foo 1>&-

# Redirect a file as read-write into the standard input
$ 1<>foo

# Redirect the standard output to /dev/null
$ >/dev/null
```

## Expansions

The shell performs various expansions, in different stages.

-   Glob Expansion: Globs shall be expanded to a list.

-   Variable Expansion: Variables shall be expanded preserving their types.

-   Brace Expansions: Brace expansions shall be expanded to a list.

-   Juxtaposition Expansion: Juxtapositions shall be expanded as list products.

-   Other expansions: Tildes, Evaluate expressions, etc. shall be expanded as needed.

### Brace Expansions

Brace expansions are of two kinds, _normal brace expansions_ and _range brace expansions_.
_Normal brace expansions_ are sequences of optional expressions inside braces (`{}`), delimited by a comma (`','`); a missing expression is treated as an empty string literal. Such expressions are simply expanded to the expressions they enclose.
_Range brace expansions_ are of the form `{start_expression..end_expression}`, where `start_expression` and `end_expression` denote the bounds of an inclusive _range_, and can be one of two types:

-   Single unicode code points: The range expands to all code points between the start and end, e.g. `{a..c}` shall expand to the list `(a b c)`.
-   Numbers: The range expands to all numbers between the start and end, e.g. `{8..11}` shall expand to the list `(8 9 10 11)`.

### Juxtapositions

Any two expressions joined without any operator are considered to be in a Juxtaposition, with the resulting value being the list product of two expressions.
For instance, `(1 2)(3 4)` shall be evaluated to `(13 14 23 24)` by calculating the list product of the two expressions `(1 2)` and `(3 4)`.

### Tildes

Any bareword starting with a tilde (`~`) and spanning up to the first path separator (`/`) - or EOL - is considered to be a tilde expansion with the text between the tilde and the separator being the _username_, which shall be expanded to a single string containing the home directory of the given _username_ (or the current user if no username is provided).

### Evaluate

Evaluate expressions take the general form of a dollar sign (`$`) followed by some _expression_, which is evaluated by the rules below.

-   Should the _expression_ be a string, it shall be evaluated as a dynamic variable lookup by first evaluating the string, and then looking up the given variable.
-   Should the _expression_ be a list or a command, it shall be converted to a command, whose output (from the standard output) shall be captured, and split to a list with the shell local variable `IFS` (or the default splitter `\n` (newline, 0x0a)). It should be noted that the shell option `inline_exec_keep_empty_segments` will determine whether empty segments in the split list shall be preserved when this expression is evaluated, this behavior is disabled by default.

## Commands

A `Command` is a single simple command, containing arguments and redirections for a single program, or a compound command containing a shell control structure. The shell can evaluate a sequence of commands, a conditional relation between commands, or various semantic elements composed of commands and intrinsics.

Commands can be either calls to Shell builtins, or external programs.

## Shell Semantic Elements

The commands can be composed into semantic elements, producing composite commands:

### Sequences

A sequence of commands, executed serially independent of each other: `Command ; Command ; Command ...`

It should be noted that a newline (`\\n`) can be substituted for the semicolon (`;`).

#### Example

```sh
# Do one thing, then do another
echo foo; echo bar
```

### Logical Relations

A sequence of commands whose execution depends somehow on the result of another

#### `Command && Command && Command ...` (AND)

Short-circuiting command evaluations, will cancel the entire chain should any command fails (have a non-zero exit code)

#### `Command || Command || Command ...` (OR)

Short-circuiting command evaluation, will continue down the chain if any command fails.

It should be noted that `And` chains bind more tightly than `Or` chains, so an expression of the form `C1 && C2 || C3` is understood as "evaluate `C1`, if successful, evaluate `C2`, if not successful, evaluate `C3`".

##### Examples

```sh
# Create file if not found
test -f foo.txt || touch foo.txt

# Announce execution status of a command
rm test && echo "deleted!" || echo "failed with $?"
```

#### Control Structures

##### Conditionals

Conditionals can either be expressed with the _Logical Relations_, or via explicit `if` expressions.
An `if` expression contains at least a _condition_ command and a _then clause_, and optionally the `else` keyword followed by an _else clause_.
An _else clause_ may contain another `if` expression instead of a normal block.

The _then clause_ **must** be surrounded by braces, but the _else clause_ may also be another `if` expression.

An `if` expression evaluates either the _then clause_ or (if available) the _else clause_, based on the exit code of the _condition_ command; should the exit code be zero, the _then clause_ will be executed, and if not, the _else clause_ will.

###### Examples

```sh
# Remove a file if it exists, create it otherwise
if test -e the_file {
    rm the_file
} else {
    touch the_file
}

# Cond chain (if-elseif-else)
if A {
    echo A
} else if B {
    echo B
} else {
    echo C
}
```

##### For Loops

For Loops evaluate a sequence of commands once per element in a given list.
The shell has two forms of _for loops_, one with an explicitly named iteration variable, and one with an implicitly named one.
The general syntax follows the form `for index index_name name in expr { sequence }`, and allows omitting the `index index_name name in` part to implicitly name the variable `it`.

It should be noted that the `index index_name` section is optional, but if supplied, will require an explicit iteration variable as well.
In other words, `for index i in foo` is not valid syntax.

A for-loop evaluates the _sequence_ once per every element in the _expr_, setting the local variable _name_ to the element being processed, and the local variable _enum name_ to the enumeration index (if set).

The Shell shall cancel the for loop if two consecutive commands are interrupted via SIGINT (\^C), and any other terminating signal aborts the loop entirely.

###### Examples

```sh
# Iterate over every non-hidden file in the current directory, and prepend '1-' to its name.
$ for * { mv $it 1-$it }

# Iterate over a sequence and write each element to a file
$ for i in $(seq 1 100) { echo $i >> foo }

# Iterate over some files and get their index
$ for index i x in * { echo file at index $i is named $x }
```

##### Infinite Loops

Infinite loops (as denoted by the keyword `loop`) can be used to repeat a block until the block runs `break`, or the loop terminates by external sources (interrupts, program exit, and terminating signals).

The behavior regarding SIGINT and other signals is the same as for loops (mentioned above).

###### Examples

```sh
# Keep deleting a file
loop {
    rm -f foo
}
```

##### Subshells

Subshells evaluate a given block in a new instance (fork) of the current shell process. to create a subshell, any valid shell code can be enclosed in braces.

###### Examples

```sh
# Run a block of code in the background, in a subshell, then detach it from the current shell
$ { for * { te $it } }&
$ disown
```

##### Functions

A function is a user-defined entity that can be used as a simple command to execute a compound command, optionally with some parameters.
Such a function is defined via the syntax below:

```sh
function_name(explicitly_named_arguments...) { compound_command }
```

The function is named `function_name`, and has some explicitly named arguments `explicitly_named_arguments...`, which _must_ be supplied by the caller, failure to do so will cause the command to exit with status 1.

The compound command shall be executed whenever the simple command `function_name` is executed.
This execution shall be performed in a new local frame.

Additionally, should the simple command containing the function name be in a pipeline, or requested to be run in the background, this execution shall be moved to a subshell; naturally, in such a case any changes to the shell state (such as variables, aliases, etc) shall not be leaked to the parent shell process.

The passed arguments shall be stored in the special variables `*` and `ARGV`, and the explicitly named arguments shall be set, in order, from the first passed argument onwards.

The exit status of a function simple command shall be the exit status of the last command executed within the command, or 0 if the function has no commands.
The declaration is _not_ a command, and will not alter the exit status.

###### Examples

```sh
fn(a b c) {
    echo $a $b $c \( $* \)
}

$ fn 1 2 3 4
# 1 2 3 ( 1 2 3 4 )
```

##### Match Expressions

The pattern matching construct `match` shall choose from a sequence of patterns, and execute the corresponding action in a new frame.
The choice is done by matching the result of the _matched expression_ (after expansion) against the _patterns_ (expanded down to either globs or literals).
Multiple _patterns_ can be attributed to a single given action by delimiting them with a pipe ('|') symbol.
A _pattern_ (or the series of) may be annotated with an extra `as (...)` clause, which allows globbed parts of the matching pattern to be named and used in the matching block.

The expanded _matched expression_ can optionally be given a name using the `as name` clause after the _matched expression_, with which it may be accessible in the action clauses.

###### Examples

```sh
# Match the result of running 'make_some_value' (which is a list when captured by $(...))
match "$(make_some_value)" as value {
    (hello*) { echo "Hi!" }
    (say\ *) { echo "No, I will not $value" }
}

# Match the result of running 'make_some_value', cast to a string
# Note the `as (expr)` in the second pattern, which assigns whatever the `*` matches
# to the name `expr` inside the block.
match "$(make_some_value)" {
    hello* { echo "Hi!" }
    say\ * as (expr) { echo "No, I will not say $expr!" }
}
```

### History Event Designators

History expansion may be utilized to reuse previously typed words or commands.
Such expressions are of the general form `!<event_designator>(:<word_designator>)`, where `event_designator` would select an entry in the shell history, and `word_designator` would select a word (or a range of words) from that entry.

| Event designator | Effect                                                                       |
| :--------------- | :--------------------------------------------------------------------------- |
| `!`              | The immediately preceding command                                            |
| _n_              | The _n_'th entry in the history, starting with 1 as the first entry          |
| -_n_             | The last _n_'th entry in the history, starting with -1 as the previous entry |
| _str_            | The most recent entry starting with _str_                                    |
| `?`_str_         | The most recent entry containing _str_                                       |

| Word designator | Effect                                                                                                             |
| :-------------- | :----------------------------------------------------------------------------------------------------------------- |
| _n_             | The word at index _n_, starting with 0 as the first word (usually the command)                                     |
| `^`             | The first argument (index 1)                                                                                       |
| `$`             | The last argument                                                                                                  |
| _x_-_y_         | The range of words starting at _x_ and ending at _y_ (inclusive). _x_ defaults to 0 if omitted                     |
| `*`             | All the arguments. Equivalent to `^`-`$`                                                                           |
| _x_`*`          | The range of words starting at _x_ and ending at the last word (`$`) (inclusive)                                   |
| _x_-            | The range of words starting at _x_ and ending at the second to last word (inclusive). _x_ defaults to 0 if omitted |

Note: The event designator and the word designator should usually be separated by a colon (`:`). This colon can be omitted only if the word designator starts with `^`, `$` or `*` (such as `!1^` for the first argument of the first entry in the history).

## Formal Grammar

### Shell Grammar

```
toplevel :: sequence?

sequence :: variable_decls? or_logical_sequence terminator sequence
          | variable_decls? or_logical_sequence '&' sequence
          | variable_decls? or_logical_sequence
          | variable_decls? function_decl (terminator sequence)?
          | variable_decls? terminator sequence

function_decl :: identifier '(' (ws* identifier)* ')' ws* '{' [!c] toplevel '}'

or_logical_sequence :: and_logical_sequence '|' '|' and_logical_sequence
                     | and_logical_sequence

and_logical_sequence :: pipe_sequence '&' '&' and_logical_sequence
                      | pipe_sequence

terminator :: ';'
            | '\n' [?!heredoc_stack.is_empty] heredoc_entries

heredoc_entries :: { .*? (heredoc_entry) '\n' } [each heredoc_entries]

variable_decls :: identifier '=' expression (' '+ variable_decls)? ' '*
                | identifier '=' '(' pipe_sequence ')' (' '+ variable_decls)? ' '*

pipe_sequence :: command '|' pipe_sequence
               | command
               | control_structure '|' pipe_sequence
               | control_structure

control_structure[c] :: for_expr
                      | loop_expr
                      | if_expr
                      | subshell
                      | match_expr
                      | ?c: continuation_control

continuation_control :: 'break'
                      | 'continue'

for_expr :: 'for' ws+ (('index' ' '+ identifier ' '+)? identifier ' '+ 'in' ws*)? expression ws+ '{' [c] toplevel '}'

loop_expr :: 'loop' ws* '{' [c] toplevel '}'

if_expr :: 'if' ws+ or_logical_sequence ws+ '{' toplevel '}' else_clause?

else_clause :: else '{' toplevel '}'
             | else if_expr

subshell :: '{' toplevel '}'

match_expr :: 'match' ws+ expression ws* ('as' ws+ identifier)? '{' match_entry* '}'

match_entry :: match_pattern ws* (as identifier_list)? '{' toplevel '}'

identifier_list :: '(' (identifier ws*)* ')'

match_pattern :: expression (ws* '|' ws* expression)*

command :: redirection command
         | list_expression command?

redirection :: number? '>'{1,2} ' '* string_composite
             | number? '<' ' '* string_composite
             | number? '>' '&' number
             | number? '>' '&' '-'

list_expression :: ' '* expression (' '+ list_expression)?

expression :: evaluate expression?
            | string_composite expression?
            | comment expression?
            | immediate_expression expression?
            | history_designator expression?
            | '(' list_expression ')' expression?

evaluate :: '$' '(' pipe_sequence ')'
          | '$' expression          {eval / dynamic resolve}

string_composite :: string string_composite?
                  | variable string_composite?
                  | bareword string_composite?
                  | glob string_composite?
                  | brace_expansion string_composite?
                  | heredoc_initiator string_composite?    {append to heredoc_entries}

heredoc_initiator :: '<' '<' '-' bareword         {*bareword, interpolate, no deindent}
                   | '<' '<' '-' "'" [^']* "'"    {*string, no interpolate, no deindent}
                   | '<' '<' '~' bareword         {*bareword, interpolate, deindent}
                   | '<' '<' '~' "'" [^']* "'"    {*bareword, no interpolate, deindent}

string :: '"' dquoted_string_inner '"'
        | "'" [^']* "'"

dquoted_string_inner :: '\' . dquoted_string_inner?       {concat}
                      | variable dquoted_string_inner?    {compose}
                      | . dquoted_string_inner?
                      | '\' 'x' xdigit*2 dquoted_string_inner?
                      | '\' 'u' xdigit*8 dquoted_string_inner?
                      | '\' [abefrnt] dquoted_string_inner?

variable :: variable_ref slice?

variable_ref :: '$' identifier
          | '$' '$'
          | '$' '?'
          | '$' '*'
          | '$' '#'
          | ...

slice :: '[' brace_expansion_spec ']'

comment :: (?<!\w) '#' .*

immediate_expression :: '$' '{' immediate_function expression* '}'

immediate_function :: identifier       { predetermined list of names, see Shell.h:ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS }

history_designator :: '!' event_selector (':' word_selector_composite)?

event_selector :: '!'                  {== '-0'}
                | '?' bareword '?'
                | bareword             {number: index, otherwise: lookup}

word_selector_composite :: word_selector ('-' word_selector)?

word_selector :: number
               | '^'                   {== 0}
               | '$'                   {== end}

bareword :: [^"'*$&|()[\]{} ?;<>] bareword?
          | '\' [^"'*$&|()[\]{} ?;<>] bareword?

bareword_with_tilde_expansion :: '~' bareword?

glob :: [*?] bareword?
      | bareword [*?]

brace_expansion :: '{' brace_expansion_spec '}'

brace_expansion_spec :: expression? (',' expression?)*
                      | expression '..' expression

digit :: <native hex digit>
number :: <number in base 10>
identifier :: <string of word characters>
```
