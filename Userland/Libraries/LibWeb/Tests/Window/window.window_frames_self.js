describe("window_frames_self", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("window.{window,frames,self} all return the Window object", () => {
            expect(page.window.window).toBe(page.window.frames);
            expect(page.window.window).toBe(page.window.self);
        });
    });
    waitForPageToLoad();
});
