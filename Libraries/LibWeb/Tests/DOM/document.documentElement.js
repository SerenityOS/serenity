loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.documentElement).not.toBe(null);
        expect(document.documentElement.nodeName).toBe("html");
    });
});
