try {
    var j = 0;
    for (var i = 0; i < 9; ++i) {
        if (i == 3)
            continue;
        ++j;
    }
    assert(j == 8);
    console.log("PASS");
} catch {
}

