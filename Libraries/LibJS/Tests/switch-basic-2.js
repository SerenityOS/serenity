var a = "foo";

switch (a + "bar") {
case 1:
    assertNotReached();
    break;
case "foobar":
case 2:
    console.log("PASS");
    break;
}
