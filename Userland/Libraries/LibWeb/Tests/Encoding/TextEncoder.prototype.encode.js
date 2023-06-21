describe("normal behavior", () => {
    loadLocalPage("/res/html/misc/blank.html");

    afterInitialPageLoad(page => {
        test("Basic functionality", () => {
            const textEncoder = new page.TextEncoder();

            {
                const typedArray = textEncoder.encode("");
                expect(typedArray).toHaveLength(0);
            }

            {
                const typedArray = textEncoder.encode("abc");
                expect(typedArray).toHaveLength(3);
                expect(Array.from(typedArray)).toEqual([97, 98, 99]);
            }

            {
                const typedArray = textEncoder.encode("€");
                expect(typedArray).toHaveLength(3);
                expect(Array.from(typedArray)).toEqual([226, 130, 172]);
                // [255, 254, 172, 32] in UTF-16, but TextEncoder always converts JS UTF-16 strings to UTF-8
            }
        });
    });
    waitForPageToLoad();
});
