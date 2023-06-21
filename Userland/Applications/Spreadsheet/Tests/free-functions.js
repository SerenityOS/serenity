describe("Basic functions", () => {
    const workbook = createWorkbook();
    const sheet = createSheet(workbook, "Sheet 1");
    sheet.makeCurrent();

    sheet.setCell("A", 0, "0");
    sheet.setCell("A", 1, "1");
    sheet.setCell("A", 2, "2");

    test("select", () => {
        expect(select).toBeDefined();
        expect(select(true, 1, 2)).toBe(1);
        expect(select(false, 1, 2)).toBe(2);
    });

    test("choose", () => {
        expect(choose).toBeDefined();
        expect(choose(0, 1, 2, 3)).toBe(1);
        expect(choose(1, 1, 2, 3)).toBe(2);
        expect(choose(3, 1, 2, 3)).toBeUndefined();
        expect(choose(-1, 1, 2, 3)).toBeUndefined();
    });

    test("now", () => {
        expect(now).toBeDefined();
        expect(now()).toBeInstanceOf(Date);
    });

    test("randRange", () => {
        expect(randRange).toBeDefined();
    });

    test("integer", () => {
        expect(integer).toBeDefined();
        expect(integer("0")).toEqual(0);
        expect(integer("32")).toEqual(32);
    });

    test("sheet", () => {
        expect(globalThis.sheet).toBeDefined();
        expect(globalThis.sheet("Sheet 1")).toBe(sheet);
        expect(globalThis.sheet("Not a sheet")).toBeUndefined();
    });

    test("reduce", () => {
        expect(reduce).toBeDefined();
        expect(reduce(acc => acc + 1, 0, [1, 2, 3, 4])).toEqual(4);
        expect(reduce(acc => acc + 1, 0, [])).toEqual(0);
        expect(reduce((acc, x) => acc + "|" + x.toString(), 0, R`A0:A2`)).toEqual("0|0|1|2");
        expect(reduce((acc, x) => acc + "|" + x.toString(), 0, R`A0:A0`)).toEqual("0|0");
    });

    test("numericReduce", () => {
        expect(numericReduce).toBeDefined();
        expect(numericReduce(acc => acc + 1, 0, [1, 2, 3, 4])).toEqual(4);
        expect(numericReduce(acc => acc + 1, 0, [])).toEqual(0);
        expect(numericReduce((acc, x) => acc + x, 19, R`A0:A2`)).toEqual(22);
        expect(numericReduce(acc => acc + 1, 3, R`A0:A0`)).toEqual(4);
    });

    test("numericResolve", () => {
        expect(numericResolve).toBeDefined();
        expect(numericResolve(["0", "1", "2"])).toEqual([0, 1, 2]);
        expect(numericResolve([])).toEqual([]);
        expect(numericResolve(R`A0:A2`)).toEqual([0, 1, 2]);
        expect(numericResolve(R`A0:A0`)).toEqual([0]);
    });

    test("resolve", () => {
        expect(resolve).toBeDefined();
        expect(resolve(["A", "B", "C"])).toEqual(["A", "B", "C"]);
        expect(resolve([])).toEqual([]);
        expect(resolve(R`A0:A2`)).toEqual(["0", "1", "2"]);
        expect(resolve(R`A0:A0`)).toEqual(["0"]);
    });
});

