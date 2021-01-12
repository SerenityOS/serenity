loadPage("file:///home/anon/web-tests/Pages/ParentNode.html");

afterInitialPageLoad(() => {
    test("getElementById basics", () => {
        const unique = document.getElementById("unique");
        expect(unique).not.toBeNull();
        expect(unique.nodeName).toBe("div");
        expect(unique.id).toBe("unique");

        const caseSensitive = document.getElementById("Unique");
        expect(caseSensitive).toBeNull();

        const firstDuplicate = document.getElementById("dupeId");
        expect(firstDuplicate).not.toBeNull();
        expect(firstDuplicate.nodeName).toBe("div");
        expect(firstDuplicate.id).toBe("dupeId");
        expect(firstDuplicate.innerHTML).toBe("First ID");
    });
});
