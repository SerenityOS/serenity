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
    expect(Date.parse("-000001")).toBe(-62198755200000);
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
    expect(Date.parse("-000000")).toBe(NaN);
});

test("time clip", () => {
    expect(Date.parse("+999999")).toBeNaN();
    expect(Date.parse("-999999")).toBeNaN();
});

test("extra micro seconds extension", () => {
    expect(Date.parse("2021-04-30T15:19:02.937+00:00")).toBe(1619795942937);
    expect(Date.parse("2021-04-30T15:19:02.9370+00:00")).toBe(1619795942937);
    expect(Date.parse("2021-04-30T15:19:02.93700+00:00")).toBe(1619795942937);
    expect(Date.parse("2021-04-30T15:19:02.937000+00:00")).toBe(1619795942937);

    expect(Date.parse("2021-04-30T15:19:02.93+00:00")).toBe(1619795942930);
    expect(Date.parse("2021-04-30T15:19:02.9+00:00")).toBe(1619795942900);

    // These values are just checked against NaN since they don't have a specified timezone.
    expect(Date.parse("2021-04-30T15:19:02.93")).not.toBe(NaN);
    expect(Date.parse("2021-04-30T15:19:02.9")).not.toBe(NaN);

    expect(Date.parse("2021-04-30T15:19:02.+00:00")).toBe(NaN);
    expect(Date.parse("2021-04-30T15:19:02.")).toBe(NaN);
    expect(Date.parse("2021-04-30T15:19:02.a")).toBe(NaN);
    expect(Date.parse("2021-04-30T15:19:02.000a")).toBe(NaN);

    expect(Date.parse("2021-04-30T15:19:02.937001+00:00")).toBe(1619795942937);
    expect(Date.parse("2021-04-30T15:19:02.937999+00:00")).toBe(1619795942937);

    expect(Date.parse("2021-06-26T07:24:40.007000+00:00")).toBe(1624692280007);
    expect(Date.parse("2021-06-26T07:24:40.0079999999999999999+00:00")).toBe(1624692280007);
    expect(Date.parse("2021-04-15T18:47:25.606000+00:00")).toBe(1618512445606);
});

test("extra date extension", () => {
    function expectStringToGiveDate(input, fullYear, month, dayInMonth) {
        // Since the timezone is not specified we just say it has to equal the date parts.
        const date = new Date(Date.parse(input));
        expect(date.getFullYear()).toBe(fullYear);
        expect(date.getMonth() + 1).toBe(month);
        expect(date.getDate()).toBe(dayInMonth);
    }

    expectStringToGiveDate("01/30/2021", 2021, 1, 30);
    expectStringToGiveDate("10/1/2021", 2021, 10, 1);
    expectStringToGiveDate("7/07/1977", 1977, 7, 7);
    expectStringToGiveDate("2/27/3058", 3058, 2, 27);
});
