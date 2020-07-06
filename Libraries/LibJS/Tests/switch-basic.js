describe("basic switch tests", () => {
    test("string case does not match number target", () => {
        switch (1 + 2) {
            case "3":
                expect().fail();
            case 3:
                return;
            case 5:
            case 1:
                break;
            default:
                break;
        }

        expect().fail();
    });

    test("string concatenation in target", () => {
        var a = "foo";

        switch (a + "bar") {
            case 1:
                expect().fail();
            case "foobar":
            case 2:
                return;
        }
        expect().fail();
    });

    test("default", () => {
        switch (100) {
            default:
                return;
        }

        expect().fail();
    });
});
