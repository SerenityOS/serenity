loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        const title = document.getElementsByTagName("title")[0];
        expect(title).toBeDefined();

        // FIXME: Add this in once Text's constructor is implemented.
        //expect(title.firstChild).toBeInstanceOf(Text);

        expect(title.firstChild.nodeName).toBe("#text");
        expect(title.firstChild.data).toBe("Blank");
        expect(title.firstChild.length).toBe(5);
    });
});
