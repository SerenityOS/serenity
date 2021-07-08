describe("errors", () => {
    test("called without new", () => {
        expect(() => {
            Temporal.TimeZone();
        }).toThrowWithMessage(TypeError, "Temporal.TimeZone constructor must be called with 'new'");
    });

    test("Invalid time zone name", () => {
        expect(() => {
            new Temporal.TimeZone("foo");
        }).toThrowWithMessage(RangeError, "Invalid time zone name");
    });
});

describe("normal behavior", () => {
    test("length is 1", () => {
        expect(Temporal.TimeZone).toHaveLength(1);
    });

    test("basic functionality", () => {
        const timeZone = new Temporal.TimeZone("UTC");
        expect(timeZone.id).toBe("UTC");
        expect(typeof timeZone).toBe("object");
        expect(timeZone).toBeInstanceOf(Temporal.TimeZone);
        expect(Object.getPrototypeOf(timeZone)).toBe(Temporal.TimeZone.prototype);
    });

    test("canonicalizes time zone name", () => {
        expect(new Temporal.TimeZone("Utc").id).toBe("UTC");
        expect(new Temporal.TimeZone("utc").id).toBe("UTC");
        expect(new Temporal.TimeZone("uTC").id).toBe("UTC");
    });

    // TODO: Add tests for time numeric zone offset once that's implemented
});
