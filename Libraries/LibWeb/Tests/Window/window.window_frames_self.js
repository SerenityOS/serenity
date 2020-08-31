loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("window.{window,frames,self} all return the Window object", () => {
        expect(window.window).toBe(window.frames);
        expect(window.window).toBe(window.self);
    });
});
