loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.head).not.toBeNull();
        // FIXME: Add this in once HTMLHeadElement's constructor is implemented.
        //expect(document.head).toBeInstanceOf(HTMLHeadElement);
        expect(document.head.nodeName).toBe("head");
    });

    // FIXME: Add this in once removeChild is implemented.
    test.skip("Nullable", () => {
        document.documentElement.removeChild(document.head);
        expect(document.head).toBeNull();
    });
});
