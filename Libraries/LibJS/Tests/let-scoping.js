load("test-common.js");

try {

    let i = 1;
    {
        let i = 3;
        assert(i === 3);
    }

    assert(i === 1);

    {
        const i = 2;
        assert(i === 2);
    }

    assert(i === 1);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
