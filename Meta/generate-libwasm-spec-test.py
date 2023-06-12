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
import array

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


class TestGenerationError(Exception):
    def __init__(self, message):
        self.msg = message


def parse_typed_value(ast):
    types = {
        'i32.const': 'i32',
        'i64.const': 'i64',
        'f32.const': 'float',
        'f64.const': 'double',
        'v128.const': 'bigint',
    }

    v128_sizes = {
        'i8x16': 1,
        'i16x8': 2,
        'i32x4': 4,
        'i64x2': 8,
        'f32x4': 4,
        'f64x2': 8,
    }
    v128_format_names = {
        'i8x16': 'b',
        'i16x8': 'h',
        'i32x4': 'i',
        'i64x2': 'q',
        'f32x4': 'f',
        'f64x2': 'd',
    }
    v128_format_names_unsigned = {
        'i8x16': 'B',
        'i16x8': 'H',
        'i32x4': 'I',
        'i64x2': 'Q',
    }

    def parse_v128_chunk(num, type) -> array:
        negative = 1
        if num.startswith('-'):
            negative = -1
            num = num[1:]
        elif num.startswith('+'):
            num = num[1:]

        # wtf spec test, split your wast tests already
        while num.startswith('0') and not num.startswith('0x'):
            num = num[1:]

        if num == '':
            num = '0'

        if type.startswith('f'):
            def generate():
                if num == 'nan:canonical':
                    return float.fromhex('0x7fc00000')
                if num == 'nan:arithmetic':
                    return float.fromhex('0x7ff00000')
                if num == 'nan:signaling':
                    return float.fromhex('0x7ff80000')
                if num.startswith('nan:'):
                    # FIXME: I have no idea if this is actually correct :P
                    rest = num[4:]
                    return float.fromhex('0x7ff80000') + int(rest, base=16)
                if num.lower() == 'infinity':
                    return float.fromhex('0x7ff00000') * negative
                try:
                    return float(num) * negative
                except ValueError:
                    return float.fromhex(num) * negative

            value = generate()
            return struct.pack(f'={v128_format_names[type]}', value)
        value = negative * int(num.replace('_', ''), base=0)
        try:
            return struct.pack(f'={v128_format_names[type]}', value)
        except struct.error:
            # The test format uses signed and unsigned values interchangeably, this is probably an unsigned value.
            return struct.pack(f'={v128_format_names_unsigned[type]}', value)

    if len(ast) >= 2 and ast[0][0] in types:
        if ast[0][0] == 'v128.const':
            value = array.array('b')
            for i, num in enumerate(ast[2:]):
                size = v128_sizes[ast[1][0]]
                s = len(value)
                value.frombytes(parse_v128_chunk(num[0], ast[1][0]))
                assert len(value) - s == size, f'Expected {size} bytes, got {len(value) - s} bytes'

            assert len(value) == 16, f'Expected 16 bytes, got {len(value)} bytes'
            return {
                'type': types[ast[0][0]],
                'value': value.tobytes().hex()
            }

        return {"type": types[ast[0][0]], "value": ast[1][0]}

    return {"type": "error"}


def generate_module_source_for_compilation(entries):
    s = '('
    for entry in entries:
        if type(entry) is tuple and len(entry) == 1 and type(entry[0]) is str:
            s += entry[0] + ' '
        elif type(entry) is str:
            s += json.dumps(entry).replace('\\\\', '\\') + ' '
        elif type(entry) is list:
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
module_output_path: str


def generate_module(ast):
    # (module ...)
    name = None
    mode = 'ast'  # binary, quote
    start_index = 1
    if len(ast) > 1:
        if isinstance(ast[1], tuple) and isinstance(ast[1][0], str) and ast[1][0].startswith('$'):
            name = ast[1][0]
            if len(ast) > 2:
                if isinstance(ast[2], tuple) and ast[2][0] in ('binary', 'quote'):
                    mode = ast[2][0]
                    start_index = 3
                else:
                    start_index = 2
        elif isinstance(ast[1][0], str):
            mode = ast[1][0]
            start_index = 2

    result = {
        'ast': lambda: ('parse', generate_module_source_for_compilation(ast)),
        'binary': lambda: ('literal', generate_binary_source(ast[start_index:])),
        # FIXME: Make this work when we have a WAT parser
        'quote': lambda: ('literal', ast[start_index]),
    }[mode]()

    return {
        'module': result,
        'name': name
    }


