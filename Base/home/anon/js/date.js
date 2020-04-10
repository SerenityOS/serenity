var now = Date.now();
console.log("Unix timestamp: " + now / 1000);

var d = new Date();
var year = d.getFullYear();
var month = (d.getMonth() + 1).toString().padStart(2, "0");
var day = d.getDate().toString().padStart(2, "0");
var hours = d.getHours().toString().padStart(2, "0");
var minutes = d.getMinutes().toString().padStart(2, "0");
var seconds = d.getSeconds().toString().padStart(2, "0");
var milliseconds = d.getMilliseconds().toString().padStart(3, "0");

console.log("Date: " + year + "-" + month + "-" + day);
console.log("Time: " + hours + ":" + minutes + ":" + seconds + "." + milliseconds);
