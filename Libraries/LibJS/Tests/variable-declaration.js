try {

    const ConstantValue = 1;
    try {
        ConstantValue = 2;
    } catch (e) { 
        assert(e.name === "TypeError");
        assert(e.message === "Assignment to constant variable");
        assert(ConstantValue === 1);
    }

    // Make sure we can define new constants in inner scopes.
    //
    const ConstantValue2 = 1;

    do 
    {
        const ConstantValue2 = 2;
    }
    while (false)

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
