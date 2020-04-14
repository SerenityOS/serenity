try {
    assert(String.prototype.trim.length === 0);
    assert(String.prototype.trimStart.length === 0);
    assert(String.prototype.trimEnd.length === 0);
    assert("   hello friends  ".trim() === "hello friends");
    assert("hello friends   ".trim() === "hello friends");
    assert("   hello friends".trim() === "hello friends");
    assert("   hello friends".trimStart() === "hello friends");
    assert("hello friends   ".trimEnd() === "hello friends");
    assert("   hello friends".trimEnd() === "   hello friends");
    assert("hello friends   ".trimStart() === "hello friends   ");
    assert("   hello friends   ".trimEnd() === "   hello friends");
    assert("    hello friends   ".trimStart() === "hello friends   ");

    assert("\thello friends".trimStart() === "hello friends");
    assert("hello friends\t".trimStart() === "hello friends\t");
    assert("\thello friends\t".trimStart() === "hello friends\t");
    
    assert("\rhello friends".trimStart() === "hello friends");
    assert("hello friends\r".trimStart() === "hello friends\r");
    assert("\rhello friends\r".trimStart() === "hello friends\r");

    assert("hello friends\t".trimEnd() === "hello friends");
    assert("\thello friends".trimEnd() === "\thello friends");
    assert("\thello friends\t".trimEnd() === "\thello friends");
    
    assert("hello friends\r".trimEnd() === "hello friends");
    assert("\rhello friends".trimEnd() === "\rhello friends");
    assert("\rhello friends\r".trimEnd() === "\rhello friends");
    
    assert("hello friends\n".trimEnd() === "hello friends");
    assert("\r\nhello friends".trimEnd() === "\r\nhello friends");
    assert("\rhello friends\r\n".trimEnd() === "\rhello friends");

    assert("\thello friends\t".trim() === "hello friends");
    assert("\thello friends".trim() === "hello friends");
    assert("hello friends\t".trim() === "hello friends");
    
    assert("\rhello friends\r".trim() === "hello friends");
    assert("\rhello friends".trim() === "hello friends");
    assert("hello friends\r".trim() === "hello friends");
    
    assert("\rhello friends\n".trim() === "hello friends");
    assert("\r\thello friends".trim() === "hello friends");
    assert("hello friends\r\n".trim() === "hello friends");
    assert("  \thello friends\r\n".trim() === "hello friends");
    assert("\n\t\thello friends\r\n".trim() === "hello friends");
    assert("\n\t\thello friends\t\t".trim() === "hello friends");


    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
