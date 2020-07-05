test("basic functionality", () => {
  expect(Date).toHaveLength(7);
  expect(Date.name === "Date");
  expect(Date.prototype).not.toHaveProperty("length");
});
