"use strict";

const Break = {};

// FIXME: Figure out a way to document non-function entities too.
class Position {
    constructor(column, row, sheet) {
        this.column = column;
        this.row = row;
        this.sheet = sheet ?? thisSheet;
        this.name = `${column}${row}`;
    }

    get contents() {
        return this.sheet.get_real_cell_contents(this.name);
    }

    set contents(value) {
        value = `${value}`;
        this.sheet.set_real_cell_contents(this.name, value);
        return value;
    }

    static from_name(name) {
        let sheet = thisSheet;
        let obj = sheet.parse_cell_name(name);
        return new Position(obj.column, obj.row, sheet);
    }

    up(how_many) {
        how_many = how_many ?? 1;
        const row = Math.max(0, this.row - how_many);
        return new Position(this.column, row, this.sheet);
    }

    down(how_many) {
        how_many = how_many ?? 1;
        const row = Math.max(0, this.row + how_many);
        return new Position(this.column, row, this.sheet);
    }

    left(how_many) {
        how_many = how_many ?? 1;
        return new Position(
            this.sheet.column_arithmetic(this.column, -how_many),
            this.row,
            this.sheet
        );
    }

    right(how_many) {
        how_many = how_many ?? 1;
        return new Position(
            this.sheet.column_arithmetic(this.column, how_many),
            this.row,
            this.sheet
        );
    }

    range_up() {
        if (this.row === 0) throw new Error(`No cells above this cell`);
        const up_one = this.up(1);
        let current_point = up_one;
        for (
            let point = current_point.up(1);
            current_point.row !== 0 && point.value() !== "";
            point = current_point.up(1)
        )
            current_point = point;

        const sheetName = Object.is(this.sheet, thisSheet)
            ? ""
            : `sheet(${JSON.stringify(this.sheet.name)}):`;
        return R(sheetName + current_point.name + ":" + up_one.name);
    }

    range_down() {
        let down_one = this.down(1);
        let current_point = down_one;
        for (let point = current_point.down(1); point.value() !== ""; point = current_point.down(1))
            current_point = point;

        const sheetName = Object.is(this.sheet, thisSheet)
            ? ""
            : `sheet(${JSON.stringify(this.sheet.name)}):`;
        return R(sheetName + current_point.name + ":" + down_one.name);
    }

    range_left() {
        if (this.column === "A") throw new Error(`No cells to the left of this cell`);
        const left_one = this.left(1);
        let current_point = left_one;
        for (
            let point = current_point.left(1);
            current_point.column !== "A" && point.value() !== "";
            point = current_point.left(1)
        )
            current_point = point;

        const sheetName = Object.is(this.sheet, thisSheet)
            ? ""
            : `sheet(${JSON.stringify(this.sheet.name)}):`;
        return R(sheetName + current_point.name + ":" + left_one.name);
    }

    range_right() {
        let right_one = this.right(1);
        let current_point = right_one;
        for (
            let point = current_point.right(1);
            point.value() !== "";
            point = current_point.right(1)
        )
            current_point = point;

        const sheetName = Object.is(this.sheet, thisSheet)
            ? ""
            : `sheet(${JSON.stringify(this.sheet.name)}):`;
        return R(sheetName + current_point.name + ":" + right_one.name);
    }

    with_column(value) {
        return new Position(value, this.row, this.sheet);
    }

    with_row(value) {
        return new Position(this.column, value, this.sheet);
    }

    in_sheet(the_sheet) {
        return new Position(this.column, this.row, sheet(the_sheet));
    }

    value() {
        return this.sheet[this.name];
    }

    valueOf() {
        return value();
    }

    toString() {
        return `<Cell at ${this.name}${
            Object.is(this.sheet, thisSheet) ? "" : ` in sheet(${JSON.stringify(this.sheet.name)})`
        }>`;
    }
}

class CommonRange {
    at(wantedIx) {
        let ix = 0;
        let found = null;
        this.forEach(cell => {
            if (ix++ === wantedIx) {
                found = cell;
                return Break;
            }
        });
        return found;
    }

    findIndex(matcher) {
        let i = 0;
        let found = false;
        this.forEach(cell => {
            if (matcher(cell, i)) {
                found = true;
                return Break;
            }
            ++i;
        });
        return found ? i : -1;
    }

    find(matcher) {
        let value = null;
        let i = 0;
        this.forEach(cell => {
            if (matcher(cell, i)) {
                value = cell;
                return Break;
            }
            ++i;
        });
        return value;
    }

    indexOf(name) {
        let i = 0;
        let found = false;
        this.forEach(cell => {
            if (cell.name === name) {
                found = true;
                return Break;
            }
            ++i;
        });
        return found ? i : -1;
    }

    has(name) {
        return this.indexOf(name) !== -1;
    }

    toArray() {
        const cells = [];
        this.forEach(val => cells.push(val));
        return cells;
    }

    filter(matches) {
        const cells = [];
        this.forEach(cell => {
            if (matches(cell)) cells.push(cell);
        });
        return new SplitRange(cells);
    }

