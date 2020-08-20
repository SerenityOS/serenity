test("basic functionality", () => {
    // FIXME: Years before 1970 don't work. Once they do, add a test. Also add a test with a year before 1900 then.
    expect(Date.UTC(2020)).toBe(1577836800000);
    expect(Date.UTC(2000, 10)).toBe(973036800000);
    expect(Date.UTC(1980, 5, 30)).toBe(331171200000);
    expect(Date.UTC(1980, 5, 30, 13)).toBe(331218000000);
    expect(Date.UTC(1970, 5, 30, 13, 30)).toBe(15600600000);
    expect(Date.UTC(1970, 0, 1, 0, 0, 59)).toBe(59000);
    expect(Date.UTC(1970, 0, 1, 0, 0, 0, 999)).toBe(999);
});
