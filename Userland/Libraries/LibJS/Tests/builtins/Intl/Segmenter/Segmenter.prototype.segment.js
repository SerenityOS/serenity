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

    test("returns segments object with valid containing method", () => {
        const string = "hello friends!";
        const graphemeSegmenter = new Intl.Segmenter("en", { granularity: "grapheme" });
        const graphemeSegments = graphemeSegmenter.segment(string);
        const graphemeSegment0 = graphemeSegments.containing(0);
        expect(graphemeSegment0.segment).toBe("h");
        expect(graphemeSegment0.index).toBe(0);
        expect(graphemeSegment0.input).toBe(string);
        expect(graphemeSegment0.isWordLike).toBeUndefined();
        const graphemeSegment5 = graphemeSegments.containing(5);
        expect(graphemeSegment5.segment).toBe(" ");
        expect(graphemeSegment5.index).toBe(5);
        expect(graphemeSegment5.input).toBe(string);
        expect(graphemeSegment5.isWordLike).toBeUndefined();

        const wordSegmenter = new Intl.Segmenter("en", { granularity: "word" });
        const wordSegments = wordSegmenter.segment(string);
        const wordSegment0 = wordSegments.containing(0);
        expect(wordSegment0.segment).toBe("hello");
        expect(wordSegment0.index).toBe(0);
        expect(wordSegment0.input).toBe(string);
        // FIXME: expect(wordSegment0.isWordLike).toBeTrue();
        const wordSegment5 = wordSegments.containing(5);
        expect(wordSegment5.segment).toBe(" ");
        expect(wordSegment5.index).toBe(5);
        expect(wordSegment5.input).toBe(string);
        expect(wordSegment5.isWordLike).toBeFalse();

        const sentenceSegmenter = new Intl.Segmenter("en", { granularity: "sentence" });
        const sentenceSegments = sentenceSegmenter.segment(string);
        const sentenceSegment0 = sentenceSegments.containing(0);
        expect(sentenceSegment0.segment).toBe(string);
        expect(sentenceSegment0.index).toBe(0);
        expect(sentenceSegment0.input).toBe(string);
        expect(sentenceSegment0.isWordLike).toBeUndefined();
        const sentenceSegment5 = sentenceSegments.containing(5);
        expect(sentenceSegment5.segment).toBe(sentenceSegment0.segment);
        expect(sentenceSegment5.index).toBe(sentenceSegment0.index);
        expect(sentenceSegment5.input).toBe(sentenceSegment0.input);
        expect(sentenceSegment5.isWordLike).toBe(sentenceSegment0.isWordLike);
    });

    test("returns segments object segment iterator", () => {
        const string = "hello friends!";
        const segmenter = new Intl.Segmenter();
        const segments = segmenter.segment(string);
        expect(Object.getPrototypeOf(segments[Symbol.iterator]())[Symbol.toStringTag]).toBe(
            "Segmenter String Iterator"
        );

        const graphemeSegmenter = new Intl.Segmenter("en", { granularity: "grapheme" });
        const graphemeSegments = graphemeSegmenter.segment(string);
        let index = 0;
        for (const segment of graphemeSegments) {
            expect(segment.segment).toBe(string[index]);
            expect(segment.index).toBe(index);
            expect(segment.input).toBe(string);
            expect(segment.isWordLike).toBeUndefined();
            index++;
        }
        expect(index).toBe(string.length);

        const wordSegmenter = new Intl.Segmenter("en", { granularity: "word" });
        const wordSegments = wordSegmenter.segment(string);
        const expectedSegments = [
            { segment: "hello", index: 0, isWordLike: true },
            { segment: " ", index: 5, isWordLike: false },
            { segment: "friends", index: 6, isWordLike: true },
            { segment: "!", index: 13, isWordLike: false },
        ];
        index = 0;
        for (const segment of wordSegments) {
            console.log(JSON.stringify(segment));
            expect(segment.segment).toBe(expectedSegments[index].segment);
            expect(segment.index).toBe(expectedSegments[index].index);
            expect(segment.input).toBe(string);
            // FIXME: expect(segment.isWordLike).toBe(expectedSegments[index].isWordLike);
            index++;
        }
        expect(index).toBe(expectedSegments.length);

        const sentenceSegmenter = new Intl.Segmenter("en", { granularity: "sentence" });
        const sentenceSegments = sentenceSegmenter.segment(string);
        index = 0;
        for (const segment of sentenceSegments) {
            expect(segment.segment).toBe(string);
            expect(segment.index).toBe(0);
            expect(segment.input).toBe(string);
            expect(segment.isWordLike).toBeUndefined();
            index++;
        }
        expect(index).toBe(1);
    });
});
