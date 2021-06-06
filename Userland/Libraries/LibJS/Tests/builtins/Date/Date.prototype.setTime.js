test("no arguments", () => {
    let date = new Date(2021, 0, 1);
    date.setTime();
    expect(date.getTime()).toBe(NaN);
});

test("NaN or undefined as only argument", () => {
    let date = new Date(2021, 0, 1);
    date.setTime(NaN);
    expect(date.getTime()).toBe(NaN);

    date = new Date(2021, 0, 1);
    date.setTime(undefined);
    expect(date.getTime()).toBe(NaN);

    date = new Date(2021, 0, 1);
    date.setTime("a");
    expect(date.getTime()).toBe(NaN);
});

test("Timestamp as argument", () => {
    let date = new Date(2021, 0, 1);

    date.setTime(1622993746000);
    expect(date.getDate()).toBe(6);
    expect(date.getMonth()).toBe(5);
    expect(date.getFullYear()).toBe(2021);
    expect(date.getUTCHours()).toBe(15);
    expect(date.getUTCMinutes()).toBe(35);
    expect(date.getUTCSeconds()).toBe(46);
    expect(date.getUTCMilliseconds()).toBe(0);
});

test("Make Invalid Date valid again", () => {
    let date = new Date(2021, 0, 1);
    date.setTime(NaN);
    expect(date.getTime()).toBe(NaN);

    date.setTime(1622993746000);
    expect(date.getDate()).toBe(6);
    expect(date.getMonth()).toBe(5);
    expect(date.getFullYear()).toBe(2021);
    expect(date.getUTCHours()).toBe(15);
    expect(date.getUTCMinutes()).toBe(35);
    expect(date.getUTCSeconds()).toBe(46);
    expect(date.getUTCMilliseconds()).toBe(0);
});
