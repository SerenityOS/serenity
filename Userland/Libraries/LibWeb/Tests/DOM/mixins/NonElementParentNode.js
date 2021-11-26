describe("NonElementParentNode", () => {
    loadLocalPage("ParentNode.html");

    afterInitialPageLoad(page => {
        test("getElementById basics", () => {
            const unique = page.document.getElementById("unique");
            expect(unique).not.toBeNull();
            expect(unique.nodeName).toBe("DIV");
            expect(unique.id).toBe("unique");

            const caseSensitive = page.document.getElementById("Unique");
            expect(caseSensitive).toBeNull();

            const firstDuplicate = page.document.getElementById("dupeId");
            expect(firstDuplicate).not.toBeNull();
            expect(firstDuplicate.nodeName).toBe("DIV");
            expect(firstDuplicate.id).toBe("dupeId");
            expect(firstDuplicate.innerHTML).toBe("First ID");
        });
    });
    waitForPageToLoad();
});
