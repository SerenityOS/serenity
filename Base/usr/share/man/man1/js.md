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
* `-t`, `--test-mode`: Run the interpreter with added functionality for the test harness

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

## Test mode

In test mode, the `load()` function is added to the global object and can be used
to load further test utility functions defined in `LibJS/Tests/test-common.js`.

Typically a test will look like this:

```js
load("test-common.js");

try {
    // test feature
    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
```

Available functions in `test-common.js`:

* `assert(expression)`: Throws an `AssertionError` if condition does not evaluate to a truthy value
* `assertNotReached()`: Throws an `AssertionError`, use to ensure certain code paths are never reached
