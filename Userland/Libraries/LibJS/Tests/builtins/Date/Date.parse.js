test("basic functionality", () => {
    expect(Date.parse("2020")).toBe(1577836800000);
    expect(Date.parse("2000-11")).toBe(973036800000);
    expect(Date.parse("1980-06-30")).toBe(331171200000);
    expect(Date.parse("1970-06-30T13:30Z")).toBe(15600600000);
    expect(Date.parse("1970-01-01T00:00:59Z")).toBe(59000);
    expect(Date.parse("1970-01-01T00:00:00.999Z")).toBe(999);
    expect(Date.parse("2020T13:14+15:16")).toBe(1577829480000);
    expect(Date.parse("2020T13:14-15:16")).toBe(1577939400000);
    expect(Date.parse("2020T23:59Z")).toBe(1577923140000);

    expect(Date.parse("+020000")).toBe(568971820800000);
    expect(Date.parse("+020000-01")).toBe(568971820800000);
    expect(Date.parse("+020000-01T00:00:00.000Z")).toBe(568971820800000);

    expect(Date.parse(2020)).toBe(1577836800000);

    expect(Date.parse("+1980")).toBe(NaN);
    expect(Date.parse("1980-")).toBe(NaN);
    expect(Date.parse("1980-05-")).toBe(NaN);
    expect(Date.parse("1980-05-00T")).toBe(NaN);
    expect(Date.parse("1980-05-00T15:15:")).toBe(NaN);
    expect(Date.parse("1980-05-00T15:15:15.")).toBe(NaN);
    expect(Date.parse("1980-5-30")).toBe(NaN);
    expect(Date.parse("1980-05-30T13")).toBe(NaN);
    expect(Date.parse("1980-05-30T13:4")).toBe(NaN);
    expect(Date.parse("1980-05-30T13:40+")).toBe(NaN);
    expect(Date.parse("1980-05-30T13:40+1")).toBe(NaN);
    expect(Date.parse("1980-05-30T13:40+1:10")).toBe(NaN);
    expect(Date.parse("1970-06-30T13:30Zoo")).toBe(NaN);
    expect(Date.parse("2020T13:30.40:")).toBe(NaN);
});
