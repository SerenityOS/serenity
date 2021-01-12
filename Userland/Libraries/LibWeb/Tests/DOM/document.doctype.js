loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.compatMode).toBe("CSS1Compat");
        expect(document.doctype).not.toBeNull();
        expect(document.doctype.name).toBe("html");
        expect(document.doctype.publicId).toBe("");
        expect(document.doctype.systemId).toBe("");
    });

    libweb_tester.changePage("file:///res/html/misc/blank-no-doctype.html");

    test("Quirks mode", () => {
        expect(document.compatMode).toBe("BackCompat");
        expect(document.doctype).toBeNull();
    });
});
