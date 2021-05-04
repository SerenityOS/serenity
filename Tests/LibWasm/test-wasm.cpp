/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/File.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/Types.h>

TEST_ROOT("Userland/Libraries/LibWasm/Tests");

TESTJS_GLOBAL_FUNCTION(read_binary_wasm_file, readBinaryWasmFile)
{
    auto filename = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto file = Core::File::open(filename, Core::OpenMode::ReadOnly);
    if (file.is_error()) {
        vm.throw_exception<JS::TypeError>(global_object, file.error());
        return {};
    }
    auto contents = file.value()->read_all();
    auto array = JS::Uint8Array::create(global_object, contents.size());
    contents.span().copy_to(array->data());
    return array;
}

TESTJS_GLOBAL_FUNCTION(parse_webassembly_module, parseWebAssemblyModule)
{
    auto object = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    if (!is<JS::Uint8Array>(object)) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a Uint8Array argument to parse_webassembly_module");
        return {};
    }
    auto& array = static_cast<JS::Uint8Array&>(*object);
    InputMemoryStream stream { array.data() };
    auto result = Wasm::Module::parse(stream);
    if (result.is_error()) {
        vm.throw_exception<JS::SyntaxError>(global_object, Wasm::parse_error_to_string(result.error()));
        return {};
    }
    if (stream.handle_any_error())
        return JS::js_undefined();
    return JS::js_null();
}

TESTJS_GLOBAL_FUNCTION(compare_typed_arrays, compareTypedArrays)
{
    auto lhs = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};
    if (!is<JS::TypedArrayBase>(lhs)) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a TypedArray");
        return {};
    }
    auto& lhs_array = static_cast<JS::TypedArrayBase&>(*lhs);
    auto rhs = vm.argument(1).to_object(global_object);
    if (vm.exception())
        return {};
    if (!is<JS::TypedArrayBase>(rhs)) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a TypedArray");
        return {};
    }
    auto& rhs_array = static_cast<JS::TypedArrayBase&>(*rhs);
    return JS::Value(lhs_array.viewed_array_buffer()->buffer() == rhs_array.viewed_array_buffer()->buffer());
}
