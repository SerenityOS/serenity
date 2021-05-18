describe("HTMLElement", () => {
    loadLocalPage("/res/html/misc/welcome.html");

    afterInitialPageLoad(page => {
        test("contentEditable attribute", () => {
            expect(page.document.body.contentEditable).toBe("inherit");
            expect(page.document.firstChild.nextSibling.nodeName).toBe("HTML");
            expect(page.document.firstChild.nextSibling.contentEditable).toBe("true");
        });
    });
    waitForPageToLoad();
});
