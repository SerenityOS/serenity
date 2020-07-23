loadPage("file:///res/html/misc/blank.html");

afterPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.compatMode).toBe("CSS1Compat");
        expect(document.doctype).toBeDefined();
        expect(document.doctype.name).toBe("html");
        expect(document.doctype.publicId).toBe("");
        expect(document.doctype.systemId).toBe("");
    });
});
