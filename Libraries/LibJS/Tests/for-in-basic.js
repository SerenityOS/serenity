load("test-common.js");

try {
    assertVisitsAll(visit => {
        for (const property in "") {
            visit(property);
        }
    }, []);

    assertVisitsAll(visit => {
        for (const property in 123) {
            visit(property);
        }
    }, []);

    assertVisitsAll(visit => {
        for (const property in {}) {
            visit(property);
        }
    }, []);

    assertVisitsAll(visit => {
        for (const property in "hello") {
            visit(property);
        }
    }, ["0", "1", "2", "3", "4"]);

    assertVisitsAll(visit => {
        for (const property in {a: 1, b: 2, c: 2}) {
            visit(property);
        }
    }, ["a", "b", "c"]);

    var property;
    for (property in "abc");
    assert(property === "2");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
