## Name

test-js - run the LibJS test suite

## Synopsis

```**sh
$ test-js [options...] [path]
```

## Description

`test-js` runs the LibJS test suite located in `/home/anon/Test/js-tests`. These
tests are using a custom JavaScript testing framework inspired by
[Jest](https://jestjs.io) (see [`test-common.js`](/home/anon/Tests/js-tests/test-common.js)).

It also supports the [test262 parser tests](https://github.com/tc39/test262-parser-tests).

The test root directory is assumed to be `/home/anon/Tests/js-tests`, or `$SERENITY_SOURCE_DIR/Userland/Libraries/LibJS/Tests`
when using the Lagom build. Optionally you can pass a custom path to `test-js` to override these defaults.

You can disable output from `dbgln()` calls by setting the `DISABLE_DBG_OUTPUT` environment variable.

## Options

-   `-t`, `--show-time`: Show duration of each test
-   `-p`, `--show-progress`: Show progress with OSC 9 (true, false)
-   `-j`, `--json`: Show results as JSON
-   `--per-file`: Show detailed per-file results as JSON (implies -j)
-   `-g`, `--collect-often`: Collect garbage after every allocation
-   `-b`, `--run-bytecode`: Use the bytecode interpreter
-   `-d`, `--dump-bytecode`: Dump the bytecode
-   `-f glob`, `--filter glob`: Only run tests matching the given glob
-   `--test262-parser-tests`: Run test262 parser tests

## Examples

A very simple test looks like this:

```js
describe("Examples from Gary Bernhardt's 'Wat' talk", () => {
    test("Na na na na na na na na na na na na na na na na Batman!", () => {
        expect(Array(16).join("wat" - 1) + " Batman!").toBe(
            "NaNNaNNaNNaNNaNNaNNaNNaNNaNNaNNaNNaNNaNNaNNaN Batman!"
        );
    });
});
```

## See also

-   [`js`(1)](help://man/1/js)
