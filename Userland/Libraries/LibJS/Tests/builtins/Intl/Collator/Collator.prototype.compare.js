describe("correct behavior", () => {
    test("length is 2", () => {
        expect(new Intl.Collator().compare).toHaveLength(2);
    });

    test("name is empty string", () => {
        expect(new Intl.Collator().compare.name).toBe("");
    });

    test("basic functionality", () => {
        const collator = new Intl.Collator();
        expect(collator.compare("", "")).toBe(0);
        expect(collator.compare("a", "a")).toBe(0);
        expect(collator.compare("6", "6")).toBe(0);

        function compareBoth(a, b) {
            const aTob = collator.compare(a, b);
            const bToa = collator.compare(b, a);

            expect(aTob > 0).toBeTrue();
            expect(aTob).toBe(-bToa);
        }

        compareBoth("a", "");
        compareBoth("1", "");
        compareBoth("a", "A");
        compareBoth("7", "3");
        compareBoth("0000", "0");

        expect(collator.compare("undefined")).toBe(0);
        expect(collator.compare("undefined", undefined)).toBe(0);

        expect(collator.compare("null", null)).toBe(0);
        expect(collator.compare("null", undefined)).not.toBe(0);
        expect(collator.compare("null") < 0).toBeTrue();
    });

    test("UTF-16", () => {
        const collator = new Intl.Collator();
        const string = "😀😀";
        expect(collator.compare(string, "😀😀")).toBe(0);
        expect(collator.compare(string, "\ud83d") > 0);
        expect(collator.compare(string, "😀😀s") < 0);
    });
});
