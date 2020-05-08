load("test-common.js");

switch (1 + 2) {
case '3':
    assertNotReached();
case 3:
    console.log("PASS");
    break;
case 5:
case 1:
    break;
default:
    break;
}
