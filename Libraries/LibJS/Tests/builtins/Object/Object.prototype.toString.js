test("basic functionality", () => {
  expect(Object.prototype.toString).toHaveLength(0);
  // FIXME: The tag is ObjectPrototype, but should be Object
  // expect(Object.prototype.toString()).toBe("[object Object]");
  expect({ foo: 1 }.toString()).toBe("[object Object]");
  expect([].toString()).toBe("");
  expect(Object.prototype.toString.call([])).toBe("[object Array]");
});
