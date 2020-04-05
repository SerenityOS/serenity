try {
    var i = 0;
    var three;
    var five;

    for (; i < 9; ) {
        switch (i) {
        case 3:
            three = i;
            break;
        case 5:
            five = i;
            break;
        }
        ++i;
    }
    assert(three === 3);
    assert(five === 5);

    console.log("PASS");
} catch {
}

