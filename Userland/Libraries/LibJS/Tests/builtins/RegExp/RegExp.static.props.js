test("RegExp.index", () => {
    for (let i = 1; i <= 9; i++) {
        const property = "$" + i;
        const desc = Object.getOwnPropertyDescriptor(RegExp, property);

        expect(desc.set).toBe(undefined);
        expect(typeof desc.get).toBe("function");
        expect(desc.enumerable).toBe(false);
        expect(desc.configurable).toBe(true);
    }

    var re = /(\w+)\s(\w+)/;
    var str = "John Smith";
    str.replace(re, "$2, $1"); // "Smith, John"
    expect(RegExp.$1).toBe("John");
    expect(RegExp.$2).toBe("Smith");

    var str = "Test 24";
    var number = /(\d+)/.test(str) ? RegExp.$1 : "0";
    expect(number).toBe("24");

    RegExp.$1 = "foo";
    expect(number).toBe("24");
});

test("RegExp.input", () => {
    var desc = Object.getOwnPropertyDescriptor(RegExp, "input");

    expect(typeof desc.get).toBe("function");
    expect(typeof desc.set).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    desc = Object.getOwnPropertyDescriptor(RegExp, "$_");
    expect(typeof desc.get).toBe("function");
    expect(typeof desc.set).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    var re = /hi/g;

    re.test("hi there!");
    expect(RegExp.input).toBe("hi there!");
    expect(RegExp.$_).toBe("hi there!");

    re.test("foo");
    expect(RegExp.input).toBe("hi there!");
    expect(RegExp.$_).toBe("hi there!");

    re.test("hi world!");
    expect(RegExp.input).toBe("hi world!");
    expect(RegExp.$_).toBe("hi world!");
});

test("RegExp.lastMatch", () => {
    var desc = Object.getOwnPropertyDescriptor(RegExp, "lastMatch");

    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    desc = Object.getOwnPropertyDescriptor(RegExp, "$&");

    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/lastMatch#examples
    var re = /hi/g;
    re.test("hi there!");
    expect(RegExp.lastMatch).toBe("hi");
    expect(RegExp["$&"]).toBe("hi");

    // https://docs.microsoft.com/en-us/openspecs/ie_standards/ms-es6/07639c84-2222-2222-a266-7565d82eb8bd
    var re = /a|c/;

    re.exec("az");
    expect(RegExp.lastMatch).toBe("a");
    expect(RegExp["$&"]).toBe("a");

    re.exec("bz"); // not match, keep the previous value
    expect(RegExp.lastMatch).toBe("a");
    expect(RegExp["$&"]).toBe("a");

    re.exec("cz"); // match again, update the lastMatch value
    expect(RegExp.lastMatch).toBe("c");
    expect(RegExp["$&"]).toBe("c");
});

test("RegExp.lastParen", () => {
    var desc = Object.getOwnPropertyDescriptor(RegExp, "lastParen");

    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    desc = Object.getOwnPropertyDescriptor(RegExp, "$+");
    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/lastParen
    var re = /(hi)/g;
    re.test("hi there!");

    expect(RegExp.lastParen).toBe("hi");
    expect(RegExp["$+"]).toBe("hi");

    // https://docs.microsoft.com/en-us/openspecs/ie_standards/ms-es6/07639c84-2222-2222-a266-7565d82eb8bd
    var re = /(a|b)(c|d)?/;

    re.exec("ac");
    expect(RegExp.lastParen).toBe("c");
    expect(RegExp["$+"]).toBe("c");

    re.exec("z");
    expect(RegExp.lastParen).toBe("c");
    expect(RegExp["$+"]).toBe("c");

    re.exec("bd");
    expect(RegExp.lastParen).toBe("d");
    expect(RegExp["$+"]).toBe("d");
});

test("RegExp.leftContext", () => {
    var desc = Object.getOwnPropertyDescriptor(RegExp, "leftContext");

    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    desc = Object.getOwnPropertyDescriptor(RegExp, "$`");
    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    // https://docs.microsoft.com/en-us/openspecs/ie_standards/ms-es6/07639c84-2222-2222-a266-7565d82eb8bd
    var re = /world/g;

    // RegExp.leftContext === ''

    re.exec("Hello world");
    expect(RegExp.leftContext).toBe("Hello ");

    re.exec("failure");
    expect(RegExp.leftContext).toBe("Hello ");

    re.exec("Another hello world");
    expect(RegExp.leftContext).toBe("Another hello ");
});

test("RegExp.rightContext", () => {
    var desc = Object.getOwnPropertyDescriptor(RegExp, "rightContext");

    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    desc = Object.getOwnPropertyDescriptor(RegExp, "$'");
    expect(desc.set).toBe(undefined);
    expect(typeof desc.get).toBe("function");
    expect(desc.enumerable).toBe(false);
    expect(desc.configurable).toBe(true);

    // https://docs.microsoft.com/en-us/openspecs/ie_standards/ms-es6/07639c84-2222-2222-a266-7565d82eb8bd
    var re = /test/g;

    // RegExp.rightContext === ''

    re.exec("test right");
    expect(RegExp.rightContext).toBe(" right");

    re.exec("failure");
    expect(RegExp.rightContext).toBe(" right");

    re.exec("test right another");
    expect(RegExp.rightContext).toBe(" right another");
});
