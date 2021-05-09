loadPage("file:///home/anon/web-tests/Pages/ParentNode.html");

afterInitialPageLoad(() => {
    test("querySelector basics", () => {
        const firstDuplicateElement = document.querySelector(".duplicate");
        expect(firstDuplicateElement).not.toBeNull();
        expect(firstDuplicateElement.nodeName).toBe("DIV");
        expect(firstDuplicateElement.innerHTML).toBe("First");

        const noElement = document.querySelector(".nonexistent");
        expect(noElement).toBeNull();
    });

    test("querySelectorAll basics", () => {
        const allDuplicates = document.querySelectorAll(".duplicate");
        expect(allDuplicates).toHaveLength(2);
        expect(allDuplicates[0].nodeName).toBe("DIV");
        expect(allDuplicates[0].innerHTML).toBe("First");
        expect(allDuplicates[1].nodeName).toBe("DIV");
        expect(allDuplicates[1].innerHTML).toBe("Second");

        const noElements = document.querySelectorAll(".nonexistent");
        expect(noElements).toHaveLength(0);
    });
});
