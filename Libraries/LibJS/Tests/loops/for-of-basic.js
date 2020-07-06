describe("correct behavior", () => {
  test("iterate through array", () => {
    const a = [];
    for (const num of [1, 2, 3]) {
      a.push(num);
    }
    expect(a).toEqual([1, 2, 3]);
  });

  test("iterate through string", () => {
    const a = [];
    for (const char of "hello") {
      a.push(char);
    }
    expect(a).toEqual(["h", "e", "l", "l", "o"]);
  });

  test("iterate through string object", () => {
    const a = [];
    for (const char of new String("hello")) {
      a.push(char);
    }
    expect(a).toEqual(["h", "e", "l", "l", "o"]);
  });

  test("use already-declared variable", () => {
    var char;
    for (char of "abc");
    expect(char).toBe("c");
  });
});

describe("errors", () => {
  test("right hand side is a primitive", () => {
    expect(() => {
      for (const _ of 123) {
      }
    }).toThrowWithMessage(TypeError, "for..of right-hand side must be iterable");
  });

  test("right hand side is an object", () => {
    expect(() => {
      for (const _ of { foo: 1, bar: 2 }) {
      }
    }).toThrowWithMessage(TypeError, "for..of right-hand side must be iterable");
  });
});
