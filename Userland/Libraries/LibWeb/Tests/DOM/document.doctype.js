describe("doctype", () => {
    loadLocalPage("/res/html/misc/blank.html");
    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            expect(page.document.compatMode).toBe("CSS1Compat");
            expect(page.document.doctype).not.toBeNull();
            expect(page.document.doctype.name).toBe("html");
            expect(page.document.doctype.publicId).toBe("");
            expect(page.document.doctype.systemId).toBe("");
        });
    });
    waitForPageToLoad();

    loadLocalPage("/res/html/misc/blank-no-doctype.html");
    afterInitialPageLoad(page => {
        test("Quirks mode", () => {
            expect(page.document.compatMode).toBe("BackCompat");
            expect(page.document.doctype).toBeNull();
        });
    });
    waitForPageToLoad();
});
