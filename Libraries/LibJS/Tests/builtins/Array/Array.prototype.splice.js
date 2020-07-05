load("test-common.js");

try {
  assert(Array.prototype.splice.length === 2);

  var array = ["hello", "friends", "serenity", 1, 2];
  var removed = array.splice(3);
  assert(array.length === 3);
  assert(array[0] === "hello");
  assert(array[1] === "friends");
  assert(array[2] === "serenity");
  assert(removed.length === 2);
  assert(removed[0] === 1);
  assert(removed[1] === 2);

  array = ["hello", "friends", "serenity", 1, 2];
  removed = array.splice(-2);
  assert(array.length === 3);
  assert(array[0] === "hello");
  assert(array[1] === "friends");
  assert(array[2] === "serenity");
  assert(removed.length === 2);
  assert(removed[0] === 1);
  assert(removed[1] === 2);

  array = ["hello", "friends", "serenity", 1, 2];
  removed = array.splice(-2, 1);
  assert(array.length === 4);
  assert(array[0] === "hello");
  assert(array[1] === "friends");
  assert(array[2] === "serenity");
  assert(array[3] === 2);
  assert(removed.length === 1);
  assert(removed[0] === 1);

  array = ["serenity"];
  removed = array.splice(0, 0, "hello", "friends");
  assert(array.length === 3);
  assert(array[0] === "hello");
  assert(array[1] === "friends");
  assert(array[2] === "serenity");
  assert(removed.length === 0);

  array = ["goodbye", "friends", "serenity"];
  removed = array.splice(0, 1, "hello");
  assert(array.length === 3);
  assert(array[0] === "hello");
  assert(array[1] === "friends");
  assert(array[2] === "serenity");
  assert(removed.length === 1);
  assert(removed[0] === "goodbye");

  array = ["foo", "bar", "baz"];
  removed = array.splice();
  assert(array.length === 3);
  assert(array[0] === "foo");
  assert(array[1] === "bar");
  assert(array[2] === "baz");
  assert(removed.length === 0);

  removed = array.splice(0, 123);
  assert(array.length === 0);
  assert(removed.length === 3);
  assert(removed[0] === "foo");
  assert(removed[1] === "bar");
  assert(removed[2] === "baz");

  array = ["foo", "bar", "baz"];
  removed = array.splice(123, 123);
  assert(array.length === 3);
  assert(array[0] === "foo");
  assert(array[1] === "bar");
  assert(array[2] === "baz");
  assert(removed.length === 0);

  array = ["foo", "bar", "baz"];
  removed = array.splice(-123, 123);
  assert(array.length === 0);
  assert(removed.length === 3);
  assert(removed[0] === "foo");
  assert(removed[1] === "bar");
  assert(removed[2] === "baz");

  console.log("PASS");
} catch (e) {
  console.log("FAIL: " + e);
}
