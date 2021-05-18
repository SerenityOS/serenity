describe("ParentNode", () => {
    loadLocalPage("ParentNode.html");

    afterInitialPageLoad(page => {
        test("querySelector basics", () => {
            const firstDuplicateElement = page.document.querySelector(".duplicate");
            expect(firstDuplicateElement).not.toBeNull();
            expect(firstDuplicateElement.nodeName).toBe("DIV");
            expect(firstDuplicateElement.innerHTML).toBe("First");

            const noElement = page.document.querySelector(".nonexistent");
            expect(noElement).toBeNull();
        });

        test("querySelectorAll basics", () => {
            const allDuplicates = page.document.querySelectorAll(".duplicate");
            expect(allDuplicates).toHaveLength(2);
            expect(allDuplicates[0].nodeName).toBe("DIV");
            expect(allDuplicates[0].innerHTML).toBe("First");
            expect(allDuplicates[1].nodeName).toBe("DIV");
            expect(allDuplicates[1].innerHTML).toBe("Second");

            const noElements = page.document.querySelectorAll(".nonexistent");
            expect(noElements).toHaveLength(0);
        });
    });
    waitForPageToLoad();
});
