describe("currentScript", () => {
    loadLocalPage("/res/html/misc/blank.html");

    beforeInitialPageLoad(page => {
        expect(page.document.currentScript).toBeNull();
    });

    afterInitialPageLoad(page => {
        test("reset to null even if currentScript is adopted into another document", () => {
            const script = page.document.createElement("script");
            script.id = "test";
            script.innerText = `
                const newDocument = globalThis.pageObject.document.implementation.createHTMLDocument();
                const thisScript = globalThis.pageObject.document.getElementById("test");
                
                // currentScript should stay the same even across adoption.
                expect(globalThis.pageObject.document.currentScript).toBe(thisScript);
                newDocument.adoptNode(thisScript);
                expect(globalThis.pageObject.document.currentScript).toBe(thisScript);
            `;

            // currentScript should be null before and after running the script on insertion.
            expect(page.document.currentScript).toBeNull();
            expect(script.ownerDocument).toBe(page.document);

            globalThis.pageObject = page;
            page.document.body.appendChild(script);
            globalThis.pageObject = undefined;

            expect(page.document.currentScript).toBeNull();
            expect(script.ownerDocument).not.toBe(page.document);
        });
    });

    waitForPageToLoad();
});
