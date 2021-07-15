test("invariants", () => {
    expect(String.prototype.matchAll).toHaveLength(1);
});

test("error cases", () => {
    [null, undefined].forEach(value => {
        expect(() => {
            value.matchAll("");
        }).toThrow(TypeError);
    });

    expect(() => {
        "hello friends".matchAll(/hello/);
    }).toThrow(TypeError);
});

test("basic functionality", () => {
    expect("hello friends".matchAll(/hello/g)).not.toBeNull();
    expect("hello friends".matchAll(/enemies/g)).not.toBeNull();

    {
        var iterator = "".matchAll(/a/g);

        var next = iterator.next();
        expect(next.done).toBeTrue();
        expect(next.value).toBeUndefined();

        next = iterator.next();
        expect(next.done).toBeTrue();
        expect(next.value).toBeUndefined();
    }
    {
        var iterator = "a".matchAll(/a/g);

        var next = iterator.next();
        expect(next.done).toBeFalse();
        expect(next.value).toEqual(["a"]);
        expect(next.value.index).toBe(0);

        next = iterator.next();
        expect(next.done).toBeTrue();
        expect(next.value).toBeUndefined();
    }
    {
        var iterator = "aa".matchAll(/a/g);

        var next = iterator.next();
        expect(next.done).toBeFalse();
        expect(next.value).toEqual(["a"]);
        expect(next.value.index).toBe(0);

        next = iterator.next();
        expect(next.done).toBeFalse();
        expect(next.value).toEqual(["a"]);
        expect(next.value.index).toBe(1);

        next = iterator.next();
        expect(next.done).toBeTrue();
        expect(next.value).toBeUndefined();
    }
    {
        var iterator = "aba".matchAll(/a/g);

        var next = iterator.next();
        expect(next.done).toBeFalse();
        expect(next.value).toEqual(["a"]);
        expect(next.value.index).toBe(0);

        next = iterator.next();
        expect(next.done).toBeFalse();
        expect(next.value).toEqual(["a"]);
        expect(next.value.index).toBe(2);

        next = iterator.next();
        expect(next.done).toBeTrue();
        expect(next.value).toBeUndefined();
    }
});
