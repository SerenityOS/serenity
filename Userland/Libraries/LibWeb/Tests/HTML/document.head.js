describe("head", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            expect(page.document.head).not.toBeNull();
            // FIXME: Add this in once HTMLHeadElement's constructor is implemented.
            //expect(page.document.head).toBeInstanceOf(HTMLHeadElement);
            expect(page.document.head.nodeName).toBe("HEAD");
        });

        // FIXME: Add this in once removeChild is implemented.
        test.skip("Nullable", () => {
            page.document.documentElement.removeChild(page.document.head);
            expect(page.document.head).toBeNull();
        });
    });
    waitForPageToLoad();
});
