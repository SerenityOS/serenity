describe("HTMLScriptElement.supports", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("length is 1", () => {
            expect(page.HTMLScriptElement.supports).toHaveLength(1);
        });

        test("Basic functionality", () => {
            expect(page.HTMLScriptElement.supports("classic")).toBeTrue();
            expect(page.HTMLScriptElement.supports("module")).toBeTrue();
            expect(page.HTMLScriptElement.supports("abc")).toBeFalse();

            // Is case sensitive.
            expect(page.HTMLScriptElement.supports("Classic")).toBeFalse();
            expect(page.HTMLScriptElement.supports("Module")).toBeFalse();

            // Doesn't strip whitespace.
            expect(page.HTMLScriptElement.supports(" classic ")).toBeFalse();
            expect(page.HTMLScriptElement.supports(" module ")).toBeFalse();
        });
    });

    waitForPageToLoad();
});
