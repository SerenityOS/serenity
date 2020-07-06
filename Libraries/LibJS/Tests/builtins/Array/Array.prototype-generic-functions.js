load("test-common.js");

try {
    [undefined, "foo", -42, 0].forEach(length => {
        const o = { length };

        assert(Array.prototype.push.call(o, "foo") === 1);
        assert(o.length === 1);
        assert(o[0] === "foo");
        assert(Array.prototype.push.call(o, "bar", "baz") === 3);
        assert(o.length === 3);
        assert(o[0] === "foo");
        assert(o[1] === "bar");
        assert(o[2] === "baz");

        assert(Array.prototype.pop.call(o) === "baz");
        assert(o.length === 2);
        assert(Array.prototype.pop.call(o) === "bar");
        assert(o.length === 1);
        assert(Array.prototype.pop.call(o) === "foo");
        assert(o.length === 0);
        assert(Array.prototype.pop.call(o) === undefined);
        assert(o.length === 0);

        o.length = length;
        assert(Array.prototype.pop.call(o) === undefined);
        assert(o.length === 0);
    });

    {
        const o = { length: 3, 0: "hello", 2: "serenity" };
        const removed = Array.prototype.splice.call(o, 0, 2, "hello", "friends");
        assert(o.length === 3);
        assert(o[0] === "hello");
        assert(o[1] === "friends");
        assert(o[2] === "serenity");
        assert(removed.length === 2);
        assert(removed[0] === "hello");
        assert(removed[1] === undefined);
    }

    {
        assert(Array.prototype.join.call({}) === "");
        assert(Array.prototype.join.call({ length: "foo" }) === "");
        assert(Array.prototype.join.call({ length: 3 }) === ",,");
        assert(Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar" }) === "foo,bar");
        assert(
            Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar", 2: "baz" }) === "foo,bar"
        );
        assert(Array.prototype.join.call({ length: 3, 1: "bar" }, "~") === "~bar~");
        assert(
            Array.prototype.join.call({ length: 3, 0: "foo", 1: "bar", 2: "baz" }, "~") ===
                "foo~bar~baz"
        );
    }

    {
        assert(Array.prototype.toString.call({}) === "[object Object]");
        assert(Array.prototype.toString.call({ join: "foo" }) === "[object Object]");
        assert(Array.prototype.toString.call({ join: () => "foo" }) === "foo");
    }

    {
        assert(Array.prototype.indexOf.call({}) === -1);
        assert(Array.prototype.indexOf.call({ 0: undefined }) === -1);
        assert(Array.prototype.indexOf.call({ length: 1, 0: undefined }) === 0);
        assert(Array.prototype.indexOf.call({ length: 1, 2: "foo" }, "foo") === -1);
        assert(Array.prototype.indexOf.call({ length: 5, 2: "foo" }, "foo") === 2);
        assert(Array.prototype.indexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo", 3) === 4);
    }

    {
        assert(Array.prototype.lastIndexOf.call({}) === -1);
        assert(Array.prototype.lastIndexOf.call({ 0: undefined }) === -1);
        assert(Array.prototype.lastIndexOf.call({ length: 1, 0: undefined }) === 0);
        assert(Array.prototype.lastIndexOf.call({ length: 1, 2: "foo" }, "foo") === -1);
        assert(Array.prototype.lastIndexOf.call({ length: 5, 2: "foo" }, "foo") === 2);
        assert(Array.prototype.lastIndexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo") === 4);
        assert(
            Array.prototype.lastIndexOf.call({ length: 5, 2: "foo", 4: "foo" }, "foo", -2) === 2
        );
    }

    {
        assert(Array.prototype.includes.call({}) === false);
        assert(Array.prototype.includes.call({ 0: undefined }) === false);
        assert(Array.prototype.includes.call({ length: 1, 0: undefined }) === true);
        assert(Array.prototype.includes.call({ length: 1, 2: "foo" }, "foo") === false);
        assert(Array.prototype.includes.call({ length: 5, 2: "foo" }, "foo") === true);
    }

    const o = { length: 5, 0: "foo", 1: "bar", 3: "baz" };

    {
        assertVisitsAll(
            visit => {
                Array.prototype.every.call(o, function (value) {
                    visit(value);
                    return true;
                });
            },
            ["foo", "bar", "baz"]
        );
    }

    ["find", "findIndex"].forEach(name => {
        assertVisitsAll(
            visit => {
                Array.prototype[name].call(o, function (value) {
                    visit(value);
                    return false;
                });
            },
            ["foo", "bar", undefined, "baz", undefined]
        );
    });

    ["filter", "forEach", "map", "some"].forEach(name => {
        assertVisitsAll(
            visit => {
                Array.prototype[name].call(o, function (value) {
                    visit(value);
                    return false;
                });
            },
            ["foo", "bar", "baz"]
        );
    });
    {
        assertVisitsAll(
            visit => {
                Array.prototype.reduce.call(
                    o,
                    function (_, value) {
                        visit(value);
                        return false;
                    },
                    "initial"
                );
            },
            ["foo", "bar", "baz"]
        );
    }

    {
        assertVisitsAll(
            visit => {
                Array.prototype.reduceRight.call(
                    o,
                    function (_, value) {
                        visit(value);
                        return false;
                    },
                    "initial"
                );
            },
            ["baz", "bar", "foo"]
        );
    }

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
