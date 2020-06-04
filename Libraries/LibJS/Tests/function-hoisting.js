load("test-common.js");

try {
    var callHoisted = hoisted();
    function hoisted() {
        return true;
    }
    assert(hoisted() === true);
    assert(callHoisted === true);

    {
        var callScopedHoisted = scopedHoisted();
        function scopedHoisted() {
            return "foo";
        }
        assert(scopedHoisted() === "foo");
        assert(callScopedHoisted === "foo");
    }
    assert(scopedHoisted() === "foo");
    assert(callScopedHoisted === "foo");

    const test = () => {
        var iife = (function () {
            return declaredLater();
        })();
        function declaredLater() {
            return "yay";
        }
        return iife;
    };
    assert(typeof declaredLater === "undefined");
    assert(test() === "yay");

    console.log("PASS");
} catch (e) {
    console.log("FAIL: " + e);
}
