/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/File.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
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

class WebAssemblyModule final : public JS::Object {
    JS_OBJECT(WebAssemblyModule, JS::Object);

public:
    explicit WebAssemblyModule(JS::Object& prototype)
        : JS::Object(prototype)
    {
    }

    static Wasm::AbstractMachine& machine() { return m_machine; }
    Wasm::Module& module() { return *m_module; }
    Wasm::ModuleInstance& module_instance() { return *m_module_instance; }

    static WebAssemblyModule* create(JS::GlobalObject& global_object, Wasm::Module module)
    {
        auto instance = global_object.heap().allocate<WebAssemblyModule>(global_object, *global_object.object_prototype());
        instance->m_module = move(module);
        if (auto result = machine().instantiate(*instance->m_module, {}); result.is_error())
            global_object.vm().throw_exception<JS::TypeError>(global_object, result.release_error().error);
        else
            instance->m_module_instance = result.release_value();
        return instance;
    }
    void initialize(JS::GlobalObject&) override;

    ~WebAssemblyModule() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(get_export);
    JS_DECLARE_NATIVE_FUNCTION(wasm_invoke);

    static Wasm::AbstractMachine m_machine;
    Optional<Wasm::Module> m_module;
    OwnPtr<Wasm::ModuleInstance> m_module_instance;
};

Wasm::AbstractMachine WebAssemblyModule::m_machine;

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

    if (stream.handle_any_error()) {
        vm.throw_exception<JS::SyntaxError>(global_object, "Bianry stream contained errors");
        return {};
    }
    return WebAssemblyModule::create(global_object, result.release_value());
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

void WebAssemblyModule::initialize(JS::GlobalObject& global_object)
{
    Base::initialize(global_object);
    define_native_function("getExport", get_export);
    define_native_function("invoke", wasm_invoke);
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyModule::get_export)
{
    auto name = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto this_value = vm.this_value(global_object);
    auto object = this_value.to_object(global_object);
    if (vm.exception())
        return {};
    if (!object || !is<WebAssemblyModule>(object)) {
        vm.throw_exception<JS::TypeError>(global_object, "Not a WebAssemblyModule");
        return {};
    }
    auto instance = static_cast<WebAssemblyModule*>(object);
    for (auto& entry : instance->module_instance().exports()) {
        if (entry.name() == name) {
            auto& value = entry.value();
            if (auto ptr = value.get_pointer<Wasm::FunctionAddress>())
                return JS::Value(static_cast<unsigned long>(ptr->value()));
            vm.throw_exception<JS::TypeError>(global_object, String::formatted("'{}' does not refer to a function", name));
            return {};
        }
    }
    vm.throw_exception<JS::TypeError>(global_object, String::formatted("'{}' could not be found", name));
    return {};
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyModule::wasm_invoke)
{
    auto address = static_cast<unsigned long>(vm.argument(0).to_double(global_object));
    if (vm.exception())
        return {};
    Wasm::FunctionAddress function_address { address };
    auto function_instance = WebAssemblyModule::machine().store().get(function_address);
    if (!function_instance) {
        vm.throw_exception<JS::TypeError>(global_object, "Invalid function address");
        return {};
    }

    const Wasm::FunctionType* type { nullptr };
    function_instance->visit([&](auto& value) { type = &value.type(); });
    if (!type) {
        vm.throw_exception<JS::TypeError>(global_object, "Invalid function found at given address");
        return {};
    }

    Vector<Wasm::Value> arguments;
    if (type->parameters().size() + 1 > vm.argument_count()) {
        vm.throw_exception<JS::TypeError>(global_object, String::formatted("Expected {} arguments for call, but found {}", type->parameters().size() + 1, vm.argument_count()));
        return {};
    }
    size_t index = 1;
    for (auto& param : type->parameters()) {
        auto value = vm.argument(index++).to_double(global_object);
        switch (param.kind()) {
        case Wasm::ValueType::Kind::I32:
            arguments.append(Wasm::Value(param, static_cast<u64>(value)));
            break;
        case Wasm::ValueType::Kind::I64:
            arguments.append(Wasm::Value(param, static_cast<u64>(value)));
            break;
        case Wasm::ValueType::Kind::F32:
            arguments.append(Wasm::Value(static_cast<float>(value)));
            break;
        case Wasm::ValueType::Kind::F64:
            arguments.append(Wasm::Value(static_cast<double>(value)));
            break;
        case Wasm::ValueType::Kind::FunctionReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Func { static_cast<u64>(value) } }));
            break;
        case Wasm::ValueType::Kind::ExternReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Func { static_cast<u64>(value) } }));
            break;
        case Wasm::ValueType::Kind::NullFunctionReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Null { Wasm::ValueType(Wasm::ValueType::Kind::FunctionReference) } }));
            break;
        case Wasm::ValueType::Kind::NullExternReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Null { Wasm::ValueType(Wasm::ValueType::Kind::ExternReference) } }));
            break;
        }
    }

    auto result = WebAssemblyModule::machine().invoke(function_address, arguments);
    if (result.is_trap()) {
        vm.throw_exception<JS::TypeError>(global_object, "Execution trapped");
        return {};
    }

    if (result.values().is_empty())
        return JS::js_null();

    JS::Value return_value;
    result.values().first().value().visit(
        [&](const auto& value) { return_value = JS::Value(static_cast<double>(value)); },
        [&](const Wasm::Reference& reference) {
            reference.ref().visit(
                [&](const Wasm::Reference::Null&) { return_value = JS::js_null(); },
                [&](const auto& ref) { return_value = JS::Value(static_cast<double>(ref.address.value())); });
        });
    return return_value;
}