def generate(ast):
    global named_modules, named_modules_inverse, registered_modules

    if type(ast) is not list:
        return []
    tests = []
    for entry in ast:
        if len(entry) > 0 and entry[0] == ('module',):
            gen = generate_module(entry)
            module, name = gen['module'], gen['name']
            tests.append({
                "module": module,
                "tests": []
            })

            if name is not None:
                named_modules[name] = len(tests) - 1
                named_modules_inverse[len(tests) - 1] = (name, None)
        elif entry[0] == ('assert_unlinkable',):
            # (assert_unlinkable module message)
            if len(entry) < 2 or not isinstance(entry[1], list) or entry[1][0] != ('module',):
                print(f"Invalid argument to assert_unlinkable: {entry[1]}", file=stderr)
                continue
            result = generate_module(entry[1])
            tests.append({
                'module': None,
                'tests': [{
                    "kind": "unlinkable",
                    "module": result['module'],
                }]
            })
        elif entry[0] in (('assert_malformed',), ('assert_invalid',)):
            # (assert_malformed/invalid module message)
            if len(entry) < 2 or not isinstance(entry[1], list) or entry[1][0] != ('module',):
                print(f"Invalid argument to assert_malformed: {entry[1]}", file=stderr)
                continue
            result = generate_module(entry[1])
            kind = entry[0][0][len('assert_'):]
            tests.append({
                'module': None,
                'kind': kind,
                'tests': [{
                    "kind": kind,
                    "module": result['module'],
                }]
            })
        elif len(entry) in [2, 3] and entry[0][0].startswith('assert_'):
            if entry[1][0] == ('invoke',):
                arg, name, module = 0, None, None
                if isinstance(entry[1][1], str):
                    name = entry[1][1]
                else:
                    name = entry[1][2]
                    module = named_modules[entry[1][1][0]]
                    arg = 1
                kind = entry[0][0][len('assert_'):]
                tests[-1]["tests"].append({
                    "kind": kind,
                    "function": {
                        "module": module,
                        "name": name,
                        "args": list(parse_typed_value(x) for x in entry[1][arg + 2:])
                    },
                    "result": parse_typed_value(entry[2]) if len(entry) == 3 + arg and kind != 'exhaustion' else None
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
            if not isinstance(entry[1], str) and isinstance(entry[1][1], str):
                name = entry[1][1]
            elif isinstance(entry[1], str):
                name = entry[1]
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
        if spec['type'] == 'bigint':
            return f"0x{x}n"

        if spec['type'] in ('i32', 'i64'):
            if x.startswith('0x'):
                if spec['type'] == 'i32':
                    # cast back to i32 to get the correct sign
                    return str(struct.unpack('>i', struct.pack('>Q', int(x, 16))[4:])[0])

                # cast back to i64 to get the correct sign
                return str(struct.unpack('>q', struct.pack('>Q', int(x, 16)))[0]) + 'n'
            if spec['type'] == 'i64':
                # Make a bigint instead, since `double' cannot fit all i64 values.
                if x.startswith('0'):
                    x = x.lstrip('0')
                if x == '':
                    x = '0'
                return x + 'n'
            return x

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
    return str(x)


all_names_in_main = {}


def genresult(ident, entry, index):
    expectation = None
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
                f'let {ident}_result = {expectation};\n    ' +
                (f'expect({ident}_result).toBe({genarg(entry["result"])})\n    ' if entry["result"] is not None else '')
        )

    if entry['kind'] == 'ignore':
        return expectation

    if entry['kind'] == 'unlinkable':
        name = f'mod-{ident}-{index}.wasm'
        outpath = path.join(module_output_path, name)
        if not compile_wasm_source(entry['module'], outpath):
            return 'throw new Error("Module compilation failed");'
        return (
            f'    expect(() => {{\n'
            f'        let content = readBinaryWasmFile("Fixtures/SpecTests/{name}");\n'
            f'        parseWebAssemblyModule(content, globalImportObject);\n'
            f'    }}).toThrow(TypeError, "Linking failed");'
        )

    if entry['kind'] in ('exhaustion', 'trap', 'invalid'):
        return (
            f'expect(() => {expectation}.toThrow(TypeError, "Execution trapped"));\n    '
        )

    if entry['kind'] == 'malformed':
        return ''

    if entry['kind'] == 'testgen_fail':
        raise TestGenerationError(entry["reason"])

    if not expectation:
        raise TestGenerationError(f"Unknown test result structure in {json.dumps(entry)}")

    return expectation


raw_test_number = 0


def gentest(entry, main_name):
    global raw_test_number
    isfunction = 'function' in entry
    name: str
    isempty = False
    if isfunction or 'get' in entry:
        name = json.dumps((entry["function"] if isfunction else entry["get"])["name"])[1:-1]
    else:
        isempty = True
        name = str(f"_inline_test_{raw_test_number}")
        raw_test_number += 1
    if type(name) is not str:
        print("Unsupported test case (call to", name, ")", file=stderr)
        return '\n    '
    ident = '_' + re.sub("[^a-zA-Z_0-9]", "_", name)
    count = all_names_in_main.get(name, 0)
    all_names_in_main[name] = count + 1
    test_name = f'execution of {main_name}: {name} (instance {count})'
    tmodule = 'module'
    if not isempty:
        key = "function" if "function" in entry else "get"
        if entry[key]['module'] is not None:
            tmodule = f'namedModules[{json.dumps(named_modules_inverse[entry[key]["module"]][0])}]'
    test = "_test"
    try:
        result = genresult(ident, entry, count)
    except TestGenerationError as e:
        test = f"/* {e.msg} */ _test.skip"
        result = ""
    return (
            f'{test}({json.dumps(test_name)}, () => {{\n' +
            (
                f'let {ident} = {tmodule}.getExport({json.dumps(name)});\n        '
                f'expect({ident}).not.toBeUndefined();\n        '
                if not isempty else ''
            ) +
            f'{result}'
            '});\n\n    '
    )


def gen_parse_module(name, index):
    export_string = ''
    if index in named_modules_inverse:
        entry = named_modules_inverse[index]
        export_string += f'namedModules[{json.dumps(entry[0])}] = module;\n    '
        if entry[1]:
            export_string += f'globalImportObject[{json.dumps(entry[1])}] = module;\n    '

    return (
        'let content, module;\n    '
        'try {\n        '
        f'content = readBinaryWasmFile("Fixtures/SpecTests/{name}.wasm");\n        '
        f'module = parseWebAssemblyModule(content, globalImportObject)\n        '
        '} catch(e) { _test("parse", () => expect().fail(e)); _test = test.skip; _test.skip = test.skip; }\n    '
        f'{export_string}\n     '
    )


def nth(a, x, y=None):
    if y:
        return a[x:y]
    return a[x]


def compile_wasm_source(mod, outpath):
    if not mod:
        return True
    if mod[0] == 'literal':
        with open(outpath, 'wb+') as f:
            f.write(mod[1])
            return True
    elif mod[0] == 'parse':
        with NamedTemporaryFile("w+") as temp:
            temp.write(mod[1])
            temp.flush()
            rc = call(["wat2wasm", "--enable-all", "--no-check", temp.name, "-o", outpath])
            return rc == 0
    return False


def main():
    global module_output_path
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
        if not compile_wasm_source(mod, outpath) and ('kind' not in description or description["kind"] != "malformed"):
            print("Failed to compile", name, "module index", index, "skipping that test", file=stderr)
            continue
        sep = ""
        print(f'''describe({json.dumps(testname)}, () => {{
let _test = test;
{gen_parse_module(testname, index) if mod else ''}
{sep.join(gentest(x, testname) for x in description["tests"])}
}});
''')


if __name__ == "__main__":
    main()