    unique() {
        const cells = [];
        const values = new Set();
        this.forEach(cell => {
            const value = cell.value();
            if (!values.has(value)) {
                values.add(value);
                cells.push(cell);
            }
        });
        return new SplitRange(cells);
    }
}

class SplitRange extends CommonRange {
    constructor(cells) {
        super();
        this.cells = cells;
    }

    static fromNames(...cellNames) {
        return new SplitRange(cellNames.map(Position.from_name));
    }

    first() {
        return this.cellNames[0];
    }

    forEach(callback) {
        for (const cell of this.cells) {
            if (callback(cell) === Break) return;
        }
    }

    toString() {
        const namesFormatted = this.cells.map(cell => '"' + cell.name + '"').join(", ");
        return `SplitRange.fromNames(${namesFormatted})`;
    }
}

class Ranges extends CommonRange {
    constructor(ranges) {
        super();
        this.ranges = ranges;
    }

    first() {
        return this.ranges[0].first();
    }

    static from(...ranges) {
        return new Ranges(ranges);
    }

    forEach(callback) {
        for (const range of this.ranges) {
            if (range.forEach(callback) === Break) break;
        }
    }

    union(other, direction = "right") {
        if (direction === "left") {
            if (other instanceof Ranges) return Ranges.from(...other.ranges, ...this.ranges);
            return Ranges.from(other, ...this.ranges);
        } else if (direction === "right") {
            if (other instanceof Ranges) return Ranges.from(...this.ranges, ...other.ranges);
            return Ranges.from(...this.ranges, other);
        } else {
            throw new Error(`Invalid direction '${direction}'`);
        }
    }

    toString() {
        return `Ranges.from(${this.ranges.map(r => r.toString()).join(", ")})`;
    }
}

class Range extends CommonRange {
    constructor(
        startingColumnName,
        endingColumnName,
        startingRow,
        endingRow,
        columnStep,
        rowStep,
        sheet
    ) {
        super();
        // using == to account for '0' since js will parse `+'0'` to 0
        if (columnStep == 0 || rowStep == 0)
            throw new Error("rowStep or columnStep is 0, this will cause an infinite loop");
        if (typeof startingRow === "string" || typeof endingRow === "string")
            throw new Error(
                "startingRow or endingRow is a string, this will cause an infinite loop"
            );
        this.startingColumnName = startingColumnName;
        this.endingColumnName = endingColumnName;
        this.startingRow = startingRow;
        this.endingRow = endingRow;
        this.columnStep = columnStep ?? 1;
        this.rowStep = rowStep ?? 1;
        this.spansEntireColumn = endingRow === undefined;
        this.sheet = sheet;
        if (!this.spansEntireColumn && startingRow === undefined)
            throw new Error("A Range with a defined end row must also have a defined start row");

        this.normalize();
    }

    first() {
        return new Position(this.startingColumnName, this.startingRow, this.sheet);
    }

    forEach(callback) {
        const ranges = [];
        let startingColumnIndex = this.sheet.column_index(this.startingColumnName);
        let endingColumnIndex = this.sheet.column_index(this.endingColumnName);
        let columnDistance = endingColumnIndex - startingColumnIndex;
        for (
            let columnOffset = 0;
            columnOffset <= columnDistance;
            columnOffset += this.columnStep
        ) {
            const columnName = this.sheet.column_arithmetic(this.startingColumnName, columnOffset);
            ranges.push({
                column: columnName,
                rowStart: this.startingRow,
                rowEnd: this.spansEntireColumn
                    ? this.sheet.get_column_bound(columnName)
                    : this.endingRow,
            });
        }

        outer: for (const range of ranges) {
            for (let row = range.rowStart; row <= range.rowEnd; row += this.rowStep) {
                if (callback(new Position(range.column, row, this.sheet)) === Break) break outer;
            }
        }
    }

    union(other) {
        if (other instanceof Ranges) return other.union(this, "left");

        if (other instanceof Range) return Ranges.from(this, other);

        throw new Error(`Cannot add ${other} to a Range`);
    }

    normalize() {
        const startColumnIndex = this.sheet.column_index(this.startingColumnName);
        const endColumnIndex = this.sheet.column_index(this.endingColumnName);
        if (startColumnIndex > endColumnIndex) {
            const temp = this.startingColumnName;
            this.startingColumnName = this.endingColumnName;
            this.endingColumnName = temp;
        }

        if (this.startingRow !== undefined && this.endingRow !== undefined) {
            if (this.startingRow > this.endingRow) {
                const temp = this.startingRow;
                this.startingRow = this.endingRow;
                this.endingRow = temp;
            }
        }
    }

    toString() {
        const endingRow = this.endingRow ?? "";
        const showSteps = this.rowStep !== 1 || this.columnStep !== 1;
        const steps = showSteps ? `:${this.columnStep}:${this.rowStep}` : "";
        const sheetName = Object.is(thisSheet, this.sheet)
            ? ""
            : `sheet(${JSON.stringify(this.sheet.name)}):`;
        return `R\`${sheetName}${this.startingColumnName}${this.startingRow}:${this.endingColumnName}${endingRow}${steps}\``;
    }
}

