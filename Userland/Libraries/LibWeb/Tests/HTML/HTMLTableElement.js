describe("HTMLTableElement", () => {
    loadLocalPage("Table.html");

    afterInitialPageLoad(page => {
        test("empty table attributes", () => {
            let table = page.document.getElementById("empty-table");
            expect(table).not.toBeNull();

            expect(table.caption).toBe(null);
            expect(table.tHead).toBe(null);
            expect(table.tFoot).toBe(null);

            expect(table.tBodies).toHaveLength(0);
            expect(table.rows).toHaveLength(0);
        });

        test("full table attributes", () => {
            let table = page.document.getElementById("full-table");
            expect(table).not.toBeNull();

            expect(table.caption.nodeName).toBe("CAPTION");
            expect(table.tHead.nodeName).toBe("THEAD");
            expect(table.tFoot.nodeName).toBe("TFOOT");

            expect(table.tBodies.length).toBe(1);
            expect(table.rows.length).toBe(3);
        });

        test("create/delete caption", () => {
            let table = page.document.createElement("table");
            expect(table).not.toBeNull();

            expect(table.caption).toBeNull();
            table.createCaption();
            expect(table.caption).not.toBeNull();
            table.deleteCaption();
            expect(table.caption).toBeNull();
        });

        test("create/delete thead", () => {
            let table = page.document.createElement("table");
            expect(table).not.toBeNull();

            expect(table.tHead).toBeNull();
            table.createTHead();
            expect(table.tHead).not.toBeNull();
            table.deleteTHead();
            expect(table.tHead).toBeNull();
        });

        test("create/delete tfoot", () => {
            let table = page.document.createElement("table");
            expect(table).not.toBeNull();

            expect(table.tFoot).toBeNull();
            table.createTFoot();
            expect(table.tFoot).not.toBeNull();
            table.deleteTFoot();
            expect(table.tFoot).toBeNull();
        });

        test("insert rows", () => {
            let table = page.document.createElement("table");
            expect(table).not.toBeNull();

            // We hardcode the default value in a few places, due to the WrapperGenerator's bug with default values
            const defaultValue = -1;

            expect(table.rows.length).toBe(0);

            // insertRow with an index > number of rows will throw
            expect(() => {
                table.insertRow(1);
            }).toThrow();

            // Inserting a row into an empty table will create a <tbody> and <tr>
            let rowFirst = table.insertRow(defaultValue);
            rowFirst.innerText = "row_first";
            expect(table.firstElementChild.nodeName).toBe("TBODY");
            expect(table.firstElementChild.firstElementChild.nodeName).toBe("TR");
            expect(table.firstElementChild.firstElementChild.innerText).toBe("row_first");

            for (let i = 0; i < 10; i++) {
                let row = table.insertRow(defaultValue);
                row.innerText = "row" + i;
            }
            expect(table.rows.length).toBe(11);

            // insertRow with the default value
            let rowDefault = table.insertRow(defaultValue);
            rowDefault.innerText = "row_default";
            expect(table.rows[table.rows.length - 1].innerText).toBe("row_default");
        });

        test("delete rows", () => {
            let table = page.document.createElement("table");
            expect(table).not.toBeNull();

            // We hardcode the default value in a few places, due to the WrapperGenerator's bug with default values
            const defaultValue = -1;

            // deleteRow with an index > number of rows will throw
            expect(table.deleteRow).toThrow();

            for (let i = 0; i < 10; i++) {
                let row = table.insertRow(defaultValue);
                row.innerText = "row" + i;
            }
            // deleteRow with with no argument will delete the last row
            expect(table.rows[table.rows.length - 1].innerText).toBe("row9");
            table.deleteRow(defaultValue);
            expect(table.rows[table.rows.length - 1].innerText).toBe("row8");

            // We can delete a row with a specific index
            expect(table.rows[5].innerText).toBe("row5");
            table.deleteRow(5);
            expect(table.rows[5].innerText).toBe("row6");
        });
    });
    waitForPageToLoad();
});
