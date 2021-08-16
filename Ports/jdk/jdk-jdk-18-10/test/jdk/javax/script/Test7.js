//this is the first line of Test7.js
var filename;
try {
    load("nashorn:mozilla_compat.js");
} catch (e) {
    //ignored
}
importPackage(java.io);
importPackage(java);
var f = new File(filename);
var r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

var firstLine = r.readLine();
print(firstLine);
