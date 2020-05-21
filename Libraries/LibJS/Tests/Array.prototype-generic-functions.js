load("test-common.js");

try {
    const o = { length: 5, 0: "foo", 1: "bar", 3: "baz" };

    ["every"].forEach(name => {
        const visited = [];
        Array.prototype[name].call(o, function (value) {
            visited.push(value);
            return true;
        });
        assert(visited.length === 3);
        assert(visited[0] === "foo");
        assert(visited[1] === "bar");
        assert(visited[2] === "baz");
    });

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
