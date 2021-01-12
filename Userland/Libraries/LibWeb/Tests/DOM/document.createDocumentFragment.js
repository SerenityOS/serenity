loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        const fragment = document.createDocumentFragment();

        // FIXME: Add this in once DocumentFragment's constructor is implemented.
        //expect(fragment).toBeInstanceOf(DocumentFragment);
        expect(fragment.nodeName).toBe("#document-fragment");
    });
});
