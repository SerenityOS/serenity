const a = 1;
const computedKey = "d";
const object = {a, b: 2, "c": 3, [computedKey]: 2 + 2};
const emptyObject = {};

console.log(object.a);
console.log(object.b);
console.log(object.c);
console.log(object.d);
console.log(emptyObject.foo);
