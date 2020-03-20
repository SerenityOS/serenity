var a = [1, 2, 3];

a[1] = 5;

var push_result = a.push(7);

for (var i = 0; i < a.length; ++i) {
    console.log(a[i]);
}

console.log("push result: " + push_result);
