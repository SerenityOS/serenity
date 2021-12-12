describe("normal behavior", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const textEncoder = new page.TextEncoder();

            expect(textEncoder.encoding).toBe("utf-8");
        });
    });
    waitForPageToLoad();
});
