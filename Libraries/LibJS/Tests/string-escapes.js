test("hex escapes", () => {
  expect("\x55").toBe("U");
  expect("X55").toBe("X55");
  expect(`\x55`).toBe("U");
  expect(`\X55`).toBe("X55");
});

test("unicode escapes", () => {
  expect("\u26a0").toBe("⚠");
  expect(`\u26a0`).toBe("⚠");
  expect("\u{1f41e}").toBe("🐞");
  expect(`\u{1f41e}`).toBe("🐞");
});
