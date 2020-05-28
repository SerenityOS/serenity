load("test-common.js");

try {
    test: {
        let o = 1;
        assert(o === 1);
        break test;
        assertNotReached();
    }

    outer: {
        {
            break outer;
        }
        assertNotReached();
    }

    let counter = 0;
    outer:
    for (a of [1, 2, 3]) {
        for (b of [4, 5, 6]) {
            if (a === 2 && b === 5)
                break outer;
            counter++;
        }
    }
    assert(counter === 4);

    let counter = 0;
    outer:
    for (a of [1, 2, 3]) {
        for (b of [4, 5, 6]) {
            if (b === 6)
                continue outer;
            counter++;
        }
    }
    assert(counter === 6);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
