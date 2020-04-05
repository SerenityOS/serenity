try {
    i < 3;
} catch (e) {
    assert(e.name === "ReferenceError");
}

console.log("PASS");
