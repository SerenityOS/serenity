test("return from try followed by finally with function call inside", () => {
    let value = (() => {
        try {
            return 1;
        } finally {
            (() => {})();
        }
    })();

    expect(value).toBe(1);
});
