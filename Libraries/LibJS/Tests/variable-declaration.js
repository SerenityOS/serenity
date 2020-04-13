try {

    const constantValue = 1;
    try {
        constantValue = 2;
        assertNotReached();
    } catch (e) {
        assert(e.name === "TypeError");
        assert(e.message === "Assignment to constant variable");
        assert(constantValue === 1);
    }

    // Make sure we can define new constants in inner scopes.
    const constantValue2 = 1;
    do {
        const constantValue2 = 2;
        assert(constantValue2 === 2);
    } while (false);
    assert(constantValue2 === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
