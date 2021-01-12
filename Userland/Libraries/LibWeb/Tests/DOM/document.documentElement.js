loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.documentElement).not.toBeNull();
        // FIXME: Add this in once HTMLHtmlElement's constructor is implemented.
        //expect(document.documentElement).toBeInstanceOf(HTMLHtmlElement);
        expect(document.documentElement.nodeName).toBe("html");
    });

    // FIXME: Add this in once removeChild is implemented.
    test.skip("Nullable", () => {
        document.removeChild(document.documentElement);
        expect(document.documentElement).toBeNull();
    });
});
