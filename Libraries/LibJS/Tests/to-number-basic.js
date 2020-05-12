load("test-common.js");

try {
    assert(+false === 0);
    assert(-false === 0);
    assert(+true === 1);
    assert(-true === -1);
    assert(+null === 0);
    assert(-null === 0);
    assert(+[] === 0);
    assert(-[] === 0);
    assert(+[,] === 0);
    assert(-[,] === 0);
    assert(+[null] === 0);
    assert(-[null] === 0);
    assert(+[undefined] === 0);
    assert(-[undefined] === 0);
    assert(+[[[[[]]]]] === 0);
    assert(-[[[[[]]]]] === 0);
    assert(+[[[[[42]]]]] === 42);
    assert(-[[[[[42]]]]] === -42);
    assert(+"" === 0);
    assert(-"" === 0);
    assert(+"42" === 42);
    assert(-"42" === -42);
    assert(+42 === 42);
    assert(-42 === -42);
    assert(+1.23 === 1.23);
    assert(-1.23 === -1.23);
    assert(+"1.23" === 1.23)
    assert(-"1.23" === -1.23)
    assert(+"Infinity" === Infinity);
    assert(+"+Infinity" === Infinity);
    assert(+"-Infinity" === -Infinity);
    assert(-"Infinity" === -Infinity);
    assert(-"+Infinity" === -Infinity);
    assert(-"-Infinity" === Infinity);
    assert(+"  \r  \t \n " === 0);
    assert(+"  \n  \t    Infinity   \r   " === Infinity);
    assert(+"\r     \n1.23   \t\t\t  \n" === 1.23);

    assert(isNaN(+undefined));
    assert(isNaN(-undefined));
    assert(isNaN(+{}));
    assert(isNaN(-{}));
    assert(isNaN(+{ a: 1 }));
    assert(isNaN(-{ a: 1 }));
    assert(isNaN(+[, , ,]));
    assert(isNaN(-[, , ,]));
    assert(isNaN(+[undefined, undefined]));
    assert(isNaN(-[undefined, undefined]));
    assert(isNaN(+[1, 2, 3]));
    assert(isNaN(-[1, 2, 3]));
    assert(isNaN(+[[[["foo"]]]]));
    assert(isNaN(-[[[["foo"]]]]));
    assert(isNaN(+"foo"));
    assert(isNaN(-"foo"));

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
