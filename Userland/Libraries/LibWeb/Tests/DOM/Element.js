describe("Element", () => {
    loadLocalPage("/res/html/misc/innertext_textcontent.html");

    afterInitialPageLoad(page => {
        test("Element.innerText", () => {
            var p = page.document.getElementsByTagName("p")[0];
            expect(p.innerText).toBe("This is a very small test page :^)");

            // FIXME: Call this on p once that's supported.
            var b = page.document.getElementsByTagName("b")[0];
            b.innerText = "foo";
            expect(b.innerText).toBe("foo");
            expect(p.innerText).toBe("This is a foo test page :^)");

            p.innerText = "bar";
            expect(p.innerText).toBe("bar");

            var p = page.document.getElementById("source");
            // FIXME: The leading and trailing two spaces each are wrong.
            // FIXME: The text should be affected by the text-transform:uppercase.
            expect(p.innerText).toBe(`  Take a look at
how this text
is interpreted below.  `);
        });

        test("Element.namespaceURI basics", () => {
            const htmlNamespace = "http://www.w3.org/1999/xhtml";
            const p = page.document.getElementsByTagName("p")[0];
            expect(p.namespaceURI).toBe(htmlNamespace);

            // createElement always sets the namespace to the HTML namespace in HTML page.documents.
            const svgElement = page.document.createElement("svg");
            expect(svgElement.namespaceURI).toBe(htmlNamespace);

            const svgNamespace = "http://www.w3.org/2000/svg";
            p.innerHTML = "<svg></svg>";
            const domSVGElement = p.getElementsByTagName("svg")[0];
            expect(domSVGElement.namespaceURI).toBe(svgNamespace);
        });
    });
    waitForPageToLoad();
});
