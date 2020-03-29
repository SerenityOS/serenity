var now = Date.now();
console.log("Unix timestamp: " + now / 1000);

// FIXME: We need String.prototype.padStart() :^)
var d = new Date();
var year = d.getFullYear();
var month = d.getMonth() + 1;
if (month < 10)
    month = "0" + month;
var day = d.getDate();
if (day < 10)
    day = "0" + day;
var hours = d.getHours();
if (hours < 10)
    hours = "0" + hours;
var minutes = d.getMinutes();
if (minutes < 10)
    minutes = "0" + minutes;
var seconds = d.getSeconds();
if (seconds < 10)
    seconds = "0" + seconds;
var milliseconds = d.getMilliseconds();
if (milliseconds < 10) {
    milliseconds = "00" + milliseconds;
} else if (milliseconds < 100) {
    milliseconds = "0" + milliseconds;
}
console.log("Date: " + year + "-" + month + "-" + day);
console.log("Time: " + hours + ":" + minutes + ":" + seconds + "." + milliseconds);