const R_FORMAT =
    /^(?:sheet\(("(?:[^"]|\\")*")\):)?([a-zA-Z_]+)(?:(\d+):([a-zA-Z_]+)(\d+)?(?::(\d+):(\d+))?)?$/;
function R(fmt, ...args) {
    if (args.length !== 0) throw new TypeError("R`` format must be a literal");
    // done because:
    // const myFunc = xyz => JSON.stringify(xyz)
    // myFunc("ABC") => ""ABC""
    // myFunc`ABC` => "["ABC"]"
    if (Array.isArray(fmt)) fmt = fmt[0];
    if (!R_FORMAT.test(fmt))
        throw new Error(
            'Invalid Format. Expected Format: R`A` or R`A0:A1` or R`A0:A2:1:2` or R`sheet("sheetName"):...`'
        );
    // Format: (sheet("sheetName"):)?Col(Row:Col(Row)?(:ColStep:RowStep)?)?
    // Ignore the first element of the match array as that will be the whole match.
    const [, ...matches] = fmt.match(R_FORMAT);
    const [sheetExpression, startCol, startRow, endCol, endRow, colStep, rowStep] = matches;
    const sheetFromName = name => {
        if (name == null || name === "") return thisSheet;
        return sheet(JSON.parse(name));
    };
    return new Range(
        startCol,
        endCol ?? startCol,
        integer(startRow ?? 0),
        // Don't make undefined an integer, because then it becomes 0.
        !!endRow ? integer(endRow) : endRow,
        integer(colStep ?? 1),
        integer(rowStep ?? 1),
        sheetFromName(sheetExpression)
    );
}

function select(criteria, t, f) {
    if (criteria) return t;
    return f;
}

function choose(index, ...args) {
    if (index > args.length) return undefined;
    if (index < 0) return undefined;
    return args[index];
}

function now() {
    return new Date();
}

function repeat(count, str) {
    return Array(count + 1).join(str);
}

function randRange(min, max) {
    return Math.random() * (max - min) + min;
}

function integer(value) {
    const typeVal = typeof value;
    if ((typeVal !== "number" && typeVal !== "string") || Number.isNaN(Number(value)))
        throw new Error(`integer() called with unexpected type "${typeVal}"`);
    return value | 0;
}

function sheet(name) {
    return workbook.sheet(name);
}

function reduce(op, accumulator, cells) {
    return resolve(cells).reduce(op, accumulator);
}

function numericReduce(op, accumulator, cells) {
    return numericResolve(cells).reduce(op, accumulator);
}

function numericResolve(cells) {
    return resolve(cells).map(val => parseFloat(val));
}

function resolve(cells) {
    if (!(cells instanceof Array)) {
        cells = [cells];
    }
    return cells.map(resolveRange).flat();
}

function resolveRange(cells) {
    const isRange = cells instanceof CommonRange;
    return isRange ? cells.toArray().map(cell => cell.value()) : cells;
}

// Statistics

function sum(...cells) {
    return numericReduce((acc, x) => acc + x, 0, cells);
}

function sumIf(condition, ...cells) {
    return numericReduce((acc, x) => (condition(x) ? acc + x : acc), 0, cells);
}

function count(...cells) {
    return reduce((acc, x) => acc + 1, 0, cells);
}

function countIf(condition, ...cells) {
    return reduce((acc, x) => (condition(x) ? acc + 1 : acc), 0, cells);
}

function average(...cells) {
    const sumAndCount = numericReduce((acc, x) => [acc[0] + x, acc[1] + 1], [0, 0], cells);
    return sumAndCount[0] / sumAndCount[1];
}

function averageIf(condition, ...cells) {
    const sumAndCount = numericReduce(
        (acc, x) => (condition(x) ? [acc[0] + x, acc[1] + 1] : acc),
        [0, 0],
        cells
    );
    return sumAndCount[0] / sumAndCount[1];
}

function maxIf(condition, ...cells) {
    return Math.max(...numericResolve(cells).filter(condition));
}

function max(...cells) {
    return maxIf(() => true, ...cells);
}

function minIf(condition, ...cells) {
    return Math.min(...numericResolve(cells).filter(condition));
}

function min(...cells) {
    return minIf(() => true, ...cells);
}

function sumProductIf(condition, rangeOne, rangeTwo) {
    const rangeOneNums = numericResolve(rangeOne);
    const rangeTwoNums = numericResolve(rangeTwo);
    return rangeOneNums.reduce((accumulator, curr, i) => {
        const prod = curr * rangeTwoNums[i];
        if (!condition(curr, rangeTwoNums[i], prod)) return accumulator;
        return accumulator + prod;
    }, 0);
}

function sumProduct(rangeOne, rangeTwo) {
    return sumProductIf(() => true, rangeOne, rangeTwo);
}

