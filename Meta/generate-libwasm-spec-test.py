#!/usr/bin/env python3
import struct
from sys import argv, stderr
from os import path
from string import whitespace
import re
import math
from tempfile import NamedTemporaryFile
from subprocess import call
import json

atom_end = set('()"' + whitespace)


def parse(sexp):
    sexp = re.sub(r'(?m)\(;.*;\)', '', re.sub(r'(;;.*)', '', sexp))
    stack, i, length = [[]], 0, len(sexp)
    while i < length:
        c = sexp[i]
        kind = type(stack[-1])
        if kind == list:
            if c == '(':
                stack.append([])
            elif c == ')':
                stack[-2].append(stack.pop())
            elif c == '"':
                stack.append('')
            elif c in whitespace:
                pass
            else:
                stack.append((c,))
        elif kind == str:
            if c == '"':
                stack[-2].append(stack.pop())
            elif c == '\\':
                i += 1
                if sexp[i] != '"':
                    stack[-1] += '\\'
                stack[-1] += sexp[i]
            else:
                stack[-1] += c
        elif kind == tuple:
            if c in atom_end:
                atom = stack.pop()
                stack[-1].append(atom)
                continue
            else:
                stack[-1] = ((stack[-1][0] + c),)
        i += 1
    return stack.pop()


def parse_typed_value(ast):
    types = {
        'i32.const': 'i32',
        'i64.const': 'i64',
        'f32.const': 'float',
        'f64.const': 'double',
    }
    if len(ast) == 2 and ast[0][0] in types:
        return {"type": types[ast[0][0]], "value": ast[1][0]}

    return {"type": "error"}


def generate_module_source_for_compilation(entries):
    s = '('
    for entry in entries:
        if type(entry) == tuple and len(entry) == 1 and type(entry[0]) == str:
            s += entry[0] + ' '
        elif type(entry) == str:
            s += json.dumps(entry) + ' '
        elif type(entry) == list:
            s += generate_module_source_for_compilation(entry)
        else:
            raise Exception("wat? I dunno how to pretty print " + str(type(entry)))
    while s.endswith(' '):
        s = s[:len(s) - 1]
    return s + ')'


def generate_binary_source(chunks):
    res = b''
    for chunk in chunks:
        i = 0
        while i < len(chunk):
            c = chunk[i]
            if c == '\\':
                res += bytes.fromhex(chunk[i + 1: i + 3])
                i += 3
                continue
            res += c.encode('utf-8')
            i += 1
    return res


named_modules = {}
named_modules_inverse = {}
registered_modules = {}


