var __outputElement = null;
let __alreadyCalledTest = false;
function __preventMultipleTestFunctions() {
    if (__alreadyCalledTest) {
        throw new Error("You must only call test() or asyncTest() once per page");
    }
    __alreadyCalledTest = true;
}

if (globalThis.internals === undefined) {
    internals = {
        signalTextTestIsDone: function () {},
    };
}

function println(s) {
    __outputElement.appendChild(document.createTextNode(s + "\n"));
}

document.addEventListener("DOMContentLoaded", function () {
    __outputElement = document.createElement("pre");
    __outputElement.setAttribute("id", "out");
    document.body.appendChild(__outputElement);
});

function test(f) {
    __preventMultipleTestFunctions();
    document.addEventListener("DOMContentLoaded", f);
    window.addEventListener("load", () => {
        internals.signalTextTestIsDone();
    });
}

function asyncTest(f) {
    const done = () => {
        __preventMultipleTestFunctions();
        internals.signalTextTestIsDone();
    };
    document.addEventListener("DOMContentLoaded", () => {
        f(done);
    });
}
