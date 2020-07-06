test("deleting object properties", () => {
    const o = {};
    o.x = 1;
    o.y = 2;
    o.z = 3;
    expect(Object.getOwnPropertyNames(o)).toHaveLength(3);

    expect(delete o.x).toBeTrue();
    expect(o.hasOwnProperty("x")).toBeFalse();
    expect(o.hasOwnProperty("y")).toBeTrue();
    expect(o.hasOwnProperty("z")).toBeTrue();
    expect(Object.getOwnPropertyNames(o)).toHaveLength(2);

    expect(delete o.y).toBeTrue();
    expect(o.hasOwnProperty("x")).toBeFalse();
    expect(o.hasOwnProperty("y")).toBeFalse();
    expect(o.hasOwnProperty("z")).toBeTrue();
    expect(Object.getOwnPropertyNames(o)).toHaveLength(1);

    expect(delete o.z).toBeTrue();
    expect(o.hasOwnProperty("x")).toBeFalse();
    expect(o.hasOwnProperty("y")).toBeFalse();
    expect(o.hasOwnProperty("z")).toBeFalse();
    expect(Object.getOwnPropertyNames(o)).toHaveLength(0);
});

test("deleting array indices", () => {
    const a = [3, 5, 7];

    expect(Object.getOwnPropertyNames(a)).toHaveLength(4);

    expect(delete a[0]).toBeTrue();
    expect(a.hasOwnProperty(0)).toBeFalse();
    expect(a.hasOwnProperty(1)).toBeTrue();
    expect(a.hasOwnProperty(2)).toBeTrue();
    expect(Object.getOwnPropertyNames(a)).toHaveLength(3);

    expect(delete a[1]).toBeTrue();
    expect(a.hasOwnProperty(0)).toBeFalse();
    expect(a.hasOwnProperty(1)).toBeFalse();
    expect(a.hasOwnProperty(2)).toBeTrue();
    expect(Object.getOwnPropertyNames(a)).toHaveLength(2);

    expect(delete a[2]).toBeTrue();
    expect(a.hasOwnProperty(0)).toBeFalse();
    expect(a.hasOwnProperty(1)).toBeFalse();
    expect(a.hasOwnProperty(2)).toBeFalse();
    expect(Object.getOwnPropertyNames(a)).toHaveLength(1);
});

test("deleting non-configurable property", () => {
    const q = {};
    Object.defineProperty(q, "foo", { value: 1, writable: false, enumerable: false });
    expect(q.foo).toBe(1);

    expect(delete q.foo).toBeFalse();
    expect(q.hasOwnProperty("foo")).toBeTrue();
});