function median(...cells) {
    const values = numericResolve(cells);

    if (values.length === 0) return 0;

    function qselect(arr, idx) {
        if (arr.length === 1) return arr[0];

        const pivot = arr[0];
        const ls = arr.filter(x => x < pivot);
        const hs = arr.filter(x => x > pivot);
        const eqs = arr.filter(x => x === pivot);

        if (idx < ls.length) return qselect(ls, k);

        if (idx < ls.length + eqs.length) return pivot;

        return qselect(hs, idx - ls.length - eqs.length);
    }

    if (values.length % 2) return qselect(values, values.length / 2);

    return (qselect(values, values.length / 2) + qselect(values, values.length / 2 - 1)) / 2;
}

function variance(...cells) {
    const sumsAndSquaresAndCount = numericReduce(
        (acc, x) => [acc[0] + x, acc[1] + x * x, acc[2] + 1],
        [0, 0, 0],
        cells
    );
    let sums = sumsAndSquaresAndCount[0];
    let squares = sumsAndSquaresAndCount[1];
    let count = sumsAndSquaresAndCount[2];

    return (count * squares - sums * sums) / count;
}

function mode(...cells) {
    const counts = numericReduce(
        (map, x) => {
            if (!map.has(x)) map.set(x, 0);
            map.set(x, map.get(x) + 1);
            return map;
        },
        new Map(),
        cells
    );

    let mostCommonValue = undefined;
    let mostCommonCount = -1;
    counts.forEach((count, value) => {
        if (count > mostCommonCount) {
            mostCommonCount = count;
            mostCommonValue = value;
        }
    });

    return mostCommonValue;
}

function stddev(...cells) {
    return Math.sqrt(variance(...cells));
}

// Lookup

function row() {
    return thisSheet.current_cell_position().row;
}

function column() {
    return thisSheet.current_cell_position().column;
}

function here() {
    const position = thisSheet.current_cell_position();
    return new Position(position.column, position.row, thisSheet);
}

function internal_lookup(
    req_lookup_value,
    lookup_inputs,
    lookup_outputs,
    if_missing,
    mode,
    reference
) {
    if_missing = if_missing ?? undefined;
    const missing = () => {
        if (if_missing !== undefined) return if_missing;

        throw new Error(`Failed to find ${req_lookup_value} in ${lookup_inputs}`);
    };

    mode = mode ?? "exact";
    const lookup_value = req_lookup_value;
    let matches = null;

    if (mode === "exact") {
        matches = value => value === lookup_value;
    } else if (mode === "nextlargest") {
        matches = value => value >= lookup_value;
    } else if (mode === "nextsmallest") {
        matches = value => value <= lookup_value;
    } else {
        throw new Error(`Match mode '${mode}' not supported`);
    }

    let i = 0;
    let value = null;
    let found_input = null;
    lookup_inputs.forEach(cell => {
        value = cell.value();
        if (matches(value)) {
            found_input = cell;
            return Break;
        }
        ++i;
    });

    if (found_input == null) return missing();

    if (lookup_outputs === undefined) {
        if (reference) return found_input;

        return value;
    }

    const found_output = lookup_outputs.at(i);

    if (found_output == null)
        throw new Error("Lookup target length must not be smaller than lookup source length");

    if (reference) return found_output;

    return found_output.value();
}

function lookup(req_lookup_value, lookup_inputs, lookup_outputs, if_missing, mode) {
    return internal_lookup(
        req_lookup_value,
        lookup_inputs,
        lookup_outputs,
        if_missing,
        mode,
        false
    );
}

function reflookup(req_lookup_value, lookup_inputs, lookup_outputs, if_missing, mode) {
    return internal_lookup(req_lookup_value, lookup_inputs, lookup_outputs, if_missing, mode, true);
}

// Cheat the system and add documentation
R.__documentation = JSON.stringify({
    name: "R",
    argc: 1,
    argnames: ["range specifier"],
    doc:
        "Generates a Range object, from the given" +
        "range specifier, which must conform to the syntax shown below",
    examples: {
        "R`A`": "Generate a Range representing all the cells in the column A",
        "R`A:C`": "Generate a Range representing all the cells in the columns A through C",
        "R`A:C:2:2`":
            "Generate a Range representing every other cells in every other column in A through C",
        "R`A1:C4`":
            "Generate a Range representing all cells in a rectangle with the top-left cell A1, and the bottom-right cell C4",
        "R`A0:B10:1:2`":
            "Generate a Range representing all cells in a rectangle with the top-left cell A1, and the bottom-right cell C4, with every column, and skipping every other row",
    },
});

select.__documentation = JSON.stringify({
    name: "select",
    argc: 3,
    argnames: ["criteria", "true value", "false value"],
    doc: "Selects between the two `true` and `false` values based on the value of `criteria`",
    examples: {
        "select(A1, A2, A3)": "Evaluates to A2 if A1 is true, A3 otherwise",
    },
});

choose.__documentation = JSON.stringify({
    name: "choose",
    argc: 1,
    argnames: ["index"],
    doc: "Selects an argument by the given `index`, starting at zero",
    examples: {
        "choose(A3, 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat')":
            "Get the day name by the number in A3",
        "choose(randRange(0, 2), 'Well', 'Hello', 'Friends')":
            "Randomly pick one of the three words 'well', 'hello' and 'friends'",
    },
});

