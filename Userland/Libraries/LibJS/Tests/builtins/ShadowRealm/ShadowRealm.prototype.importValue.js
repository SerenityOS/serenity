describe("normal behavior", () => {
    test("length is 2", () => {
        expect(ShadowRealm.prototype.importValue).toHaveLength(2);
    });

    test("basic functionality", () => {
        // NOTE: The actual import is currently not implemented and always pretends to fail for now.
        const shadowRealm = new ShadowRealm();
        const promise = shadowRealm.importValue("./myModule.js", "foo");
        let error;
        promise.catch(value => {
            error = value;
        });
        expect(promise).toBeInstanceOf(Promise);
        runQueuedPromiseJobs();
        expect(error).toBeInstanceOf(TypeError);
        expect(error.message).toBe("Import of 'foo' from './myModule.js' failed");
    });
});

describe("errors", () => {
    test("this value must be a ShadowRealm object", () => {
        expect(() => {
            ShadowRealm.prototype.importValue.call("foo");
        }).toThrowWithMessage(TypeError, "Not an object of type ShadowRealm");
    });
});
