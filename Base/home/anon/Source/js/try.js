try {
    console.log("you should see me");
    foo();
    console.log("not me");
} catch (e) {
    console.log("catch");
    console.log(e.name);
} finally {
    console.log("finally");
}