now.__documentation = JSON.stringify({
    name: "now",
    argc: 0,
    argnames: [],
    doc: "Returns a Date instance for the current moment",
    examples: {
        "now().toString()":
            "Returns a string containing the current date. Ex: 'Tue Sep 21 2021 02:38:10 GMT+0000 (UTC)'",
        "now().getFullYear()": "Returns the current year. Ex: 2021",
    },
});

repeat.__documentation = JSON.stringify({
    name: "repeat",
    argc: 2,
    argnames: ["string", "count"],
    doc: "Returns a string equivalent to `string` repeated `count` times",
    examples: {
        'repeat("a", 10)': 'Generates the string "aaaaaaaaaa"',
    },
});

randRange.__documentation = JSON.stringify({
    name: "randRange",
    argc: 2,
    argnames: ["start", "end"],
    doc: "Returns a random number in the range (`start`, `end`)",
    examples: {
        "randRange(0, 10)": "Returns a number from 0 through 10. Ex: 5.185799582250052",
    },
});

integer.__documentation = JSON.stringify({
    name: "integer",
    argc: 1,
    argnames: ["value"],
    doc: "Returns the integer value of `value`",
    examples: {
        "A1 = integer(A0)": "Sets the value of the cell A1 to the integer value of the cell A0",
    },
});

sheet.__documentation = JSON.stringify({
    name: "sheet",
    argc: 1,
    argnames: ["name or index"],
    doc: "Returns a reference to another sheet, identified by _name_ or _index_",
    examples: {
        "sheet('Sheet 1').A4": "Read the value of the cell A4 in a sheet named 'Sheet 1'",
        "sheet(0).A0 = 123": "Set the value of the cell A0 in the first sheet to 123",
    },
});

reduce.__documentation = JSON.stringify({
    name: "reduce",
    argc: 3,
    argnames: ["reduction function", "accumulator", "cells"],
    doc:
        "Reduces the entries in `cells` with repeated applications of the `reduction function` " +
        "to the `accumulator`\n The `reduction function` should be a function of arity 2, taking " +
        "first the accumulator, then the current value, and returning the new accumulator value\n\n" +
        "Please keep in mind that this function respects the cell type, and can yield non-numeric " +
        "values to the `current value`.",
    examples: {
        "reduce((acc, x) => acc * x, 1, R`A0:A5`)":
            "Calculate the product of all values in the range A0:A5",
    },
});

numericReduce.__documentation = JSON.stringify({
    name: "numericReduce",
    argc: 3,
    argnames: ["reduction function", "accumulator", "cells"],
    doc:
        "Reduces the entries in `cells` with repeated applications of the `reduction function` to the " +
        "`accumulator`\n The `reduction function` should be a function of arity 2, taking first the " +
        "accumulator, then the current value, and returning the new accumulator value\n\nThis function, " +
        "unlike [`reduce`](spreadsheet://doc/reduce), casts the values to a number before passing them to the `reduction function`.",
    examples: {
        "numericReduce((acc, x) => acc * x, 1, R`A0:A5`)":
            "Calculate the numeric product of all values in the range A0:A5",
    },
});

sum.__documentation = JSON.stringify({
    name: "sum",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the total of the numbers or cell values in `numbers or cell names`",
    examples: {
        "sum(R`A0:C3`)":
            "Calculate the sum of the values in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
        "sum(1, 2, 3)": "Calculate the sum of 1, 2, and 3 (Sum = 6)",
    },
});

sumIf.__documentation = JSON.stringify({
    name: "sumIf",
    argc: 2,
    argnames: ["condition", "numbers or cell names"],
    doc: "Calculates the sum of all numbers or cell values which evaluate to true when passed to `condition`",
    examples: {
        "sumIf(x => x instanceof Number, R`A1:C4`)":
            "Calculates the sum of all numbers within A1:C4",
    },
});

count.__documentation = JSON.stringify({
    name: "count",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Counts the number of inputs or cells in a given range",
    examples: {
        "count(R`A0:C3`)":
            "Count the number of cells in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
    },
});

countIf.__documentation = JSON.stringify({
    name: "countIf",
    argc: 2,
    argnames: ["condition", "numbers or cell names"],
    doc: "Counts inputs or cell values which evaluate to true when passed to `condition`",
    examples: {
        "countIf(x => x instanceof Number, R`A1:C3`)":
            "Count the number of cells which have numbers within A1:C3",
    },
});

average.__documentation = JSON.stringify({
    name: "average",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the average of the numbers or cell values in `numbers or cell names`",
    examples: {
        "average(R`A0:C3`)":
            "Calculate the average of the values in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
        "average(4, 6)": "Calculate the average of 4 and 6 (Average = 5)",
    },
});

averageIf.__documentation = JSON.stringify({
    name: "averageIf",
    argc: 2,
    argnames: ["condition", "numbers or cell names"],
    doc: "Calculates the average of all numbers or cell values which evaluate to true when passed to `condition`",
    examples: {
        "averageIf(x => x > 4, R`A1:C4`)":
            "Calculate the average of all numbers larger then 4 within A1:C4",
    },
});

