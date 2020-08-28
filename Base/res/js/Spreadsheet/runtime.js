function range(start, end, columnStep, rowStep) {
    columnStep = integer(columnStep ?? 1);
    rowStep = integer(rowStep ?? 1);
    start = parse_cell_name(start) ?? { column: "A", row: 0 };
    end = parse_cell_name(end) ?? start;

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

function sumIf(condition, cells) {
    let sum = null;
    for (let name of cells) {
        let cell = thisSheet[name];
        if (condition(cell)) sum = sum === null ? cell : sum + cell;
    }
    return sum;
}

function countIf(condition, cells) {
    let count = 0;
    for (let name of cells) {
        let cell = thisSheet[name];
        if (condition(cell)) count++;
    }
    return count;
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

countIf.__documentation = JSON.stringify({
    name: "countIf",
    argc: 2,
    argnames: ["condition", "cell names"],
    doc: "Counts cells the value of which evaluates to true when passed to `condition`",
    examples: {
        'countIf(x => x instanceof Number, range("A1", "C4"))':
            "Counts the number of cells which have numbers within A1:C4",
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
