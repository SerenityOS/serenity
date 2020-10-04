test("Issue #3641, strict mode should be function- or program-level, not block-level", () => {
    function func() {
        expect(isStrictMode()).toBeFalse();

        {
            "use strict";
            expect(isStrictMode()).toBeFalse();
        }

        if (true) {
            "use strict";
            expect(isStrictMode()).toBeFalse();
        }

        do {
            "use strict";
            expect(isStrictMode()).toBeFalse();
        } while (false);
    }

    func();
});
