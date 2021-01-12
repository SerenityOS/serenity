test("basic functionality", () => {
    expect(Error.prototype).not.toHaveProperty("length");

    var changedInstance = new Error("");
    changedInstance.name = "NewCustomError";
    expect(changedInstance.name).toBe("NewCustomError");

    var normalInstance = new Error("");
    expect(normalInstance.name).toBe("Error");
});
