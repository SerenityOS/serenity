test("extending function", () => {
    class A extends function () {
        this.foo = 10;
    } {}

    expect(new A().foo).toBe(10);
});

test("extending null", () => {
    class A extends null {}

    expect(Object.getPrototypeOf(A.prototype)).toBeNull();

    expect(() => {
        new A();
    }).toThrowWithMessage(ReferenceError, "|this| has not been initialized");
});

test("extending String", () => {
    class MyString extends String {}

    const ms = new MyString("abc");
    expect(ms).toBeInstanceOf(MyString);
    expect(ms).toBeInstanceOf(String);
    expect(ms.charAt(1)).toBe("b");

    class MyString2 extends MyString {
        charAt(i) {
            return `#${super.charAt(i)}`;
        }
    }

    const ms2 = new MyString2("abc");
    expect(ms2.charAt(1)).toBe("#b");
});
