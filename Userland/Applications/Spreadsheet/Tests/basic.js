describe("Position", () => {
    test("here", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();

        sheet.setCell("A", 0, "0");
        sheet.focusCell("A", 0);

        expect(here).toBeDefined();
        let position = here();
        expect(position).toBeDefined();

        expect(position.column).toEqual("A");
        expect(position.name).toEqual("A0");
        expect(position.row).toEqual(0);
        expect(position.sheet).toBe(sheet);

        expect(position.contents).toEqual("0");
        expect(position.value()).toEqual("0");
        expect(position.toString()).toEqual("<Cell at A0>");

        position.contents = "=1 + 1";
        expect(position.contents).toEqual("=1 + 1");
        expect(position.value()).toEqual(2);

        expect(position.up().row).toEqual(0);
        expect(position.down().row).toEqual(1);
        expect(position.right().row).toEqual(0);
        expect(position.left().row).toEqual(0);

        sheet.addColumn("B");
        expect(position.up().column).toEqual("A");
        expect(position.down().column).toEqual("A");
        expect(position.right().column).toEqual("B");
        expect(position.left().column).toEqual("A");
    });

    test("Position.from_name", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();

        sheet.setCell("A", 0, "0");
        sheet.focusCell("A", 0);

        expect(Position.from_name).toBeDefined();
        let position = Position.from_name("A0");
        expect(position).toBeInstanceOf(Position);

        position = Position.from_name("A123");
        expect(position).toBeInstanceOf(Position);
    });
});

describe("Range", () => {
    test("simple", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();

        sheet.setCell("A", 0, "0");
        sheet.setCell("A", 10, "0");
        sheet.setCell("B", 1, "0");
        sheet.focusCell("A", 0);

        expect(R).toBeDefined();
        let cellsVisited = 0;
        R`A0:A10`.forEach(name => {
            ++cellsVisited;
        });
        expect(cellsVisited).toEqual(11);

        cellsVisited = 0;
        R`A0:A10:1:2`.forEach(name => {
            ++cellsVisited;
        });
        expect(cellsVisited).toEqual(6);
    });

    test("multiple sheets", () => {
        const workbook = createWorkbook();
        const sheet1 = createSheet(workbook, "Sheet 1");
        const sheet2 = createSheet(workbook, "Sheet 2");
        sheet1.makeCurrent();

        sheet1.setCell("A", 0, "0");
        sheet1.focusCell("A", 0);

        sheet2.setCell("A", 0, "0");
        sheet2.setCell("A", 10, "0");
        sheet2.setCell("B", 1, "0");
        sheet2.focusCell("A", 0);

        expect(R).toBeDefined();
        let cellsVisited = 0;
        R`sheet("Sheet 2"):A0:A10`.forEach(name => {
            ++cellsVisited;
        });
        expect(cellsVisited).toEqual(11);
    });

    test("Ranges", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();

        sheet.setCell("A", 0, "0");
        sheet.setCell("A", 10, "0");
        sheet.setCell("B", 1, "0");
        sheet.focusCell("A", 0);

        let cellsVisited = 0;
        R`A0:A5`.union(R`A6:A10`).forEach(name => {
            ++cellsVisited;
        });
        expect(cellsVisited).toEqual(11);
    });

    test("Range#first", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();

        sheet.setCell("A", 0, "0");
        sheet.setCell("A", 1, "0");
        sheet.setCell("A", 2, "0");
        sheet.focusCell("A", 0);
        expect(R`A0:A`.first().name).toEqual("A0");
        expect(R`A0:A25`.first().name).toEqual("A0");
        expect(R`A2:A25`.first().name).toEqual("A2");
    });

    test("CommonRange#at", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();
        let i = 0;
        for (const col of ["A", "B"]) {
            for (const row of [0, 1, 2]) {
                sheet.setCell(col, row, Math.pow(i++, 2));
            }
        }

        sheet.focusCell("A", 0);
        expect(R`A0:A2`.at(2).name).toEqual("A2");
        expect(Ranges.from(R`A0:A2`, R`B0:B2`).at(5).name).toEqual("B2");
    });

    test("CommonRange#toArray", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "Sheet 1");
        sheet.makeCurrent();
        let i = 0;
        for (const col of ["A", "B"]) {
            for (const row of [0, 1, 2]) {
                sheet.setCell(col, row, Math.pow(i++, 2));
            }
        }

        sheet.focusCell("A", 0);
        expect(R`A0:A2`.toArray().toString()).toEqual("<Cell at A0>,<Cell at A1>,<Cell at A2>");
        expect(
            Ranges.from(R`A0:A2`, R`B0:B2`)
                .toArray()
                .toString()
        ).toEqual("<Cell at A0>,<Cell at A1>,<Cell at A2>,<Cell at B0>,<Cell at B1>,<Cell at B2>");
    });

    test("CommonRange#findIndex", () => {
        makeSheet();
        const range = R`B`;
        let idxs = [];
        let values = [];
        const idx = range.findIndex((val, idx) => {
            idxs.push(idx);
            values.push(val.value());
            return integer(val.value()) === 4;
        });
        expect(idx).toEqual(1);
        expect(values).toEqual(["1", "4"]);
        expect(idxs).toEqual([0, 1]);
    });

    test("CommonRange#find", () => {
        makeSheet();
        const range = R`B`;
        let idxs = [];
        let values = [];
        const val = range.find((val, idx) => {
            idxs.push(idx);
            values.push(val.value());
            return integer(val.value()) === 4;
        });
        expect(val.name).toEqual("B1");
        expect(values).toEqual(["1", "4"]);
        expect(idxs).toEqual([0, 1]);
    });

    test("CommonRange#indexOf", () => {
        makeSheet();
        const range = R`B`;
        expect(range.indexOf("B1")).toEqual(1);
    });

    test("CommonRange#has", () => {
        makeSheet();
        const range = R`B`;
        expect(range.has("B1")).toEqual(true);
        expect(range.has("B4")).toEqual(false);
    });
});