describe("Statistics", () => {
    const workbook = createWorkbook();
    const sheet = createSheet(workbook, "Sheet 1");
    sheet.makeCurrent();

    for (let i = 0; i < 10; ++i) sheet.setCell("A", i, `${i}`);
    for (let i = 0; i < 10; ++i) sheet.setCell("B", i, `${i * i}`);

    test("sum", () => {
        expect(sum).toBeDefined();
        expect(sum(R`A0:A9`)).toEqual(45);
    });

    test("sumIf", () => {
        expect(sumIf).toBeDefined();
        expect(sumIf(x => !Number.isNaN(x), R`A0:A10`)).toEqual(45);
    });

    test("count", () => {
        expect(count).toBeDefined();
        expect(count(R`A0:A9`)).toEqual(10);
    });

    test("countIf", () => {
        expect(countIf).toBeDefined();
        expect(countIf(x => x, R`A0:A10`)).toEqual(10);
    });

    test("average", () => {
        expect(average).toBeDefined();
        expect(average(R`A0:A9`)).toEqual(4.5);
    });

    test("averageIf", () => {
        expect(averageIf).toBeDefined();
        expect(averageIf(x => !Number.isNaN(x), R`A0:A10`)).toEqual(4.5);
    });

    test("minIf", () => {
        expect(minIf).toBeDefined();
        expect(minIf(x => x > 25, R`B0:B9`)).toEqual(36);
    });

    test("min", () => {
        expect(min).toBeDefined();
        expect(min(R`B0:B9`)).toEqual(0);
    });

    test("maxIf", () => {
        expect(maxIf).toBeDefined();
        expect(maxIf(x => x > 25, R`B0:B9`)).toEqual(81);
    });

    test("max", () => {
        expect(max).toBeDefined();
        expect(max(R`B0:B9`)).toEqual(81);
    });

    test("sumProductIf", () => {
        expect(sumProductIf).toBeDefined();
        expect(sumProductIf((a, b) => b > 25, R`A0:A9`, R`B0:B9`)).toEqual(1800);
    });

    test("sumProduct", () => {
        expect(sumProduct).toBeDefined();
        expect(sumProduct(R`A0:A9`, R`B0:B9`)).toEqual(2025);
    });

    test("median", () => {
        expect(median).toBeDefined();
        expect(median(R`A0:A9`)).toEqual(4.5);
        expect(median(R`A0:A2`)).toEqual(1);
    });

    test("variance", () => {
        expect(variance).toBeDefined();
        expect(variance(R`A0:A0`)).toEqual(0);
        expect(variance(R`A0:A9`)).toEqual(82.5);
    });

    test("mode", () => {
        expect(mode).toBeDefined();
        expect(mode(R`A0:A0`.union(R`A0:A0`).union(R`A1:A9`))).toEqual(0);
    });

    test("stddev", () => {
        expect(stddev).toBeDefined();
        expect(stddev(R`A0:A0`)).toEqual(0);
        expect(stddev(R`A0:A9`)).toEqual(Math.sqrt(82.5));
    });
});

describe("Lookup", () => {
    const workbook = createWorkbook();
    const sheet = createSheet(workbook, "Sheet 1");
    sheet.makeCurrent();

    for (let i = 0; i < 10; ++i) {
        sheet.setCell("A", i, `${i}`);
        sheet.setCell("B", i, `B${i}`);
    }

    sheet.focusCell("A", 0);

    test("row", () => {
        expect(row()).toEqual(0);
    });

    test("column", () => {
        expect(column()).toEqual("A");
    });

    test("lookup", () => {
        expect(lookup).toBeDefined();
        // Note: String ordering.
        expect(lookup("2", R`A0:A9`, R`B0:B9`)).toEqual("B2");
        expect(() => lookup("20", R`A0:A9`, R`B0:B9`)).toThrow();
        expect(lookup("80", R`A0:A9`, R`B0:B9`, undefined, "nextlargest")).toEqual("B9");
    });

    test("reflookup", () => {
        expect(reflookup).toBeDefined();
        // Note: String ordering.
        expect(reflookup("2", R`A0:A9`, R`B0:B9`).name).toEqual("B2");
        expect(() => reflookup("20", R`A0:A9`, R`B0:B9`)).toThrow();
        expect(reflookup("80", R`A0:A9`, R`B0:B9`, undefined, "nextlargest").name).toEqual("B9");
    });
});

describe("integer() function", () => {
    test("undefined", () => {
        expect(() => integer(undefined)).toThrow(Error);
    });
    test("null", () => {
        expect(() => integer(null)).toThrow(Error);
    });
    test("NaN", () => {
        expect(() => integer(NaN)).toThrow(Error);
    });
    test("object", () => {
        expect(() => integer({})).toThrow(Error);
    });
    test("function", () => {
        expect(() => integer(() => {})).toThrow(Error);
    });
    test("try 1 as string", () => {
        expect(integer("1")).toBe(1);
    });
    test("try 1 as number", () => {
        expect(integer(1)).toBe(1);
    });
    test("try 1000000 as string", () => {
        expect(integer("1000000")).toBe(1000000);
    });
    test("try 1000000 as number", () => {
        expect(integer(1000000)).toBe(1000000);
    });
    test("don't just allow any strings", () => {
        expect(() => integer("is this NaN yet?")).toThrow();
    });
});
