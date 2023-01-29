describe("Harness-defined functions", () => {
    test("createWorkbook", () => {
        expect(createWorkbook).toBeDefined();
        const workbook = createWorkbook();
        expect(workbook).toBeDefined();
        expect(workbook.sheet).toBeDefined();
    });
    test("createSheet", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "foo");
        expect(sheet).toBeDefined();
        expect(sheet.get_real_cell_contents).toBeDefined();
        expect(sheet.set_real_cell_contents).toBeDefined();
        expect(sheet.parse_cell_name).toBeDefined();
        expect(sheet.current_cell_position).toBeDefined();
        expect(sheet.column_index).toBeDefined();
        expect(sheet.column_arithmetic).toBeDefined();
        expect(sheet.get_column_bound).toBeDefined();
    });
    test("Sheet mock behavior", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "foo");
        sheet.setCell("A", 0, "10");
        expect(sheet.getCell("A", 0)).toEqual(["10", "10"]);

        sheet.setCell("A", 0, "=10");
        expect(sheet.getCell("A", 0)).toEqual(["=10", 10]);

        expect(sheet.getColumns()).toEqual(["A"]);
    });
    test("Workbook mock behavior", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "foo");
        expect(workbook.sheet("foo")).toBe(sheet);
        expect(workbook.sheet(0)).toBe(sheet);
        expect(workbook.sheet(1)).toBeUndefined();
        expect(workbook.sheet("bar")).toBeUndefined();
    });
    test("Referencing cells", () => {
        const workbook = createWorkbook();
        const sheet = createSheet(workbook, "foo");
        sheet.setCell("A", 0, "42");
        sheet.setCell("A", 1, "=A0");
        expect(sheet.getCell("A", 1)).toEqual(["=A0", "42"]);
    });
});
