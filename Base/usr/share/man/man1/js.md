## Name

js - evaluate JavaScript

## Synopsis

```**sh
$ js [options...] [script.js]
```

## Description

`js` evaluates JavaScript programs using the LibJS engine. If you pass it a path
to a script file, it will execute that script. Otherwise, it enters the
Read-Eval-Print-Loop (REPL) mode, where it interactively reads pieces (usually,
single lines) of code from standard input, evaluates them in one shared
interpreter context, and prints back their results. This mode is useful for
quickly experimenting with LibJS.

Run `help()` in REPL mode to see its available built-in functions.

## Options

* `-A`, `--dump-ast`: Dump the Abstract Syntax Tree after parsing the program.
* `-l`, `--print-last-result`: Print the result of the last statement executed.
* `-g`, `--gc-on-every-allocation`: Run garbage collection on every allocation.
* `-s`, `--no-syntax-highlight`: Disable live syntax highlighting in the REPL

## Examples

Here's how you execute a script:

```sh
$ js ~/js/type-play.js
```

And here's an example of an interactive REPL session:

```js
$ js
> function log_sum(a, b) {
>     console.log(a + b)
> }
undefined
> log_sum(35, 42)
77
undefined
```

## See also

* [`test-js`(1)](test-js.md)