def generate(ast):
    global named_modules, named_modules_inverse, registered_modules

    if type(ast) != list:
        return []
    tests = []
    for entry in ast:
        if len(entry) > 0 and entry[0] == ('module',):
            name = None
            mode = 'ast'  # binary, quote
            start_index = 1
            if len(entry) > 1:
                if isinstance(entry[1], tuple) and isinstance(entry[1][0], str) and entry[1][0].startswith('$'):
                    name = entry[1][0]
                    if len(entry) > 2:
                        if isinstance(entry[2], tuple) and entry[2][0] in ('binary', 'quote'):
                            mode = entry[2][0]
                            start_index = 3
                        else:
                            start_index = 2
                elif isinstance(entry[1][0], str):
                    mode = entry[1][0]
                    start_index = 2

            tests.append({
                "module": {
                    'ast': lambda: ('parse', generate_module_source_for_compilation(entry)),
                    'binary': lambda: ('literal', generate_binary_source(entry[start_index:])),
                    # FIXME: Make this work when we have a WAT parser
                    'quote': lambda: ('literal', entry[start_index]),
                }[mode](),
                "tests": []
            })

            if name is not None:
                named_modules[name] = len(tests) - 1
                named_modules_inverse[len(tests) - 1] = (name, None)
        elif len(entry) in [2, 3] and entry[0][0].startswith('assert_'):
            if entry[1][0] == ('invoke',):
                arg, name, module = 0, None, None
                if isinstance(entry[1][1], str):
                    name = entry[1][1]
                else:
                    name = entry[1][2]
                    module = named_modules[entry[1][1][0]]
                    arg = 1
                tests[-1]["tests"].append({
                    "kind": entry[0][0][len('assert_'):],
                    "function": {
                        "module": module,
                        "name": name,
                        "args": list(parse_typed_value(x) for x in entry[1][arg + 2:])
                    },
                    "result": parse_typed_value(entry[2]) if len(entry) == 3 + arg else None
                })
            elif entry[1][0] == ('get',):
                arg, name, module = 0, None, None
                if isinstance(entry[1][1], str):
                    name = entry[1][1]
                else:
                    name = entry[1][2]
                    module = named_modules[entry[1][1][0]]
                    arg = 1
                tests[-1]["tests"].append({
                    "kind": entry[0][0][len('assert_'):],
                    "get": {
                        "name": name,
                        "module": module,
                    },
                    "result": parse_typed_value(entry[2]) if len(entry) == 3 + arg else None
                })
            else:
                if not len(tests):
                    tests.append({
                        "module": ('literal', b""),
                        "tests": []
                    })
                tests[-1]["tests"].append({
                    "kind": "testgen_fail",
                    "function": {
                        "module": None,
                        "name": "<unknown>",
                        "args": []
                    },
                    "reason": f"Unknown assertion {entry[0][0][len('assert_'):]}"
                })
        elif len(entry) >= 2 and entry[0][0] == 'invoke':
            # toplevel invoke :shrug:
            arg, name, module = 0, None, None
            if isinstance(entry[1][1], str):
                name = entry[1][1]
            else:
                name = entry[1][2]
                module = named_modules[entry[1][1][0]]
                arg = 1
            tests[-1]["tests"].append({
                "kind": "ignore",
                "function": {
                    "module": module,
                    "name": name,
                    "args": list(parse_typed_value(x) for x in entry[1][arg + 2:])
                },
                "result": parse_typed_value(entry[2]) if len(entry) == 3 + arg else None
            })
        elif len(entry) > 1 and entry[0][0] == 'register':
            if len(entry) == 3:
                registered_modules[entry[1]] = named_modules[entry[2][0]]
                x = named_modules_inverse[named_modules[entry[2][0]]]
                named_modules_inverse[named_modules[entry[2][0]]] = (x[0], entry[1])
            else:
                index = len(tests) - 1
                registered_modules[entry[1]] = index
                named_modules_inverse[index] = (":" + entry[1], entry[1])
        else:
            if not len(tests):
                tests.append({
                    "module": ('literal', b""),
                    "tests": []
                })
            tests[-1]["tests"].append({
                "kind": "testgen_fail",
                "function": {
                    "module": None,
                    "name": "<unknown>",
                    "args": []
                },
                "reason": f"Unknown command {entry[0][0]}"
            })
    return tests


def genarg(spec):
    if spec['type'] == 'error':
        return '0'

    def gen():
        x = spec['value']
        if x == 'nan':
            return 'NaN'
        if x == '-nan':
            return '-NaN'

        try:
            x = float(x)
            if math.isnan(x):
                # FIXME: This is going to mess up the different kinds of nan
                return '-NaN' if math.copysign(1.0, x) < 0 else 'NaN'
            if math.isinf(x):
                return 'Infinity' if x > 0 else '-Infinity'
            return x
        except ValueError:
            try:
                x = float.fromhex(x)
                if math.isnan(x):
                    # FIXME: This is going to mess up the different kinds of nan
                    return '-NaN' if math.copysign(1.0, x) < 0 else 'NaN'
                if math.isinf(x):
                    return 'Infinity' if x > 0 else '-Infinity'
                return x
            except ValueError:
                try:
                    x = int(x, 0)
                    return x
                except ValueError:
                    return x

    x = gen()
    if isinstance(x, str):
        if x.startswith('nan'):
            return 'NaN'
        if x.startswith('-nan'):
            return '-NaN'
        return x
    if spec['type'] == 'i32':
        # cast back to i32 to get the correct sign
        return str(struct.unpack('>i', struct.pack('>q', int(x))[4:])[0])
    return str(x)


