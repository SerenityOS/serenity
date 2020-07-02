load("test-common.js");

try {
    (function() {
        assert(!isStrictMode());
    })();

    (function() {
        'use strict';
        assert(isStrictMode());
    })();

    (function() {
        "use strict";
        assert(isStrictMode());
    })();

    (function() {
        `use strict`;
        assert(!isStrictMode());
    })();

    (function() {
        ;'use strict';
        assert(!isStrictMode());
    })();

    (function() {
        ;"use strict";
        assert(!isStrictMode());
    })();

    (function() {
        "use strict";
        (function() {
            assert(isStrictMode());
        })();
    })();

    (function() {
        assert(!isStrictMode());
        (function(){
            "use strict";
            assert(isStrictMode());
        })();
        assert(!isStrictMode());
    })();

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
