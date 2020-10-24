test("valid 'use strict; directive", () => {
    expect(
        (() => {
            "use strict";
            return isStrictMode();
        })()
    ).toBeTrue();
    expect(
        (() => {
            'use strict';
            return isStrictMode();
        })()
    ).toBeTrue();
});

test("invalid 'use strict; directive", () => {
    expect(
        (() => {
            " use strict ";
            return isStrictMode();
        })()
    ).toBeFalse();
    expect(
        (() => {
            `use strict`;
            return isStrictMode();
        })()
    ).toBeFalse();
    expect(
        (() => {
            "use\
            strict";
            return isStrictMode();
        })()
    ).toBeFalse();
    expect(
        (() => {
            "use\ strict";
            return isStrictMode();
        })()
    ).toBeFalse();
    expect(
        (() => {
            "use \163trict";
            return isStrictMode();
        })()
    ).toBeFalse();
});
