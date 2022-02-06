test("Anonymous function", function () {
    let stackString = (() => {
        return Error();
    })().stack;
    let [header, ...stackFrames] = stackString.split("\n");

    expect(header).toBe("Error");
    expect(!!stackFrames[0].match(/^    at Error \(.*\/error-stack\.js:3:\d+\)$/)).toBeTrue();
    expect(!!stackFrames[1].match(/^    at .*\/error-stack\.js:3:\d+$/)).toBeTrue();
    expect(!!stackFrames[2].match(/^    at .*\/error-stack\.js:2:\d+$/)).toBeTrue();
});

test("Named function with message", function () {
    function f() {
        throw Error("You Shalt Not Pass!");
    }
    try {
        f();
    } catch (e) {
        let stackString = e.stack;
        let [header, ...stack_frames] = stackString.split("\n");

        expect(header).toBe("Error: You Shalt Not Pass!");
        expect(!!stack_frames[0].match(/^    at Error \(.*\/error-stack\.js:15:\d+\)$/)).toBeTrue();
        expect(!!stack_frames[1].match(/^    at f \(.*\/error-stack\.js:15:\d+\)$/)).toBeTrue();
        expect(!!stack_frames[2].match(/^    at .*\/error-stack\.js:18:\d+$/)).toBeTrue();
    }
});
