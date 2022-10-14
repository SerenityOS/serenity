describe("readyState", () => {
    loadLocalPage("/res/html/misc/blank.html");

    beforeInitialPageLoad(page => {
        window.events = [];

        page.document.addEventListener("readystatechange", () => {
            window.events.push(page.document.readyState);
        });

        page.document.addEventListener("DOMContentLoaded", () => {
            test("Ready state should be 'interactive' when 'DOMContentLoaded' fires", () => {
                expect(page.document.readyState).toBe("interactive");
            });
        });

        test("Ready state should be 'loading' initially", () => {
            expect(page.document.readyState).toBe("loading");
        });
    });

    afterInitialPageLoad(page => {
        test("'interactive' should come before 'complete' and both should have happened", () => {
            expect(page.window.events).toHaveLength(2);
            expect(page.window.events[0]).toBe("interactive");
            expect(page.window.events[1]).toBe("complete");
        });

        test("Ready state should be 'complete' after loading", () => {
            expect(page.document.readyState).toBe("complete");
        });
    });
    waitForPageToLoad();
});
