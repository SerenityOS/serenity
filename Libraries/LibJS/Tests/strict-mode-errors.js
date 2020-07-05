"use strict";

load("test-common.js");

try {
  [true, false, "foo", 123].forEach(primitive => {
    assertThrowsError(
      () => {
        primitive.foo = "bar";
      },
      {
        error: TypeError,
        message: "Cannot assign property foo to primitive value",
      }
    );
  });

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
