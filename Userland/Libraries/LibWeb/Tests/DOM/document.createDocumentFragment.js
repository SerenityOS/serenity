describe("createDocumentFragment", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const fragment = page.document.createDocumentFragment();

            // FIXME: Add this in once DocumentFragment's constructor is implemented.
            //expect(fragment).toBeInstanceOf(DocumentFragment);
            expect(fragment.nodeName).toBe("#document-fragment");
        });
    });

    waitForPageToLoad();
});
