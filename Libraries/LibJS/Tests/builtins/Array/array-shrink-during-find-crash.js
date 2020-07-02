load("test-common.js");

try {
    var a, callbackCalled;

    callbackCalled = 0;
    a = [1, 2, 3, 4, 5];
    a.find(() => {
        callbackCalled++;
        a.pop();
    });
    assert(callbackCalled === 5);

    callbackCalled = 0;
    a = [1, 2, 3, 4, 5];
    a.findIndex(() => {
        callbackCalled++;
        a.pop();
    });
    assert(callbackCalled === 5);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
