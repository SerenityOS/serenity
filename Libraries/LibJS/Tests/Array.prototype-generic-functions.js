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
        assert(Array.prototype.join.call({}) === "");
        assert(Array.prototype.join.call({ length: "foo" }) === "");
        assert(Array.prototype.join.call({ length: 3 }) === ",,");
        assert(Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar" }) === "foo,bar");
        assert(Array.prototype.join.call({ length: 2, 0: "foo", 1: "bar", 2: "baz" }) === "foo,bar");
        assert(Array.prototype.join.call({ length: 3, 1: "bar" }, "~") === "~bar~");
        assert(Array.prototype.join.call({ length: 3, 0: "foo", 1: "bar", 2: "baz" }, "~") === "foo~bar~baz");
    }

    {
        assert(Array.prototype.toString.call({}) === "[object Object]");
        assert(Array.prototype.toString.call({ join: "foo" }) === "[object Object]");
        assert(Array.prototype.toString.call({ join: () => "foo" }) === "foo");
    }

    const o = { length: 5, 0: "foo", 1: "bar", 3: "baz" };

    {
        const visited = [];
        Array.prototype.every.call(o, function (value) {
            visited.push(value);
            return true;
        });
        assert(visited.length === 3);
        assert(visited[0] === "foo");
        assert(visited[1] === "bar");
        assert(visited[2] === "baz");
    }

    ["find", "findIndex"].forEach(name => {
        const visited = [];
        Array.prototype[name].call(o, function (value) {
            visited.push(value);
            return false;
        });
        assert(visited.length === 5);
        assert(visited[0] === "foo");
        assert(visited[1] === "bar");
        assert(visited[2] === undefined);
        assert(visited[3] === "baz");
        assert(visited[4] === undefined);
    });

    ["filter", "forEach", "map", "some"].forEach(name => {
        const visited = [];
        Array.prototype[name].call(o, function (value) {
            visited.push(value);
            return false;
        });
        assert(visited.length === 3);
        assert(visited[0] === "foo");
        assert(visited[1] === "bar");
        assert(visited[2] === "baz");
    });

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
