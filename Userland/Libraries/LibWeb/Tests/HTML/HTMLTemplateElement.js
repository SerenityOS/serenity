describe("HTMLTemplateElement", () => {
    loadLocalPage("Template.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const template = page.document.getElementById("template");
            expect(template).not.toBeNull();

            // The contents of a template element are not children of the actual element.
            // The document fragment is not a child of the element either.
            expect(template.firstChild).toBeNull();

            // FIXME: Add this in once page.DocumentFragment's constructor is implemented.
            //expect(template.content).toBeInstanceOf(page.DocumentFragment);
            expect(template.content.nodeName).toBe("#document-fragment");

            const templateDiv = template.content.getElementById("templatediv");
            expect(templateDiv.nodeName).toBe("DIV");
            expect(templateDiv.textContent).toBe("Hello template!");
        });

        test("Templates are inert (e.g. scripts won't run)", () => {
            // The page has a template element with a script element in it.
            // Templates are inert, for example, they won't run scripts.
            // That script will set "templateScriptRan" to true if it ran.
            expect(page.window.templateScriptRan).toBeUndefined();
        });
    });
    waitForPageToLoad();
});
