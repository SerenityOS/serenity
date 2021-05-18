describe("documentElement", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            expect(page.document.documentElement).not.toBeNull();
            // FIXME: Add this in once HTMLHtmlElement's constructor is implemented.
            //expect(document.documentElement).toBeInstanceOf(HTMLHtmlElement);
            expect(page.document.documentElement.nodeName).toBe("HTML");
        });

        // FIXME: Add this in once removeChild is implemented.
        test.skip("Nullable", () => {
            page.document.removeChild(page.document.documentElement);
            expect(page.document.documentElement).toBeNull();
        });
    });

    waitForPageToLoad();
});
