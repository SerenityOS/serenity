loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("Basic functionality", () => {
        const comment = document.createComment("Create Comment Test");

        // FIXME: Add this in once Comment's constructor is implemented.
        //expect(comment).toBeInstanceOf(Comment);
        expect(comment.nodeName).toBe("#comment");
        expect(comment.data).toBe("Create Comment Test");
        expect(comment.length).toBe(19);
    });
});
