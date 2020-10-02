// FIXME: Figure out a way to document non-function entities too.
class Position {
    constructor(column, row, sheet) {
        this.column = column;
        this.row = row;
        this.sheet = sheet ?? thisSheet;
        this.name = `${column}${row}`;
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
        const column = Math.min(
            "Z".charCodeAt(0),
            Math.max("A".charCodeAt(0), this.column.charCodeAt(0) - how_many)
        );
        return new Position(String.fromCharCode(column), this.row, this.sheet);
    }

    right(how_many) {
        how_many = how_many ?? 1;
        const column = Math.min(
            "Z".charCodeAt(0),
            Math.max("A".charCodeAt(0), this.column.charCodeAt(0) + how_many)
        );
        return new Position(String.fromCharCode(column), this.row, this.sheet);
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
        return `<Cell at ${this.name}>`;
    }
}

function range(start, end, columnStep, rowStep) {
    columnStep = integer(columnStep ?? 1);
    rowStep = integer(rowStep ?? 1);
    if (!(start instanceof Position)) {
        start = parse_cell_name(start) ?? { column: "A", row: 0 };
    }
    if (!(end instanceof Position)) {
        end = parse_cell_name(end) ?? start;
    }

    if (end.column.length > 1 || start.column.length > 1)
        throw new TypeError("Only single-letter column names are allowed (TODO)");

    const cells = [];

    for (
        let col = Math.min(start.column.charCodeAt(0), end.column.charCodeAt(0));
        col <= Math.max(start.column.charCodeAt(0), end.column.charCodeAt(0));
        col += columnStep
    ) {
        for (
            let row = Math.min(start.row, end.row);
            row <= Math.max(start.row, end.row);
            row += rowStep
        ) {
            cells.push(String.fromCharCode(col) + row);
        }
    }

    return cells;
}

// FIXME: Remove this and use String.split() eventually
function split(str, sep) {
    const parts = [];
    let splitIndex = -1;
    for (;;) {
        splitIndex = str.indexOf(sep);
        if (splitIndex == -1) {
            if (str.length) parts.push(str);
            break;
        }
        parts.push(str.substring(0, splitIndex));
        str = str.slice(splitIndex + sep.length);
    }
    return parts;
}

function R(fmt, ...args) {
    if (args.length !== 0) throw new TypeError("R`` format must be literal");

    fmt = fmt[0];
    return range(...split(fmt, ":"));
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
    return value | 0;
}

function sheet(name) {
    return workbook.sheet(name);
}

function reduce(op, accumulator, cells) {
    for (let name of cells) {
        let cell = thisSheet[name];
        accumulator = op(accumulator, cell);
    }
    return accumulator;
}

function numericReduce(op, accumulator, cells) {
    return reduce((acc, x) => op(acc, Number(x)), accumulator, cells);
}

function numericResolve(cells) {
    const values = [];
    for (let name of cells) values.push(Number(thisSheet[name]));
    return values;
}

function resolve(cells) {
    const values = [];
    for (let name of cells) values.push(thisSheet[name]);
    return values;
}

// Statistics

function sum(cells) {
    return numericReduce((acc, x) => acc + x, 0, cells);
}

function sumIf(condition, cells) {
    return numericReduce((acc, x) => (condition(x) ? acc + x : acc), 0, cells);
}

function count(cells) {
    return reduce((acc, x) => acc + 1, 0, cells);
}

function countIf(condition, cells) {
    return reduce((acc, x) => (condition(x) ? acc + 1 : acc), 0, cells);
}

function average(cells) {
    const sumAndCount = numericReduce((acc, x) => [acc[0] + x, acc[1] + 1], [0, 0], cells);
    return sumAndCount[0] / sumAndCount[1];
}

function averageIf(condition, cells) {
    const sumAndCount = numericReduce(
        (acc, x) => (condition(x) ? [acc[0] + x, acc[1] + 1] : acc),
        [0, 0],
        cells
    );
    return sumAndCount[0] / sumAndCount[1];
}