max.__documentation = JSON.stringify({
    name: "max",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the largest number or cell value in `numbers or cell names`",
    examples: {
        "max(R`A1:C4`)": "Finds the largest number within A1:C4",
        "max(1, 2, 3)": "Returns the largest of 1, 2, and 3 (Max = 3)",
    },
});

maxIf.__documentation = JSON.stringify({
    name: "maxIf",
    argc: 1,
    argnames: ["condition", "numbers or cell names"],
    doc: "Calculates the largest of all numbers or cell values which evaluate to true when passed to `condition`",
    examples: {
        "maxIf(x => x > 4, R`A1:C4`)":
            "Finds the largest number within A1:C4 that is greater than 4",
    },
});

min.__documentation = JSON.stringify({
    name: "min",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the smallest number or cell value in `numbers or cell names`",
    examples: {
        "min(R`A1:C4`)": "Finds the smallest number within A1:C4",
        "min(1, 2, 3)": "Returns the smallest of 1, 2, and 3 (Min = 1)",
    },
});

minIf.__documentation = JSON.stringify({
    name: "minIf",
    argc: 1,
    argnames: ["condition", "numbers or cell names"],
    doc: "Calculates the smallest of all numbers or cell values which evaluate to true when passed to `condition`",
    examples: {
        "minIf(x => x > 4, R`A1:C4`)":
            "Finds the smallest number within A1:C4 that is greater than 4",
    },
});

sumProduct.__documentation = JSON.stringify({
    name: "sumProduct",
    argc: 2,
    argnames: ["range one", "range two"],
    doc: "For each cell in the first range, multiply it by the cell at the same index in range two, then add the result to a sum",
    example_data: {
        "sumProductIf((a, b, prod) => a > 2, R`A0:A`, R`B0:B`)":
            "Calculate the product of each cell in a times it's equivalent cell in b, then adds the products, [Click to view](spreadsheet://example/sumProductIf#sum_product)",
    },
});

sumProductIf.__documentation = JSON.stringify({
    name: "sumProductIf",
    argc: 3,
    argnames: ["condition", "range one", "range two"],
    doc: "For each cell in the first range, multiply it by the cell at the same index in range two, then add the result to a sum, if the condition evaluated to true",
    examples: {
        "sumProductIf((a, b, prod) => a > 2, R`A0:A`, R`B0:B`)":
            "Calculate the product of each cell in a times it's equivalent cell in b, then adds the products if a's value was greater than 2, [Click to view](spreadsheet://example/sumProductIf#sum_product)",
    },
    example_data: {
        sum_product: {
            name: "Sum Product",
            columns: ["A", "B", "C"],
            rows: 3,
            cells: {
                C0: {
                    kind: "Formula",
                    source: "sumProduct(R`A0:A`, R`B0:B`)",
                    value: "300.0",
                    type: "Numeric",
                    type_metadata: {
                        format: "sumProduct: %f",
                    },
                },
                C1: {
                    kind: "Formula",
                    source: "sumProductIf((a, b, prod) => a > 2, R`A0:A`, R`B0:B`)",
                    value: "250.0",
                    type: "Numeric",
                    type_metadata: {
                        format: "sumProductIf: %f",
                    },
                },
                ...Array.apply(null, { length: 4 })
                    .map((_, i) => i)
                    .reduce((acc, i) => {
                        return {
                            ...acc,
                            [`A${i}`]: {
                                kind: "LiteralString",
                                value: `${i + 1}`,
                                type: "Numeric",
                            },
                            [`B${i}`]: {
                                kind: "LiteralString",
                                value: `${(i + 1) * 10}`,
                                type: "Numeric",
                            },
                        };
                    }, {}),
            },
        },
    },
});

median.__documentation = JSON.stringify({
    name: "median",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the median number or cell value in `numbers or cell names`",
    examples: {
        "median(R`A0:C3`)":
            "Calculate the median of the values in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
        "median(1, 2, 5)": "Calculate the median of 1, 2, and 5 (Median = 2)",
    },
});

