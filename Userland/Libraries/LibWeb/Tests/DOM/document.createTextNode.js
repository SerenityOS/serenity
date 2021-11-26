describe("createTextNode", () => {
    loadLocalPage("/res/html/misc/blank.html");
    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const text = page.document.createTextNode("Create Text Test");

            // FIXME: Add this in once Text's constructor is implemented.
            //expect(text).toBeInstanceOf(Text);
            expect(text.nodeName).toBe("#text");
            expect(text.data).toBe("Create Text Test");
            expect(text.length).toBe(16);
        });
    });
    waitForPageToLoad();
});
