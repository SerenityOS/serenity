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


def generate(ast):
    if type(ast) != list:
        return []
    tests = []
    for entry in ast:
        if len(entry) > 0 and entry[0] == ('module',):
            tests.append({
                "module": generate_module_source_for_compilation(entry),
                "tests": []
            })
        elif len(entry) in [2, 3] and entry[0][0].startswith('assert_'):
            if entry[1][0] == ('invoke',):
                tests[-1]["tests"].append({
                    "kind": entry[0][0][len('assert_'):],
                    "function": {
                        "name": entry[1][1],
                        "args": list(parse_typed_value(x) for x in entry[1][2:])
                    },
                    "result": parse_typed_value(entry[2]) if len(entry) == 3 else None
                })
            else:
                if not len(tests):
                    tests.append({
                        "module": "",
                        "tests": []
                    })
                tests[-1]["tests"].append({
                    "kind": "testgen_fail",
                    "function": {
                        "name": "<unknown>",
                        "args": []
                    },
                    "reason": f"Unknown assertion {entry[0][0][len('assert_'):]}"
                })
        elif len(entry) >= 2 and entry[0][0] == 'invoke':
            # toplevel invoke :shrug:
            tests[-1]["tests"].append({
                "kind": "ignore",
                "function": {
                    "name": entry[1][1],
                    "args": list(parse_typed_value(x) for x in entry[1][2:])
                },
                "result": parse_typed_value(entry[2]) if len(entry) == 3 else None
            })
        else:
            if not len(tests):
                tests.append({
                    "module": "",
                    "tests": []
                })
            tests[-1]["tests"].append({
                "kind": "testgen_fail",
                "function": {
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
    if entry['kind'] == 'return':
        return_check = f'expect({ident}_result).toBe({genarg(entry["result"])})' if entry["result"] is not None else ''
        return (
            f'let {ident}_result ='
            f' module.invoke({ident}, {", ".join(genarg(x) for x in entry["function"]["args"])});\n        '
            f'{return_check};\n    '
        )

    if entry['kind'] == 'trap':
        return (
            f'expect(() => module.invoke({ident}, {", ".join(genarg(x) for x in entry["function"]["args"])}))'
            '.toThrow(TypeError, "Execution trapped");\n    '
        )

    if entry['kind'] == 'ignore':
        return f'module.invoke({ident}, {", ".join(genarg(x) for x in entry["function"]["args"])});\n    '

    if entry['kind'] == 'testgen_fail':
        return f'throw Exception("Test Generator Failure: " + {json.dumps(entry["reason"])});\n    '

    return f'throw Exception("(Test Generator) Unknown test kind {entry["kind"]}");\n    '


def gentest(entry, main_name):
    name = json.dumps(entry["function"]["name"])[1:-1]
    if type(name) != str:
        print("Unsupported test case (call to", name, ")", file=stderr)
        return '\n    '
    ident = '_' + re.sub("[^a-zA-Z_0-9]", "_", name)
    count = all_names_in_main.get(name, 0)
    all_names_in_main[name] = count + 1
    test_name = f'execution of {main_name}: {name} (instance {count})'
    source = (
        f'test({json.dumps(test_name)}, () => {{\n'
        f'let {ident} = module.getExport({json.dumps(name)});\n        '
        f'expect({ident}).not.toBeUndefined();\n        '
        f'{genresult(ident, entry)}'
        '});\n\n    '
    )
    return source


def gen_parse_module(name):
    return (
        f'let content = readBinaryWasmFile("Fixtures/SpecTests/{name}.wasm");\n    '
        f'const module = parseWebAssemblyModule(content)\n    '
    )


def main():
    with open(argv[1]) as f:
        sexp = f.read()
    name = argv[2]
    module_output_path = argv[3]
    ast = parse(sexp)
    for index, description in enumerate(generate(ast)):
        testname = f'{name}_{index}'
        outpath = path.join(module_output_path, f'{testname}.wasm')
        with NamedTemporaryFile("w+") as temp:
            temp.write(description["module"])
            temp.flush()
            rc = call(["wasm-as", "-n", "-all", temp.name, "-o", outpath])
            if rc != 0:
                print("Failed to compile", name, "module index", index, "skipping that test", file=stderr)
                continue

        sep = ""
        print(f'''describe({json.dumps(testname)}, () => {{
{gen_parse_module(testname)}
{sep.join(gentest(x, testname) for x in description["tests"])}
}});
''')


if __name__ == "__main__":
    main()
