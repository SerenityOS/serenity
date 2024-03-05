test("Don't fuse unrelated jump and compare", () => {
    function go(a) {
        a < 3;
        a &&= 1;

        a < 3;
        a ||= 1;
    }
    go();
});
