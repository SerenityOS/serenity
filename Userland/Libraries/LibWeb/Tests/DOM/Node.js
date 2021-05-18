describe("Node", () => {
    loadLocalPage("/res/html/misc/innertext_textcontent.html");

    afterInitialPageLoad(page => {
        test("Node.textContent", () => {
            var p = page.document.getElementsByTagName("p")[0];
            expect(p.textContent).toBe("This is a very small test page :^)");
            expect(p.firstChild.textContent).toBe("This is a ");
            expect(p.firstChild.firstChild).toBe(null);

            p.firstChild.textContent = "foo";
            expect(p.firstChild.textContent).toBe("foo");
            expect(p.firstChild.firstChild).toBe(null);
            expect(p.textContent).toBe("foovery small test page :^)");

            p.textContent = "bar";
            expect(p.textContent).toBe("bar");
            expect(p.firstChild.textContent).toBe("bar");
            expect(p.firstChild.firstChild).toBe(null);

            var p = page.document.getElementById("source");
            expect(p.textContent).toBe(`
            #source { color: red;  } #text { text-transform: uppercase; }
            Take   a look athow this textis interpreted
                   below.
            HIDDEN TEXT
        `);
        });

        test("Node.isConnected", () => {
            var element = page.document.createElement("p");
            expect(element.isConnected).toBeFalse();

            page.document.body.appendChild(element);
            expect(element.isConnected).toBeTrue();

            page.document.body.removeChild(element);
            expect(element.isConnected).toBeFalse();
        });

        test("Node.compareDocumentPosition()", () => {
            const head = page.document.head;
            const body = page.document.body;

            expect(head.compareDocumentPosition(head)).toBe(0);

            // FIXME: Can be uncommented once the IDL parser correctly implements nullable parameters.
            // expect(head.compareDocumentPosition(null) & Node.DOCUMENT_POSITION_DISCONNECTED | Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC).
            //    toBe(Node.DOCUMENT_POSITION_DISCONNECTED | Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);

            expect(head.compareDocumentPosition(body)).toBe(page.Node.DOCUMENT_POSITION_FOLLOWING);
            expect(body.compareDocumentPosition(head)).toBe(page.Node.DOCUMENT_POSITION_PRECEDING);

            const source = page.document.getElementById("source");
            expect(source.compareDocumentPosition(body)).toBe(
                page.Node.DOCUMENT_POSITION_CONTAINS | page.Node.DOCUMENT_POSITION_PRECEDING
            );
            expect(body.compareDocumentPosition(source)).toBe(
                page.Node.DOCUMENT_POSITION_CONTAINED_BY | page.Node.DOCUMENT_POSITION_FOLLOWING
            );
            expect(source.compareDocumentPosition(head)).toBe(
                page.Node.DOCUMENT_POSITION_PRECEDING
            );
        });
    });

    waitForPageToLoad();
});
