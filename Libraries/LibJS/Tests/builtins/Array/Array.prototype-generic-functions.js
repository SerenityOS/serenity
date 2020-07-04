describe("ability to work with generic non-array objects", () => {
    test("push, pop", () => {
        [undefined, "foo", -42, 0].forEach(length => {
            const o = { length };

            expect(Array.prototype.push.call(o, "foo")).toBe(1);
            expect(o).toHaveLength(1);
            expect(o[0]).toBe("foo");
            expect(Array.prototype.push.call(o, "bar", "baz")).toBe(3);
            expect(o).toHaveLength(3);
            expect(o[0]).toBe("foo");
            expect(o[1]).toBe("bar");
            expect(o[2]).toBe("baz");

            expect(Array.prototype.pop.call(o)).toBe("baz");
            expect(o).toHaveLength(2);
            expect(Array.prototype.pop.call(o)).toBe("bar");
            expect(o).toHaveLength(1);
            expect(Array.prototype.pop.call(o)).toBe("foo");
            expect(o).toHaveLength(0);
            expect(Array.prototype.pop.call(o)).toBeUndefined();
            expect(o).toHaveLength(0);

            o.length = length;
            expect(Array.prototype.pop.call(o)).toBeUndefined();
            expect(o).toHaveLength(0);
        });
    });

    test("splice", () => {
        const o = { length: 3, 0: "hello", 2: "serenity" };
        const removed = Array.prototype.splice.call(o, 0, 2, "hello", "friends");
        expect(o).toHaveLength(3);
        expect(o[0]).toBe("hello");
        expect(o[1]).toBe("friends");
        expect(o[2]).toBe("serenity");
        expect(removed).toHaveLength(2);
        expect(removed[0]).toBe("hello");
        expect(removed[1]).toBeUndefined();
    });

    test("join", () => {
        expect(Array.prototype.join.call({})).toBe("");
        expect(Array.prototype.join.call({ length: "foo" })).toBe("");
        expect(Array.prototype.join.call({ length: 3 })).toBe(",,");
        expect(Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar" })).toBe("foo,bar");
        expect(Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar", 2: "baz" })).toBe(
            "foo,bar"
        );
        expect(Array.prototype.join.call({ length: 3, 1: "bar" }, "~")).toBe("~bar~");
        expect(Array.prototype.join.call({ length: 3, 0: "foo", 1: "bar", 2: "baz" }, "~")).toBe(
            "foo~bar~baz"
        );
    });

    // FIXME: test-js asserts when this is just called "toString" ಠ_ಠ
    test("toString (FIXME)", () => {
        expect(Array.prototype.toString.call({})).toBe("[object Object]");
        expect(Array.prototype.toString.call({ join: "foo" })).toBe("[object Object]");
        expect(Array.prototype.toString.call({ join: () => "foo" })).toBe("foo");
    });

    test("indexOf", () => {
        expect(Array.prototype.indexOf.call({})).toBe(-1);
        expect(Array.prototype.indexOf.call({ 0: undefined })).toBe(-1);
        expect(Array.prototype.indexOf.call({ length: 1, 0: undefined })).toBe(0);
        expect(Array.prototype.indexOf.call({ length: 1, 2: "foo" }, "foo")).toBe(-1);
        expect(Array.prototype.indexOf.call({ length: 5, 2: "foo" }, "foo")).toBe(2);
        expect(Array.prototype.indexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo", 3)).toBe(4);
    });

    test("lastIndexOf", () => {
        expect(Array.prototype.lastIndexOf.call({})).toBe(-1);
        expect(Array.prototype.lastIndexOf.call({ 0: undefined })).toBe(-1);
        expect(Array.prototype.lastIndexOf.call({ length: 1, 0: undefined })).toBe(0);
        expect(Array.prototype.lastIndexOf.call({ length: 1, 2: "foo" }, "foo")).toBe(-1);
        expect(Array.prototype.lastIndexOf.call({ length: 5, 2: "foo" }, "foo")).toBe(2);
        expect(Array.prototype.lastIndexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo")).toBe(4);
        expect(Array.prototype.lastIndexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo", -2)).toBe(
            2
        );
    });

    test("includes", () => {
        expect(Array.prototype.includes.call({})).toBeFalse();
        expect(Array.prototype.includes.call({ 0: undefined })).toBeFalse();
        expect(Array.prototype.includes.call({ length: 1, 0: undefined })).toBeTrue();
        expect(Array.prototype.includes.call({ length: 1, 2: "foo" }, "foo")).toBeFalse();
        expect(Array.prototype.includes.call({ length: 5, 2: "foo" }, "foo")).toBeTrue();
    });

    const o = { length: 5, 0: "foo", 1: "bar", 3: "baz" };

    test("every", () => {
        const visited = [];
        Array.prototype.every.call(o, value => {
            visited.push(value);
            return true;
        });
        expect(visited).toEqual(["foo", "bar", "baz"]);
    });

    test("find, findIndex", () => {
        ["find", "findIndex"].forEach(name => {
            const visited = [];
            Array.prototype[name].call(o, value => {
                visited.push(value);
                return false;
            });
            expect(visited).toEqual(["foo", "bar", undefined, "baz", undefined]);
        });
    });

    test("filter, forEach, map, some", () => {
        ["filter", "forEach", "map", "some"].forEach(name => {
            const visited = [];
            Array.prototype[name].call(o, value => {
                visited.push(value);
                return false;
            });
            expect(visited).toEqual(["foo", "bar", "baz"]);
        });
    });

    test("reduce", () => {
        const visited = [];
        Array.prototype.reduce.call(
            o,
            (_, value) => {
                visited.push(value);
                return false;
            },
            "initial"
        );
        expect(visited).toEqual(["foo", "bar", "baz"]);
    });

    test("reduceRight", () => {
        const visited = [];
        Array.prototype.reduceRight.call(
            o,
            (_, value) => {
                visited.push(value);
                return false;
            },
            "initial"
        );
        expect(visited).toEqual(["baz", "bar", "foo"]);
    });
});
