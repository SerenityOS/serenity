loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        expect(document.body).not.toBeNull();
        // FIXME: Add this in once HTMLBodyElement's constructor is implemented.
        //expect(document.body).toBeInstanceOf(HTMLBodyElement);
        expect(document.body.nodeName).toBe("body");
    });

    // FIXME: Add this in once set_body is fully implemented.
    test.skip("Setting body to a new body element", () => {
        // Add something to body to see if it's gone afterwards
        const p = document.createElement("p");
        document.body.appendChild(p);

        expect(document.body.firstChild).toBe(p);

        const newBody = document.createElement("body");
        document.body = newBody;

        expect(document.body).not.toBeNull();
        expect(document.body.nodeName).toBe("body");

        // FIXME: Add this in once HTMLBodyElement's constructor is implemented.
        //expect(document.body).toBeInstanceOf(HTMLBodyElement);

        expect(document.body.firstChild).toBeNull();
    });

    // FIXME: Add this in once set_body is fully implemented.
    test.skip("Setting body to a new frameset element", () => {
        const newFrameSet = document.createElement("frameset");
        document.body = newFrameSet;

        expect(document.body).not.toBeNull();
        expect(document.body.nodeName).toBe("frameset");

        // FIXME: Add this in once HTMLFrameSetElement's constructor is implemented.
        //expect(document.body).toBeInstanceOf(HTMLFrameSetElement);
    });

    // FIXME: Add this in once set_body is fully implemented.
    test.skip("Setting body to an element that isn't body/frameset", () => {
        expect(() => {
            document.body = document.createElement("div");
        }).toThrow(DOMException);
    });

    // FIXME: Add this in once removeChild is implemented.
    test.skip("Nullable", () => {
       document.documentElement.removeChild(document.body);
       expect(document.body).toBeNull();
    });
});
