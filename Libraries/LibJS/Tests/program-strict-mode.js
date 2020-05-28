"use strict";

load("test-common.js");

try {
    assert(isStrictMode());

    (function() {
       assert(isStrictMode());
    })();

    (function() {
        "use strict";
         assert(isStrictMode());
    })();


    (() => {
        assert(isStrictMode());
    })();

    (() => {
        "use strict";
        assert(isStrictMode());
    })();

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
