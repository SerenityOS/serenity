describe("returning from loops", () => {
    test("returning from while loops", () => {
        function foo() {
            while (true) {
                return 10;
            }
        }

        expect(foo()).toBe(10);
    });

    test("returning from do-while loops", () => {
        function foo() {
            do {
                return 10;
            } while (true);
        }

        expect(foo()).toBe(10);
    });

    test("returning from for loops", () => {
        function foo() {
            for (let i = 0; i < 5; i++) {
                return 10;
            }
        }

        expect(foo()).toBe(10);
    });

    test("returning from for-in loops", () => {
        function foo() {
            const o = { a: 1, b: 2 };
            for (let a in o) {
                return 10;
            }
        }

        expect(foo()).toBe(10);
    });

    test("returning from for-of loops", () => {
        function foo() {
            const o = [1, 2, 3];
            for (let a of o) {
                return 10;
            }
        }

        expect(foo()).toBe(10);
    });
});
