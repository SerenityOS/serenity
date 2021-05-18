describe("body", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            expect(page.document.body).not.toBeNull();
            // FIXME: Add this in once HTMLBodyElement's constructor is implemented.
            //expect(page.document.body).toBeInstanceOf(HTMLBodyElement);
            expect(page.document.body.nodeName).toBe("BODY");
        });

        // FIXME: Add this in once set_body is fully implemented.
        test.skip("Setting body to a new body element", () => {
            // Add something to body to see if it's gone afterwards
            const p = page.document.createElement("p");
            page.document.body.appendChild(p);

            expect(page.document.body.firstChild).toBe(p);

            const newBody = page.document.createElement("body");
            page.document.body = newBody;

            expect(page.document.body).not.toBeNull();
            expect(page.document.body.nodeName).toBe("BODY");

            // FIXME: Add this in once HTMLBodyElement's constructor is implemented.
            //expect(page.document.body).toBeInstanceOf(HTMLBodyElement);

            expect(page.document.body.firstChild).toBeNull();
        });

        // FIXME: Add this in once set_body is fully implemented.
        test.skip("Setting body to a new frameset element", () => {
            const newFrameSet = page.document.createElement("frameset");
            page.document.body = newFrameSet;

            expect(page.document.body).not.toBeNull();
            expect(page.document.body.nodeName).toBe("FRAMESET");

            // FIXME: Add this in once HTMLFrameSetElement's constructor is implemented.
            //expect(page.document.body).toBeInstanceOf(HTMLFrameSetElement);
        });

        // FIXME: Add this in once set_body is fully implemented.
        test.skip("Setting body to an element that isn't body/frameset", () => {
            expect(() => {
                page.document.body = page.document.createElement("div");
            }).toThrow(DOMException);
        });

        // FIXME: Add this in once removeChild is implemented.
        test.skip("Nullable", () => {
            page.document.page.documentElement.removeChild(page.document.body);
            expect(page.document.body).toBeNull();
        });
    });
    waitForPageToLoad();
});
