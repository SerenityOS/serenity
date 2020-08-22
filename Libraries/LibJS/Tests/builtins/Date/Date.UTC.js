test("basic functionality", () => {
    expect(Date.UTC(2020)).toBe(1577836800000);
    expect(Date.UTC(2000, 10)).toBe(973036800000);
    expect(Date.UTC(1980, 5, 30)).toBe(331171200000);
    expect(Date.UTC(1980, 5, 30, 13)).toBe(331218000000);
    expect(Date.UTC(1970, 5, 30, 13, 30)).toBe(15600600000);
    expect(Date.UTC(1970, 0, 1, 0, 0, 59)).toBe(59000);
    expect(Date.UTC(1970, 0, 1, 0, 0, 0, 999)).toBe(999);

    expect(Date.UTC(1969, 11, 31, 23, 59, 59, 817)).toBe(-183);

    expect(Date.UTC(1799, 0)).toBe(-5396198400000);
    expect(Date.UTC(1800, 0)).toBe(-5364662400000);
    expect(Date.UTC(1801, 0)).toBe(-5333126400000);
    expect(Date.UTC(1802, 0)).toBe(-5301590400000);
    expect(Date.UTC(1803, 0)).toBe(-5270054400000);
    expect(Date.UTC(1804, 0)).toBe(-5238518400000);

    expect(Date.UTC(1999, 0)).toBe(915148800000);
    expect(Date.UTC(2000, 0)).toBe(946684800000);
    expect(Date.UTC(2001, 0)).toBe(978307200000);
    expect(Date.UTC(2002, 0)).toBe(1009843200000);
    expect(Date.UTC(2003, 0)).toBe(1041379200000);
    expect(Date.UTC(2004, 0)).toBe(1072915200000);

    expect(Date.UTC(20000, 0)).toBe(568971820800000);
});
