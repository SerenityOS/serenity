describe("Base64", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("atob", () => {
            expect(page.atob("YQ==")).toBe("a");
            expect(page.atob("YWE=")).toBe("aa");
            expect(page.atob("YWFh")).toBe("aaa");
            expect(page.atob("YWFhYQ==")).toBe("aaaa");
            expect(page.atob("/w==")).toBe("\xff");
        });

        test("btoa", () => {
            expect(page.btoa("a")).toBe("YQ==");
            expect(page.btoa("aa")).toBe("YWE=");
            expect(page.btoa("aaa")).toBe("YWFh");
            expect(page.btoa("aaaa")).toBe("YWFhYQ==");
            expect(page.btoa("\xff")).toBe("/w==");
        });
    });
    waitForPageToLoad();
});
