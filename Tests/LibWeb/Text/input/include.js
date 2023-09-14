var __outputElement = null;

function println(s) {
    __outputElement.appendChild(document.createTextNode(s + "\n"));
}

document.addEventListener("DOMContentLoaded", function () {
    __outputElement = document.createElement("pre");
    __outputElement.setAttribute("id", "out");
    document.body.appendChild(__outputElement);
});

function test(f) {
    document.addEventListener("DOMContentLoaded", f);
    window.addEventListener("load", () => {
        internals.signalTextTestIsDone();
    });
}

function asyncTest(f) {
    const done = () => internals.signalTextTestIsDone();
    document.addEventListener("DOMContentLoaded", () => {
        f(done);
    });
}
