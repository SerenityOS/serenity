const sheet = this

function range(start, end, column_step, row_step) {
    column_step = integer(column_step ?? 1)
    row_step = integer(row_step ?? 1)
    start = sheet.parse_cell_name(start) ?? {column: 'A', row: 0}
    end = sheet.parse_cell_name(end) ?? start

    if (end.column.length > 1 || start.column.length > 1)
        throw new TypeError("Only single-letter column names are allowed (TODO)");

    const cells = []

    for (let col = Math.min(start.column.charCodeAt(0), end.column.charCodeAt(0));
        col <= Math.max(start.column.charCodeAt(0), end.column.charCodeAt(0));
        ++col) {
        for (let row = Math.min(start.row, end.row);
            row <= Math.max(start.row, end.row);
            ++row) {

            cells.push(String.fromCharCode(col) + row)
        }
    }

    return cells
}

// FIXME: Remove this and use String.split() eventually
function split(str, sep) {
    const parts = []
    let split_index = -1
    for(;;) {
        split_index = str.indexOf(sep)
        if (split_index == -1) {
            if (str.length)
                parts.push(str)
            return parts
        }
        parts.push(str.substring(0, split_index))
        str = str.slice(split_index + sep.length)
    }
}

function R(fmt, ...args) {
    if (args.length !== 0)
        throw new TypeError("R`` format must be literal")

    fmt = fmt[0]
    return range(...split(fmt, ':'))
}

function select(criteria, t, f) {
    if (criteria)
        return t;
    return f;
}

function sumif(condition, cells) {
    let sum = null
    for (let name of cells) {
        let cell = sheet[name]
        if (condition(cell))
            sum = sum === null ? cell : sum + cell
    }
    return sum
}

function countif(condition, cells) {
    let count = 0
    for (let name of cells) {
        let cell = sheet[name]
        if (condition(cell))
            count++
    }
    return count
}

function now() {
    return new Date()
}

function repeat(count, str) {
    return Array(count + 1).join(str)
}

function randrange(min, max) {
    return Math.random() * (max - min) + min
}

function integer(value) {
    return value | 0
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
