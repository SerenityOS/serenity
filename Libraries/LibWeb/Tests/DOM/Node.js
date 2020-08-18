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
});