function median(cells) {
    const values = numericResolve(cells);

    if (values.length == 0) return 0;

    function qselect(arr, idx) {
        if (arr.length == 1) return arr[0];

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

function variance(cells) {
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

function stddev(cells) {
    return Math.sqrt(variance(cells));
}

// Lookup

function row() {
    return thisSheet.current_cell_position().row;
}

function column() {
    return thisSheet.current_cell_position().column;
}

function here() {
    const position = current_cell_position();
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
    lookup_outputs = lookup_outputs ?? lookup_inputs;

    if (lookup_inputs.length > lookup_outputs.length)
        throw new Error(
            `Uneven lengths in outputs and inputs: ${lookup_inputs.length} > ${lookup_outputs.length}`
        );

    let references = lookup_outputs;
    lookup_inputs = resolve(lookup_inputs);
    lookup_outputs = resolve(lookup_outputs);
    if_missing = if_missing ?? undefined;
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

    let retval = if_missing;
    for (let i = 0; i < lookup_inputs.length; ++i) {
        if (matches(lookup_inputs[i])) {
            retval = reference ? Position.from_name(references[i]) : lookup_outputs[i];
            break;
        }
    }

    return retval;
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
    return internal_lookup(
        req_lookup_value,
        lookup_inputs,
        lookup_outputs,
        if_missing ?? here(),
        mode,
        true
    );
}

// Cheat the system and add documentation
range.__documentation = JSON.stringify({
    name: "range",
    argc: 2,
    argnames: ["start", "end", "column step", "row step"],
    doc:
        "Generates a list of cell names in a rectangle defined by two " +
        "_top left_ and _bottom right_ cells `start` and `end`, spaced" +
        " `column step` columns, and `row step` rows apart.",
    examples: {
        'range("A1", "C4")': "Generate a range A1:C4",
        'range("A1", "C4", 2)': "Generate a range A1:C4, skipping every other column",
    },
});

R.__documentation = JSON.stringify({
    name: "R",
    argc: 1,
    argnames: ["range specifier"],
    doc:
        "Generates a list of cell names in a rectangle defined by " +
        "_range specifier_, which must be two cell names " +
        "delimited by a comma ':'. Operates the same as `range`", // TODO: Add support for hyperlinks.
    examples: {
        "R`A1:C4`": "Generate the range A1:C4",
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
    examples: {},
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
    examples: {},
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
        'reduce((acc, x) => acc * x, 1, range("A0", "A5"))':
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
        "unlike `reduce`, casts the values to a number before passing them to the `reduction function`.",
    examples: {
        'numericReduce((acc, x) => acc * x, 1, range("A0", "A5"))':
            "Calculate the numeric product of all values in the range A0:A5",
    },
});

sum.__documentation = JSON.stringify({
    name: "sum",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the sum of the values in `cells`",
    examples: {
        'sum(range("A0", "C4"))': "Calculate the sum of the values in A0:C4",
    },
});

sumIf.__documentation = JSON.stringify({
    name: "sumIf",
    argc: 2,
    argnames: ["condition", "cell names"],
    doc:
        "Calculates the sum of cells the value of which evaluates to true when passed to `condition`",
    examples: {
        'sumIf(x => x instanceof Number, range("A1", "C4"))':
            "Calculates the sum of all numbers within A1:C4",
    },
});

count.__documentation = JSON.stringify({
    name: "count",
    argc: 1,
    argnames: ["cell names"],
    doc: "Counts the number of cells in the given range",
    examples: {
        'count(range("A0", "C4"))': "Count the number of cells in A0:C4",
    },
});

countIf.__documentation = JSON.stringify({
    name: "countIf",
    argc: 2,
    argnames: ["condition", "cell names"],
    doc: "Counts cells the value of which evaluates to true when passed to `condition`",
    examples: {
        'countIf(x => x instanceof Number, range("A1", "C4"))':
            "Count the number of cells which have numbers within A1:C4",
    },
});

average.__documentation = JSON.stringify({
    name: "average",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the average of the values in `cells`",
    examples: {
        'average(range("A0", "C4"))': "Calculate the average of the values in A0:C4",
    },
});

averageIf.__documentation = JSON.stringify({
    name: "averageIf",
    argc: 2,
    argnames: ["condition", "cell names"],
    doc:
        "Calculates the average of cells the value of which evaluates to true when passed to `condition`",
    examples: {
        'averageIf(x => x > 4, range("A1", "C4"))':
            "Calculate the sum of all numbers larger then 4 within A1:C4",
    },
});

median.__documentation = JSON.stringify({
    name: "median",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the median of the numeric values in the given range of cells",
    examples: {
        'median(range("A0", "C4"))': "Calculate the median of the values in A0:C4",
    },
});

variance.__documentation = JSON.stringify({
    name: "variance",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the variance of the numeric values in the given range of cells",
    examples: {
        'variance(range("A0", "C4"))': "Calculate the variance of the values in A0:C4",
    },
});

stddev.__documentation = JSON.stringify({
    name: "stddev",
    argc: 1,
    argnames: ["cell names"],
    doc: "Calculates the standard deviation of the numeric values in the given range of cells",
    examples: {
        'stddev(range("A0", "C4"))': "Calculate the standard deviation of the values in A0:C4",
    },
});

row.__documentation = JSON.stringify({
    name: "row",
    argc: 0,
    argnames: [],
    doc: "Returns the row number of the current cell",
    examples: {},
});

column.__documentation = JSON.stringify({
    name: "column",
    argc: 0,
    argnames: [],
    doc: "Returns the column name of the current cell",
    examples: {},
});

here.__documentation = JSON.stringify({
    name: "here",
    argc: 0,
    argnames: [],
    doc:
        "Returns an object representing the current cell's position, see `Position` below.\n\n" +
        "## Position\na `Position` is an object representing a given cell position in a given sheet.\n" +
        "### Methods:\n- `up(count = 1)`: goes up count cells, or returns the top position if at the top\n" +
        "- `down(count = 1)`: goes down count cells\n" +
        "- `left(count = 1)`: Goes left count cells, or returns the leftmost position if the edge\n" +
        "- `right(count = 1)`: Goes right count cells.\n" +
        "- `with_row(row)`: Returns a Position with its column being this object's, and its row being the provided the value.\n" +
        "- `with_column(column)`: Similar to `with_row()`, but changes the column instead.\n" +
        "- `in_sheet(the_sheet)`: Returns a Position with the same column and row as this one, but with its sheet being `the_sheet`.\n" +
        "- `value()`: Returns the value at the position which it represents, in the object's sheet (current sheet by default).\n\n" +
        "**NOTE**: Currently only supports single-letter column names",
    examples: {
        "here().up().value()": "Get the value of the cell above this one",
        "here().up().with_column('A')":
            "Get a Position above this one in column A, for instance, evaluates to A2 if run in B3.",
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
        "if nothing matches, the value `value if no match`" +
        " is returned, which is `undefined` by default.\nBy setting the `match method`, the function can be altered to return " +
        "the closest ordered value (above or below) instead of an exact match. The valid choices for `match method` are:\n" +
        "- `'exact'`: The default method. Uses strict equality to match values.\n" +
        "- `'nextlargest'`: Uses the greater-or-equal operator to match values.\n" +
        "- `'nextsmallest'`: Uses the less-than-or-equal operator to match values.\n",
    examples: {
        "lookup(F3, R`B2:B11`, R`D2:D11`)":
            "Look for the value of F3 in the range B2:B11, and return the corresponding value from the D column",
        "lookup(E2, R`C2:C5`, R`B2:B5`, 0, 'nextlargest')":
            "Find the closest (larger) value to E2 in range C2:C5, and evaluate to 0 if no value in that range is larger",
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
        "if nothing matches, the value `value if no match`" +
        " is returned, which is `undefined` by default.\nBy setting the `match method`, the function can be altered to return " +
        "the closest ordered value (above or below) instead of an exact match. The valid choices for `match method` are:\n" +
        "- `'exact'`: The default method. Uses strict equality to match values.\n" +
        "- `'nextlargest'`: Uses the greater-or-equal operator to match values.\n" +
        "- `'nextsmallest'`: Uses the less-than-or-equal operator to match values.\n" +
        "\nThis function return a `Position` (see `here()`)",
    examples: {
        "reflookup(F3, R`B2:B11`, R`D2:D11`)":
            "Look for the value of F3 in the range B2:B11, and return the corresponding cell name from the D column",
        "reflookup(E2, R`C2:C5`, R`B2:B5`, here(), 'nextlargest')":
            "Find the cell with the closest (larger) value to E2 in range C2:C5, and evaluate to the current cell if no value in that range is larger",
    },
});
