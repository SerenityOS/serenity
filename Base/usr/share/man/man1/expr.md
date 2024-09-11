## Name

expr - evaluate expressions

## Synopsis

```**sh
$ expr <expression>
$ expr [--help]
```

## Description

expr evaluates and prints the result of an expression as described below to standard output.

An _expression_ may be any of the following:

-   `expr1 | expr2`
    `expr2` if `expr1` is falsy, `expr1` otherwise.
-   `expr1 & expr2`
    `expr1` if neither expression is falsy, `0` otherwise.
-   `expr1 < expr2`
    `1` if `expr1` is less than `expr2`, `0` otherwise.
-   `expr1 <= expr2`
    `1` if `expr1` is less than or equal to `expr2`, `0` otherwise.
-   `expr1 = expr2`
    `1` if `expr1` is equal to `expr2`, `0` otherwise.
-   `expr1 = expr2`
    `1` if `expr1` is not equal to `expr2`, `0` otherwise.
-   `expr1 => expr2`
    `1` if `expr1` is greater than or equal to `expr2`, `0` otherwise.
-   `expr1 > expr2`
    `1` if `expr1` is greater than `expr2`, `0` otherwise.
-   `expr1 + expr2`
    arithmetic integral sum of `expr1` and `expr2`.
-   `expr1 - expr2`
    arithmetic integral difference of `expr1` and `expr2`.
-   `expr1 * expr2`
    arithmetic integral product of `expr1` and `expr2`.
-   `expr1 / expr2`
    arithmetic integral quotient of `expr1` divided by `expr2`.
-   `expr1 % expr2`
    arithmetic integral quotient of `expr1` divided by `expr2`.
-   `expr1 : expr2`
    pattern match of `expr2` as a regular expression in `expr1` - currently not implemented.
-   `match expr1 expr2`
    same as `expr1 : expr2`.
-   `substr expr1 expr2 expr3`
    substring with length `expr3` of `expr1`, starting at `expr2`, indices starting at 1.
-   `index expr1 expr2`
    index of `expr2` in `expr1`, starting at 1. 0 if not found.
-   `length expr1`
    length of the string `expr1`
-   `+ token`
    interpret `token` as a string, regardless of whether it is a keyword or an operator.
-   `( expr )`
    value of `expr`

Note that many operators will need to be escaped or quoted if used from within a shell.
"falsy" means either the number 0, or the empty string.

## Options

-   `--help`: Prints usage information and exits.

## Examples

```sh
$ expr 1 + 2 * 3          # = 7
$ expr \( 1 + 2 \) = 3    # = 1
$ expr substr foobar 1 3  # foo
```

## See also

-   [`test`(1)](help://man/1/test)
-   [`js`(1)](help://man/1/js) for evaluating more complex expressions
