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
});
