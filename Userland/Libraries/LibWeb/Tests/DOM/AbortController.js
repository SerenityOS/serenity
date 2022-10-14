describe("AbortController", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const abortController = new page.AbortController();
            let timesCallbackCalled = 0;
            abortController.signal.addEventListener("abort", () => {
                timesCallbackCalled++;
            });

            abortController.abort();
            expect(abortController.signal.aborted).toBeTrue();

            abortController.abort();
            expect(timesCallbackCalled).toBe(1);
        });
    });
    waitForPageToLoad();
});
