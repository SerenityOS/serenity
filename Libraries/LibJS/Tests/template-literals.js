load("test-common.js");

try {
    assert(`foo` === "foo");
    assert(`foo{` === "foo{");
    assert(`foo}` === "foo}");
    assert(`foo$` === "foo$");
    assert(`foo\`` === "foo`")
    assert(`foo\$` === "foo$");
    
    assert(`foo ${undefined}` === "foo undefined");
    assert(`foo ${null}` === "foo null");
    assert(`foo ${5}` === "foo 5");
    assert(`foo ${true}` === "foo true");
    assert(`foo ${"bar"}` === "foo bar");
    assert(`foo \${"bar"}` === 'foo ${"bar"}');

    assert(`foo ${{}}` === "foo [object Object]");
    assert(`foo ${{ bar: { baz: "qux" }}}` === "foo [object Object]");
    assert(`foo ${"bar"} ${"baz"}` === "foo bar baz");
    assert(`${"foo"} bar baz` === "foo bar baz");
    assert(`${"foo bar baz"}` === "foo bar baz");

    let a = 27;
    assert(`${a}` === "27");
    assert(`foo ${a}` === "foo 27");
    assert(`foo ${a ? "bar" : "baz"}` === "foo bar");
    assert(`foo ${(() => a)()}` === "foo 27");

    assert(`foo ${`bar`}` === "foo bar");
    assert(`${`${`${`${"foo"}`} bar`}`}` === "foo bar");
    assert(`foo
    bar` === "foo\n    bar");
    
    assertThrowsError(() => {
        `${b}`;
    }, {
        error: ReferenceError,
        message: "'b' not known"
    })

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
