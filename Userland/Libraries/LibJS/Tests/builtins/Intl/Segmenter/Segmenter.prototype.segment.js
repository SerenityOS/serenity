describe("correct behavior", () => {
    test("length is 1", () => {
        expect(Intl.Segmenter.prototype.segment).toHaveLength(1);
    });

    test("returns segments object with shared segments prototype", () => {
        const segmenter = new Intl.Segmenter();
        expect(Object.getPrototypeOf(segmenter.segment("hello"))).toBe(
            Object.getPrototypeOf(segmenter.segment("friends"))
        );
    });

    test("returns segments object segment iterator", () => {
        const segmenter = new Intl.Segmenter();
        const segments = segmenter.segment("hello friends!");
        expect(Object.getPrototypeOf(segments[Symbol.iterator]())[Symbol.toStringTag]).toBe(
            "Segmenter String Iterator"
        );
    });
});