variance.__documentation = JSON.stringify({
    name: "variance",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the variance of the numbers or cell values in `numbers or cell names`",
    examples: {
        "variance(R`A0:C3`)":
            "Calculate the variance of the values in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
    },
    example_data: {
        simple: {
            name: "Simple Statistics",
            columns: ["A", "B", "C", "D", "E"],
            rows: 6,
            cells: {
                E0: {
                    kind: "Formula",
                    source: "stddev(R`A0:C3`)",
                    value: "5.329165",
                    type: "Numeric",
                    type_metadata: {
                        format: "stddev: %f",
                    },
                },
                E1: {
                    kind: "Formula",
                    source: "variance(R`A0:C3`)",
                    value: "28.39999999",
                    type: "Numeric",
                    type_metadata: {
                        format: "variance: %f",
                    },
                },
                E2: {
                    kind: "Formula",
                    source: "median(R`A0:C3`)",
                    value: "1",
                    type: "Numeric",
                    type_metadata: {
                        format: "median: %f",
                    },
                },
                E3: {
                    kind: "Formula",
                    source: "average(R`A0:C3`)",
                    value: "1.1999999",
                    type: "Numeric",
                    type_metadata: {
                        format: "average: %f",
                    },
                },
                E4: {
                    kind: "Formula",
                    source: "mode(R`A0:C3`)",
                    value: "1",
                    type: "Numeric",
                    type_metadata: {
                        format: "mode: %d",
                    },
                },
                E5: {
                    kind: "Formula",
                    source: "count(R`A0:C3`)",
                    value: "12",
                    type: "Numeric",
                    type_metadata: {
                        format: "count: %d",
                    },
                },
                E6: {
                    kind: "Formula",
                    source: "sum(R`A0:C3`)",
                    value: "18",
                    type: "Numeric",
                    type_metadata: {
                        format: "sum: %d",
                    },
                },
                ...Array.apply(null, { length: 4 })
                    .map((_, i) => i)
                    .reduce((acc, i) => {
                        return {
                            ...acc,
                            [`A${i}`]: {
                                kind: "LiteralString",
                                value: `${i}`,
                                type: "Numeric",
                            },
                            [`B${i}`]: {
                                kind: "LiteralString",
                                value: `${i + 1}`,
                                type: "Numeric",
                            },
                            [`C${i}`]: {
                                kind: "LiteralString",
                                value: `${i - 1}`,
                                type: "Numeric",
                            },
                        };
                    }, {}),
            },
        },
    },
});

mode.__documentation = JSON.stringify({
    name: "mode",
    argc: 1,
    argnames: ["numbers or cell names"],
    doc: "Calculates the mode (most common value) of the numbers or cell values in `numbers or cell names`",
    examples: {
        "mode(R`A2:A14`)":
            "Calculate the mode of the values in A2:A14, [Click to view](spreadsheet://example/variance#simple)",
        "mode(1, 2, 2)": "Calculate the mode of 1, 2, and 2 (Mode = 2)",
    },
});

stddev.__documentation = JSON.stringify({
    name: "stddev",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the standard deviation (square root of variance) of the numbers or cell values in `numbers or cell names`",
    examples: {
        "stddev(R`A0:C3`)":
            "Calculate the standard deviation of the values in A0:C3, [Click to view](spreadsheet://example/variance#simple)",
    },
});

row.__documentation = JSON.stringify({
    name: "row",
    argc: 0,
    argnames: [],
    doc: "Returns the row number of the current cell",
    examples: {
        "row()": "Evaluates to 6 if placed in A6",
    },
});

column.__documentation = JSON.stringify({
    name: "column",
    argc: 0,
    argnames: [],
    doc: "Returns the column name of the current cell",
    examples: {
        "column()": "Evaluates to A if placed in A6",
    },
});

here.__documentation = JSON.stringify({
    name: "here",
    argc: 0,
    argnames: [],
    doc:
        "Returns an object representing the current cell's position, see `Position` below.\n\n" +
        "## Position\na `Position` is an object representing a given cell position in a given sheet.\n" +
        "### Methods:\n" +
        "- `up(count = 1)`: goes up count cells, or returns the top position if at the top\n" +
        "- `down(count = 1)`: goes down count cells\n" +
        "- `left(count = 1)`: Goes left count cells, or returns the leftmost position if the edge\n" +
        "- `right(count = 1)`: Goes right count cells.\n" +
        "- `range_up()`: make a range from the cell above this cell, upward, until there is a cell with no number in it.\n" +
        "- `range_down()`: make a range from the cell below this cell, downward, until there is a cell with no number in it.\n" +
        "- `range_left()`: make a range from the cell to the left of this cell, going left, until there is a cell with no number in it.\n" +
        "- `range_right()`: make a range from the cell to the right of this cell, going right, until there is a cell with no number in it.\n" +
        "- `with_row(row)`: Returns a Position with its column being this object's, and its row being the provided the value.\n" +
        "- `with_column(column)`: Similar to `with_row()`, but changes the column instead.\n" +
        "- `in_sheet(the_sheet)`: Returns a Position with the same column and row as this one, but with its sheet being `the_sheet`.\n" +
        "- `value()`: Returns the value at the position which it represents, in the object's sheet (current sheet by default).\n" +
        "- `contents`: An accessor for the real contents of the cell (i.e. the text as typed in the cell editor)\n",
    examples: {
        "here().up().value()": "Get the value of the cell above this one",
        "here().up().with_column('A')":
            "Get a Position above this one in column A, for instance, evaluates to A2 if run in B3, [Click to view](spreadsheet://example/here#with_column)",
    },
    example_data: {
        with_column: {
            name: "here() With Column",
            columns: ["A", "B"],
            rows: 4,
            cells: {
                B3: {
                    kind: "Formula",
                    source: "here().up().with_column('A').name",
                    value: '"A2"',
                    type: "Identity",
                },
            },
        },
    },
});

