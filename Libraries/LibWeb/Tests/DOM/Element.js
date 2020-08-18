loadPage("file:///res/html/misc/innertext_textcontent.html");

afterInitialPageLoad(() => {
    test("Element.innerText", () => {
        var p = document.getElementsByTagName("p")[0];
        expect(p.innerText).toBe("This is a very small test page :^)");

        // FIXME: Call this on p once that's supported.
        var b = document.getElementsByTagName("b")[0];
        b.innerText = "foo";
        expect(b.innerText).toBe("foo");
        expect(p.innerText).toBe("This is a foo test page :^)");

        p.innerText = "bar";
        expect(p.innerText).toBe("bar");

        var p = document.getElementById("source");
        // FIXME: The leading and trailing two spaces each are wrong.
        // FIXME: The text should be affected by the text-transform:uppercase.
        expect(p.innerText).toBe(`  Take a look at
how this text
is interpreted below.  `);
    });
});
