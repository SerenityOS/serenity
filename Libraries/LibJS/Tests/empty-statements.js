try {
    ;;;
    if (true);
    if (false); else if (false); else;
    while (false);
    do; while (false);

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
