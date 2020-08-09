loadPage("file:///res/html/misc/blank.html");

afterInitialPageLoad(() => {
    test("atob", () => {
        expect(atob("YQ==")).toBe("a");
        expect(atob("YWE=")).toBe("aa");
        expect(atob("YWFh")).toBe("aaa");
        expect(atob("YWFhYQ==")).toBe("aaaa");
        expect(atob("/w==")).toBe("\xff");
    });

    test("btoa", () => {
        expect(btoa("a")).toBe("YQ==");
        expect(btoa("aa")).toBe("YWE=");
        expect(btoa("aaa")).toBe("YWFh");
        expect(btoa("aaaa")).toBe("YWFhYQ==");
        expect(btoa("\xff")).toBe("/w==");
    });
});
