if (Testobj.getVal() != 'Hello World') {
    throw "unexpected value";
}

Testobj = "a string";
if (Testobj.getVal != undefined ||
    Testobj != 'a string') {
    throw "can' change Testobj?";
}
