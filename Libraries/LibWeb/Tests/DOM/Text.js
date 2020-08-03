loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        var title = document.getElementsByTagName("title")[0];
        expect(title.firstChild.nodeName).toBe("#text");
        expect(title.firstChild.data).toBe("Blank");
        expect(title.firstChild.length).toBe(5);
    });
});
