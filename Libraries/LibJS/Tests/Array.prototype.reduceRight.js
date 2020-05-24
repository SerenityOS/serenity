load("test-common.js");

try {
  assert(Array.prototype.reduceRight.length === 1);

  assertThrowsError(
    () => {
      [1].reduceRight();
    },
    {
      error: TypeError,
      message: "Array.prototype.reduceRight() requires at least one argument",
    }
  );

  assertThrowsError(
    () => {
      [1].reduceRight(undefined);
    },
    {
      error: TypeError,
      message: "undefined is not a function",
    }
  );

  assertThrowsError(
    () => {
      [].reduceRight((a, x) => x);
    },
    {
      error: TypeError,
      message: "Reduce of empty array with no initial value",
    }
  );

  assertThrowsError(
    () => {
      [, ,].reduceRight((a, x) => x);
    },
    {
      error: TypeError,
      message: "Reduce of empty array with no initial value",
    }
  );

  [1, 2].reduceRight(() => {
    assert(this === undefined);
  });

  var callbackCalled = 0;
  var callback = () => {
    callbackCalled++;
    return true;
  };

  assert([1].reduceRight(callback) === 1);
  assert(callbackCalled === 0);

  assert([1].reduceRight(callback) === 1);
  assert(callbackCalled === 0);

  callbackCalled = 0;
  assert([1, 2, 3].reduceRight(callback) === true);
  assert(callbackCalled === 2);

  callbackCalled = 0;
  assert([1, 2, 3, ,].reduceRight(callback) === true);
  assert(callbackCalled === 2);

  callbackCalled = 0;
  assert([, , , 1, , , 10, , 100, , ,].reduceRight(callback) === true);
  assert(callbackCalled === 2);

  var constantlySad = () => ":^(";
  var result = [].reduceRight(constantlySad, ":^)");
  assert(result === ":^)");

  result = [":^0"].reduceRight(constantlySad, ":^)");
  assert(result === ":^(");

  result = [":^0"].reduceRight(constantlySad);
  assert(result === ":^0");

  result = [5, 4, 3, 2, 1].reduceRight((accum, elem) => "" + accum + elem);
  assert(result === "12345");

  result = [1, 2, 3, 4, 5, 6].reduceRight((accum, elem) => {
    return "" + accum + elem;
  }, 100);
  assert(result === "100654321");

  result = [6, 5, 4, 3, 2, 1].reduceRight((accum, elem) => {
    return "" + accum + elem;
  }, 100);
  assert(result === "100123456");

  var indexes = [];
  result = ["foo", 1, true].reduceRight((a, v, i) => {
    indexes.push(i);
  });
  assert(result === undefined);
  assert(indexes.length === 2);
  assert(indexes[0] === 1);
  assert(indexes[1] === 0);

  indexes = [];
  result = ["foo", 1, true].reduceRight((a, v, i) => {
    indexes.push(i);
  }, "foo");
  assert(result === undefined);
  assert(indexes.length === 3);
  assert(indexes[0] === 2);
  assert(indexes[1] === 1);
  assert(indexes[2] === 0);

  var mutable = { prop: 0 };
  result = ["foo", 1, true].reduceRight((a, v) => {
    a.prop = v;
    return a;
  }, mutable);
  assert(result === mutable);
  assert(result.prop === "foo");

  var a1 = [1, 2];
  var a2 = null;
  a1.reduceRight((a, v, i, t) => {
    a2 = t;
  });
  assert(a1 === a2);

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
