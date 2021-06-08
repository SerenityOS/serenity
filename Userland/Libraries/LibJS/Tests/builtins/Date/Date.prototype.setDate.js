test("no arguments", () => {
    let date = new Date(2021, 0, 1);
    date.setDate();
    expect(date.getTime()).toBe(NaN);
});

test("NaN or undefined as only argument", () => {
    let date = new Date(2021, 0, 1);
    date.setDate(NaN);
    expect(date.getTime()).toBe(NaN);

    date = new Date(2021, 0, 1);
    date.setDate(undefined);
    expect(date.getTime()).toBe(NaN);

    date = new Date(2021, 0, 1);
    date.setDate("a");
    expect(date.getTime()).toBe(NaN);
});

test("Day as argument", () => {
    let date = new Date(2021, 0, 1);

    date.setDate(17);
    expect(date.getDate()).toBe(17);
    expect(date.getMonth()).toBe(0);
    expect(date.getFullYear()).toBe(2021);
    expect(date.getHours()).toBe(0);
    expect(date.getMinutes()).toBe(0);
    expect(date.getSeconds()).toBe(0);
    expect(date.getMilliseconds()).toBe(0);
});

test("Make Invalid Date valid again", () => {
    let date = new Date(2021, 0, 1);
    date.setDate(NaN);
    expect(date.getTime()).toBe(NaN);

    date.setDate(16);
    expect(date.getFullYear()).toBe(2021);
    expect(date.getMonth()).toBe(0);
    expect(date.getDate()).toBe(16);
    expect(date.getHours()).toBe(0);
    expect(date.getMinutes()).toBe(0);
    expect(date.getSeconds()).toBe(0);
    expect(date.getMilliseconds()).toBe(0);
});
