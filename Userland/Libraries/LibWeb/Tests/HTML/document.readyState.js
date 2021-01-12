loadPage("file:///res/html/misc/blank.html");

beforeInitialPageLoad(() => {
    window.events = [];

    document.addEventListener("readystatechange", () => {
        window.events.push(document.readyState);
    });

    document.addEventListener("DOMContentLoaded", () => {
        test("Ready state should be 'interactive' when 'DOMContentLoaded' fires", () => {
            expect(document.readyState).toBe("interactive");
        });
    });

    test("Ready state should be 'loading' initially", () => {
        expect(document.readyState).toBe("loading");
    });
});

afterInitialPageLoad(() => {
    test("'interactive' should come before 'complete' and both should have happened", () => {
        expect(window.events).toHaveLength(2);
        expect(window.events[0]).toBe("interactive");
        expect(window.events[1]).toBe("complete");
    });

    test("Ready state should be 'complete' after loading", () => {
        expect(document.readyState).toBe("complete");
    });
});
