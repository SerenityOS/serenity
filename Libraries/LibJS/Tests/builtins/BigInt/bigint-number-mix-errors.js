load("test-common.js");

try {
  [1, null, undefined].forEach(value => {
    assertThrowsError(
      () => {
        1n + value;
      },
      {
        error: TypeError,
        message: "Cannot use addition operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n - value;
      },
      {
        error: TypeError,
        message: "Cannot use subtraction operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n * value;
      },
      {
        error: TypeError,
        message: "Cannot use multiplication operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n / value;
      },
      {
        error: TypeError,
        message: "Cannot use division operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n % value;
      },
      {
        error: TypeError,
        message: "Cannot use modulo operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n ** value;
      },
      {
        error: TypeError,
        message: "Cannot use exponentiation operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n | value;
      },
      {
        error: TypeError,
        message: "Cannot use bitwise OR operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n & value;
      },
      {
        error: TypeError,
        message: "Cannot use bitwise AND operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n ^ value;
      },
      {
        error: TypeError,
        message: "Cannot use bitwise XOR operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n << value;
      },
      {
        error: TypeError,
        message: "Cannot use left-shift operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n >> value;
      },
      {
        error: TypeError,
        message: "Cannot use right-shift operator with BigInt and other type",
      }
    );
    assertThrowsError(
      () => {
        1n >>> value;
      },
      {
        error: TypeError,
        message: "Cannot use unsigned right-shift operator with BigInt",
      }
    );
  });

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