lookup.__documentation = JSON.stringify({
    name: "lookup",
    argc: 2,
    argnames: [
        "lookup value",
        "lookup source",
        "lookup target",
        "value if no match",
        "match method",
    ],
    doc:
        "Allows for finding things in a table or tabular data, by looking for matches in one range, and " +
        "grabbing the corresponding output value from another range.\n" +
        "if `lookup target` is not specified or is nullish, it is assumed to be the same as the `lookup source`\n." +
        "if nothing matches, either the value `value if no match` (if not `undefined`) is returned, or " +
        "an error is thrown.\nBy setting the `match method`, the function can be altered to return " +
        "the closest ordered value (above or below) instead of an exact match. The valid choices for `match method` are:\n" +
        "- `'exact'`: The default method. Uses strict equality to match values.\n" +
        "- `'nextlargest'`: Uses the greater-or-equal operator to match values.\n" +
        "- `'nextsmallest'`: Uses the less-than-or-equal operator to match values.\n",
    examples: {
        "lookup(F3, R`B2:B11`, R`D2:D11`)":
            "Look for the value of F3 in the range B2:B11, and return the corresponding value from the D column",
        "lookup(E2, R`C2:C5`, R`B2:B5`, 0, 'nextlargest')":
            "Find the closest (larger) value to E2 in range C2:C5, and evaluate to 0 if no value in that range is larger.",
    },
});

reflookup.__documentation = JSON.stringify({
    name: "reflookup",
    argc: 2,
    argnames: [
        "lookup value",
        "lookup source",
        "lookup target",
        "value if no match",
        "match method",
    ],
    doc:
        "Allows for finding references to things in a table or tabular data, by looking for matches in one range, and " +
        "grabbing the corresponding output value from another range.\n" +
        "if `lookup target` is not specified or is nullish, it is assumed to be the same as the `lookup source`\n." +
        "if nothing matches, either the value `value if no match` (if not `undefined`) is returned, or " +
        "an error is thrown.\nBy setting the `match method`, the function can be altered to return " +
        "the closest ordered value (above or below) instead of an exact match. The valid choices for `match method` are:\n" +
        "- `'exact'`: The default method. Uses strict equality to match values.\n" +
        "- `'nextlargest'`: Uses the greater-or-equal operator to match values.\n" +
        "- `'nextsmallest'`: Uses the less-than-or-equal operator to match values.\n" +
        "\nThis function return a `Position` (see [`here()`](spreadsheet://doc/here))",
    examples: {
        "reflookup(A0, R`B1:B5`, R`C1:C5`)":
            "Look for the value of A0 in the range B1:B5, and return the corresponding cell name from the C column," +
            "[Click to view](spreadsheet://example/reflookup#simple)",
        "reflookup(A0, R`C2:C5`, R`B2:B5`, here(), 'nextlargest')":
            "Find the cell with the closest (larger) value to A0 in range C2:C5, and give the corresponding cell in range C1:C5, " +
            "evaluating to the current cell if no value in that range is larger, [Click to view](spreadsheet://example/reflookup#nextlargest)",
    },
    example_data: {
        simple: {
            name: "Simple",
            columns: ["A", "B", "C"],
            rows: 6,
            cells: {
                B1: {
                    kind: "LiteralString",
                    value: "1",
                },
                B0: {
                    kind: "Formula",
                    source: "reflookup(A0, R`B1:B5`, R`C1:C5`).value()",
                    value: '"C"',
                    type: "Identity",
                },
                C3: {
                    kind: "LiteralString",
                    value: "C",
                    type: "Identity",
                },
                C2: {
                    kind: "LiteralString",
                    value: "B",
                    type: "Identity",
                },
                B2: {
                    kind: "LiteralString",
                    value: "2",
                },
                C4: {
                    kind: "LiteralString",
                    value: "D",
                    type: "Identity",
                },
                A0: {
                    kind: "LiteralString",
                    value: "3",
                },
                C1: {
                    kind: "LiteralString",
                    value: "A",
                    type: "Identity",
                },
                C5: {
                    kind: "LiteralString",
                    value: "E",
                    type: "Identity",
                },
                B3: {
                    kind: "LiteralString",
                    value: "3",
                },
                B5: {
                    kind: "LiteralString",
                    value: "5",
                },
                B4: {
                    kind: "LiteralString",
                    value: "4",
                },
            },
        },
        nextlargest: {
            name: "Next Largest",
            columns: ["A", "B", "C"],
            rows: 6,
            cells: {
                B0: {
                    kind: "Formula",
                    source: "reflookup(A0, R`C2:C5`, R`B2:B5`, here(), 'nextlargest').name",
                    value: '"B2"',
                    type: "Identity",
                },
                C3: {
                    kind: "LiteralString",
                    value: "3",
                },
                C2: {
                    kind: "LiteralString",
                    value: "2",
                },
                B2: {
                    kind: "LiteralString",
                    value: "B",
                    type: "Identity",
                },
                C4: {
                    kind: "LiteralString",
                    value: "4",
                },
                A0: {
                    kind: "LiteralString",
                    value: "1",
                },
                C5: {
                    kind: "LiteralString",
                    value: "5",
                },
                B3: {
                    kind: "LiteralString",
                    value: "C",
                    type: "Identity",
                },
                B5: {
                    kind: "LiteralString",
                    value: "E",
                    type: "Identity",
                },
                B4: {
                    kind: "LiteralString",
                    value: "D",
                    type: "Identity",
                },
            },
        },
    },
});
