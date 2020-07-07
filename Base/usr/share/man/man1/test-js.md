## Name

test-js - run the LibJS test suite

## Synopsis

```**sh
$ test-js [options...]
```

## Description

`test-js` runs the LibJS test suite located in `/home/anon/js-tests`. These
tests are using a custom JavaScript testing framework inspired by
[Jest](https://jestjs.io) (see [`test-common.js`](/home/anon/js-tests/test-common.js)).

## Options

* `-t`, `--show-time`: Show duration of each test

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

* [`js`(1)](js.md)