all_names_in_main = {}


def genresult(ident, entry):
    expectation = f'expect().fail("Unknown result structure " + {json.dumps(entry)})'
    if "function" in entry:
        tmodule = 'module'
        if entry['function']['module'] is not None:
            tmodule = f'namedModules[{json.dumps(named_modules_inverse[entry["function"]["module"]][0])}]'
        expectation = (
            f'{tmodule}.invoke({ident}, {", ".join(genarg(x) for x in entry["function"]["args"])})'
        )
    elif "get" in entry:
        expectation = f'module.getExport({ident})'

    if entry['kind'] == 'return':
        return (
            f'let {ident}_result = {expectation};\n    '
            f'expect({ident}_result).toBe({genarg(entry["result"])})\n    ' if entry["result"] is not None else ''
        )

    if entry['kind'] == 'trap':
        return (
            f'expect(() => {expectation}).toThrow(TypeError, "Execution trapped");\n    '
        )

    if entry['kind'] == 'ignore':
        return expectation

    if entry['kind'] == 'unlinkable':
        return

    if entry['kind'] == 'testgen_fail':
        return f'throw Exception("Test Generator Failure: " + {json.dumps(entry["reason"])});\n    '

    return f'throw Exception("(Test Generator) Unknown test kind {entry["kind"]}");\n    '


def gentest(entry, main_name):
    isfunction = 'function' in entry
    name = json.dumps((entry["function"] if isfunction else entry["get"])["name"])[1:-1]
    if type(name) != str:
        print("Unsupported test case (call to", name, ")", file=stderr)
        return '\n    '
    ident = '_' + re.sub("[^a-zA-Z_0-9]", "_", name)
    count = all_names_in_main.get(name, 0)
    all_names_in_main[name] = count + 1
    test_name = f'execution of {main_name}: {name} (instance {count})'
    tmodule = 'module'
    key = "function" if "function" in entry else "get"
    if entry[key]['module'] is not None:
        tmodule = f'namedModules[{json.dumps(named_modules_inverse[entry[key]["module"]][0])}]'
    source = (
        f'test({json.dumps(test_name)}, () => {{\n'
        f'let {ident} = {tmodule}.getExport({json.dumps(name)});\n        '
        f'expect({ident}).not.toBeUndefined();\n        '
        f'{genresult(ident, entry)}'
        '});\n\n    '
    )
    return source


def gen_parse_module(name, index):
    export_string = ''
    if index in named_modules_inverse:
        entry = named_modules_inverse[index]
        export_string += f'namedModules[{json.dumps(entry[0])}] = module;\n    '
        if entry[1]:
            export_string += f'globalImportObject[{json.dumps(entry[1])}] = module;\n    '

    return (
        f'let content = readBinaryWasmFile("Fixtures/SpecTests/{name}.wasm");\n    '
        f'const module = parseWebAssemblyModule(content, globalImportObject)\n    '
        f'{export_string}\n     '
    )


def nth(a, x, y=None):
    if y:
        return a[x:y]
    return a[x]


def main():
    with open(argv[1]) as f:
        sexp = f.read()
    name = argv[2]
    module_output_path = argv[3]
    ast = parse(sexp)
    print('let globalImportObject = {};')
    print('let namedModules = {};\n')
    for index, description in enumerate(generate(ast)):
        testname = f'{name}_{index}'
        outpath = path.join(module_output_path, f'{testname}.wasm')
        mod = description["module"]
        if mod[0] == 'literal':
            with open('outpath', 'wb+') as f:
                f.write(mod[1])
        elif mod[0] == 'parse':
            with NamedTemporaryFile("w+") as temp:
                temp.write(mod[1])
                temp.flush()
                rc = call(["wat2wasm", temp.name, "-o", outpath])
                if rc != 0:
                    print("Failed to compile", name, "module index", index, "skipping that test", file=stderr)
                    continue

        sep = ""
        print(f'''describe({json.dumps(testname)}, () => {{
{gen_parse_module(testname, index)}
{sep.join(gentest(x, testname) for x in description["tests"])}
}});
''')


if __name__ == "__main__":
    main()
