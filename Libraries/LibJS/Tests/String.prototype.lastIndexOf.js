load("test-common.js");

try {
    assert(String.prototype.lastIndexOf.length === 1);
    assert("hello friends".lastIndexOf() === -1);
    assert("hello friends".lastIndexOf("e") === 9);
    assert("hello friends".lastIndexOf("e", -7) === -1);
    assert("hello friends".lastIndexOf("e", 100) === 9);
    assert("hello friends".lastIndexOf("") === 13);
    assert("hello friends".lastIndexOf("Z") === -1);
    assert("hello friends".lastIndexOf("serenity") === -1);
    assert("hello friends".lastIndexOf("", 4) === 4);
    assert("hello serenity friends".lastIndexOf("serenity") === 6);
    assert("hello serenity friends serenity".lastIndexOf("serenity") === 23);
    assert("hello serenity friends serenity".lastIndexOf("serenity", 14) === 6);
    assert("".lastIndexOf("") === 0);
    assert("".lastIndexOf("", 1) === 0);
    assert("".lastIndexOf("", -1) === 0);
    assert("hello friends serenity".lastIndexOf("h", 10) === 0);
    assert("hello friends serenity".lastIndexOf("l", 4) === 3);
    assert("hello friends serenity".lastIndexOf("s", 13) === 12);
    assert("hello".lastIndexOf("serenity") === -1);

    console.log("PASS");
} catch (err) {
    console.log("FAIL: " + err);
}
