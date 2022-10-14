var thisSheet;
var workbook;

var createWorkbook = () => {
    return {
        __sheets: new Map(),
        sheet(nameOrIndex) {
            if (typeof nameOrIndex !== "number") return this.__sheets.get(nameOrIndex);
            for (const entry of this.__sheets) {
                if (nameOrIndex === 0) return entry[1];
                nameOrIndex--;
            }
            return undefined;
        },
    };
};

function toBijectiveBase(number) {
    const alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    number += 1;
    let c = 0;
    let x = 1;
    while (number >= x) {
        ++c;
        number -= x;
        x *= 26;
    }

    let s = "";
    for (let i = 0; i < c; i++) {
        s = alpha.charAt(number % 26) + s;
        number = Math.floor(number / 26);
    }

    return s;
}

function __evaluate(expression, that) {
    const object = Object.create(null);
    for (const entry of that.__cells) {
        const cell = JSON.parse(entry[0]);
        object[`${cell[0]}${cell[1]}`] = entry[1][1];
    }

    const sheetObject = that;
    let __value;

    // Warning: Dragons and fire ahead.
    with (that.__workbook) {
        with (object) {
            with (sheetObject) {
                __value = eval(expression);
            }
        }
    }
    return __value;
}

class Sheet {
    constructor(workbook) {
        this.__cells = new Map();
        this.__columns = new Set();
        this.__workbook = workbook;
        this.__currentCellPosition = undefined;
    }

    get_real_cell_contents(name) {
        const cell = this.parse_cell_name(name);
        if (cell === undefined) throw new TypeError("Invalid cell name");
        return this.getCell(cell.column, cell.row)[0];
    }

    set_real_cell_contents(name, value) {
        const cell = this.parse_cell_name(name);
        if (cell === undefined) throw new TypeError("Invalid cell name");

        this.setCell(cell.column, cell.row, value);
    }

    parse_cell_name(name) {
        const match = /^([a-zA-Z]+)(\d+)$/.exec(name);
        if (!Array.isArray(match)) return undefined;

        return {
            column: match[1],
            row: +match[2],
        };
    }

    current_cell_position() {
        return this.__currentCellPosition;
    }

    column_index(name) {
        let i = 0;
        for (const column of this.__columns) {
            if (column === name) return i;
            ++i;
        }
    }

    column_arithmetic(name, offset) {
        if (offset < 0) {
            const columns = this.getColumns();
            let index = columns.indexOf(name);
            if (index === -1) throw new TypeError(`${name} is not a valid column name`);

            index += offset;
            if (index < 0) return columns[0];
            return columns[index];
        }

        let found = false;
        for (const column of this.__columns) {
            if (!found) found = column === name;
            if (found) {
                if (offset === 0) return column;
                offset--;
            }
        }

        if (!found) throw new TypeError(`${name} is not a valid column name`);

        let newName;
        for (let i = 0; i < offset; ++i) {
            newName = toBijectiveBase(this.__columns.size);
            this.addColumn(newName);
        }
        return newName;
    }

    get_column_bound(name) {
        let bound = 0;
        for (const entry of this.__cells) {
            const [column, row] = JSON.parse(entry[0]);
            if (column !== name) continue;
            bound = Math.max(bound, row);
        }
        return bound;
    }

    evaluate(currentColumn, currentRow, expression) {
        const currentCellSave = this.__currentCellPosition;
        this.__currentCellPosition = { column: currentColumn, row: currentRow };
        try {
            return __evaluate(expression, this);
        } finally {
            this.__currentCellPosition = currentCellSave;
        }
    }

    addColumn(name) {
        this.__columns.add(name);
    }

    getColumns() {
        return Array.from(this.__columns);
    }

    setCell(column, row, source, value = undefined) {
        this.addColumn(column);
        source = `${source}`;
        if (value === undefined) {
            value = source;
            if (value[0] === "=") value = this.evaluate(column, row, value.substr(1));
        }

        this.__cells.set(JSON.stringify([column, row]), [source, value]);
        this[`${column}${row}`] = value;
    }

    getCell(column, row) {
        const data = this.__cells.get(JSON.stringify([column, row]));
        if (data === undefined) return undefined;
        return data;
    }

    focusCell(column, row) {
        this.__currentCellPosition = { column, row };
    }

    makeCurrent() {
        thisSheet = this;
        workbook = this.__workbook;
    }
}

var createSheet = (workbook, name) => {
    const sheet = new Sheet(workbook);
    workbook.__sheets.set(name, sheet);
    return sheet;
};
