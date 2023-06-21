describe("Comments", () => {
    loadLocalPage("Comment.html");
    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const comment = page.document.body.firstChild.nextSibling;
            expect(comment).not.toBeNull();

            // FIXME: Add this in once Comment's constructor is implemented.
            //expect(comment).toBeInstanceOf(Comment);

            expect(comment.nodeName).toBe("#comment");
            expect(comment.data).toBe("This is a comment");
            expect(comment).toHaveLength(17);
        });
    });
    waitForPageToLoad();
});
