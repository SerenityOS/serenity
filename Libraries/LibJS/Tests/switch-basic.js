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

    test("return from switch statement", () => {
        function foo(value) {
            switch (value) {
                case 42:
                    return "return from 'case 42'";
                default:
                    return "return from 'default'";
            }
        }
        expect(foo(42)).toBe("return from 'case 42'");
        expect(foo(43)).toBe("return from 'default'");
    });

    test("continue from switch statement", () => {
        let i = 0;
        for (; i < 5; ++i) {
            switch (i) {
                case 0:
                    continue;
                    expect().fail();
                case 0:
                    expect().fail();
                default:
                    continue;
                    expect().fail();
            }
            expect().fail();
        }
        expect(i).toBe(5);
    });
});
