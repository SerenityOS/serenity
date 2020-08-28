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
        "values to the `curent value`.",
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