describe("SplitRange", () => {
    makeSheet();
    test("Range#filter => SplitRange", () => {
        const range = R`A0:B`.filter(c => c.value() % 2 === 1);
        expect(range.toString()).toEqual('SplitRange.fromNames("A0", "A2", "B0", "B2")');
        expect(resolve(range)).toEqual(["1", "3", "1", "9"]);
        expect(numericResolve(range)).toEqual([1, 3, 1, 9]);
        expect(count(range)).toEqual(4);
    });

    test("Range#unique => SplitRange", () => {
        makeSheet();

        const origRange = R`A0:B`;
        const uniqueRange = origRange.unique();
        expect(uniqueRange.toString()).toEqual(
            'SplitRange.fromNames("A0", "A1", "A2", "B1", "B2")'
        );

        const uniqueCount = count(uniqueRange);
        // We expect that making a set (unique array) of the original range should equal the length of our unique range
        expect(new Set(resolve(origRange)).size).toEqual(uniqueCount);
        expect(uniqueCount).toEqual(5);
    });
});

describe("R function", () => {
    makeSheet();

    test("Check for correctness: R`A0:A`", () => {
        const range = R`A0:A`;
        expect(range.toString()).toEqual("R`A0:A`");
        expect(count(range)).toEqual(3);
        expect(sum(range)).toEqual(6);
    });
    test("Check for correctness: R`A`", () => {
        const range = R`A`;
        expect(range.toString()).toEqual("R`A0:A`");
        expect(count(range)).toEqual(3);
        expect(sum(range)).toEqual(6);
    });
    test("Check for correctness: R`A0:B`", () => {
        const range = R`A0:B`;
        expect(range.toString()).toEqual("R`A0:B`");
        expect(count(range)).toEqual(6);
        expect(sum(range)).toEqual(20);
    });
    test("Check for correctness: R`A0:B0`", () => {
        const range = R`A0:B0`;
        expect(range.toString()).toEqual("R`A0:B0`");
        expect(count(range)).toEqual(2);
        expect(sum(range)).toEqual(2);
    });
    test("Check for correctness: R`A0:B:2:1`", () => {
        const range = R`A0:B2:1:2`;
        expect(range.toString()).toEqual("R`A0:B2:1:2`");
        expect(range.toArray().toString()).toEqual(
            "<Cell at A0>,<Cell at A2>,<Cell at B0>,<Cell at B2>"
        );
        expect(count(range)).toEqual(4);
        expect(sum(range)).toEqual(14);
    });
    test("R`B`", () => {
        const range = R`B`;
        expect(range.toString()).toEqual("R`B0:B`");
        expect(count(range)).toEqual(3);
        expect(sum(range)).toEqual(14);
    });
});

/*
 A B
 +++
0+1 1
1+2 4
2+3 9
*/

function makeSheet() {
    const workbook = createWorkbook();
    const sheet = createSheet(workbook, "Sheet 1");
    sheet.makeCurrent();
    for (const row of ["A", "B"]) {
        for (let col of [0, 1, 2]) {
            sheet.setCell(row, col++, row === "A" ? col : Math.pow(col, 2));
        }
    }
    sheet.focusCell("A", 0);
}
