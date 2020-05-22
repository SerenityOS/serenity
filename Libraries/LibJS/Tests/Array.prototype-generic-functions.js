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
    });

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
