loadPage("file:///res/html/misc/innertext_textcontent.html");

afterInitialPageLoad(() => {
    test("Node.textContent", () => {
        var p = document.getElementsByTagName("p")[0];
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

        var p = document.getElementById("source");
        expect(p.textContent).toBe(`
            #source { color: red;  } #text { text-transform: uppercase; }
            Take   a look athow this textis interpreted
                   below.
            HIDDEN TEXT
        `);
    });

    test("Node.isConnected", () => {
        var element = document.createElement("p");
        expect(element.isConnected).toBeFalse();

        document.body.appendChild(element);
        expect(element.isConnected).toBeTrue();

        document.body.removeChild(element);
        expect(element.isConnected).toBeFalse();
    });

    test("Node.compareDocumentPosition()", () => {
        const head = document.head;
        const body = document.body;

        expect(head.compareDocumentPosition(head)).toBe(0);

        // FIXME: Can be uncommented once the IDL parser correctly implements nullable parameters.
        // expect(head.compareDocumentPosition(null) & Node.DOCUMENT_POSITION_DISCONNECTED | Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC).
        //    toBe(Node.DOCUMENT_POSITION_DISCONNECTED | Node.DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);

        expect(head.compareDocumentPosition(body)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
        expect(body.compareDocumentPosition(head)).toBe(Node.DOCUMENT_POSITION_PRECEDING);

        const source = document.getElementById("source");
        expect(source.compareDocumentPosition(body)).toBe(
            Node.DOCUMENT_POSITION_CONTAINS | Node.DOCUMENT_POSITION_PRECEDING
        );
        expect(body.compareDocumentPosition(source)).toBe(
            Node.DOCUMENT_POSITION_CONTAINED_BY | Node.DOCUMENT_POSITION_FOLLOWING
        );
        expect(source.compareDocumentPosition(head)).toBe(Node.DOCUMENT_POSITION_PRECEDING);
    });
});
