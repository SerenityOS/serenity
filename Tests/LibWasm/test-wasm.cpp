/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibTest/JavaScriptTestRunner.h>
#include <LibWasm/AbstractMachine/BytecodeInterpreter.h>
#include <LibWasm/Types.h>
#include <string.h>

TEST_ROOT("Userland/Libraries/LibWasm/Tests");

TESTJS_GLOBAL_FUNCTION(read_binary_wasm_file, readBinaryWasmFile)
{
    auto& realm = *global_object.associated_realm();
    auto filename = TRY(vm.argument(0).to_string(global_object));
    auto file = Core::Stream::File::open(filename, Core::Stream::OpenMode::Read);
    if (file.is_error())
        return vm.throw_completion<JS::TypeError>(strerror(file.error().code()));

    auto file_size = file.value()->size();
    if (file_size.is_error())
        return vm.throw_completion<JS::TypeError>(strerror(file_size.error().code()));

    auto* array = TRY(JS::Uint8Array::create(realm, file_size.value()));

    auto read = file.value()->read(array->data());
    if (read.is_error())
        return vm.throw_completion<JS::TypeError>(strerror(read.error().code()));

    return JS::Value(array);
}

class WebAssemblyModule final : public JS::Object {
    JS_OBJECT(WebAssemblyModule, JS::Object);

public:
    explicit WebAssemblyModule(JS::Object& prototype)
        : JS::Object(prototype)
    {
        m_machine.enable_instruction_count_limit();
    }

    static Wasm::AbstractMachine& machine() { return m_machine; }
    Wasm::Module& module() { return *m_module; }
    Wasm::ModuleInstance& module_instance() { return *m_module_instance; }

    static JS::ThrowCompletionOr<WebAssemblyModule*> create(JS::Realm& realm, Wasm::Module module, HashMap<Wasm::Linker::Name, Wasm::ExternValue> const& imports)
    {
        auto& vm = realm.vm();
        auto* instance = realm.heap().allocate<WebAssemblyModule>(realm, *realm.global_object().object_prototype());
        instance->m_module = move(module);
        Wasm::Linker linker(*instance->m_module);
        linker.link(imports);
        linker.link(spec_test_namespace());
        auto link_result = linker.finish();
        if (link_result.is_error())
            return vm.throw_completion<JS::TypeError>("Link failed");
        auto result = machine().instantiate(*instance->m_module, link_result.release_value());
        if (result.is_error())
            return vm.throw_completion<JS::TypeError>(result.release_error().error);
        instance->m_module_instance = result.release_value();
        return instance;
    }
    void initialize(JS::Realm&) override;

    ~WebAssemblyModule() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(get_export);
    JS_DECLARE_NATIVE_FUNCTION(wasm_invoke);

    static HashMap<Wasm::Linker::Name, Wasm::ExternValue> const& spec_test_namespace()
    {
        if (!s_spec_test_namespace.is_empty())
            return s_spec_test_namespace;
        Wasm::FunctionType print_i32_type { { Wasm::ValueType(Wasm::ValueType::I32) }, {} };

        auto address = m_machine.store().allocate(Wasm::HostFunction {
            [](auto&, auto&) -> Wasm::Result {
                // Noop, this just needs to exist.
                return Wasm::Result { Vector<Wasm::Value> {} };
            },
            print_i32_type });
        s_spec_test_namespace.set({ "spectest", "print_i32", print_i32_type }, Wasm::ExternValue { *address });

        return s_spec_test_namespace;
    }

    static HashMap<Wasm::Linker::Name, Wasm::ExternValue> s_spec_test_namespace;
    static Wasm::AbstractMachine m_machine;
    Optional<Wasm::Module> m_module;
    OwnPtr<Wasm::ModuleInstance> m_module_instance;
};

Wasm::AbstractMachine WebAssemblyModule::m_machine;
HashMap<Wasm::Linker::Name, Wasm::ExternValue> WebAssemblyModule::s_spec_test_namespace;

TESTJS_GLOBAL_FUNCTION(parse_webassembly_module, parseWebAssemblyModule)
{
    auto& realm = *global_object.associated_realm();
    auto* object = TRY(vm.argument(0).to_object(global_object));
    if (!is<JS::Uint8Array>(object))
        return vm.throw_completion<JS::TypeError>("Expected a Uint8Array argument to parse_webassembly_module");
    auto& array = static_cast<JS::Uint8Array&>(*object);
    InputMemoryStream stream { array.data() };
    ScopeGuard handle_stream_error {
        [&] {
            stream.handle_any_error();
        }
    };
    auto result = Wasm::Module::parse(stream);
    if (result.is_error())
        return vm.throw_completion<JS::SyntaxError>(Wasm::parse_error_to_string(result.error()));

    if (stream.handle_any_error())
        return vm.throw_completion<JS::SyntaxError>("Binary stream contained errors");

    HashMap<Wasm::Linker::Name, Wasm::ExternValue> imports;
    auto import_value = vm.argument(1);
    if (import_value.is_object()) {
        auto& import_object = import_value.as_object();
        for (auto& property : import_object.shape().property_table()) {
            auto value = import_object.get_without_side_effects(property.key);
            if (!value.is_object() || !is<WebAssemblyModule>(value.as_object()))
                continue;
            auto& module_object = static_cast<WebAssemblyModule&>(value.as_object());
            for (auto& entry : module_object.module_instance().exports()) {
                // FIXME: Don't pretend that everything is a function
                imports.set({ property.key.as_string(), entry.name(), Wasm::TypeIndex(0) }, entry.value());
            }
        }
    }

    return JS::Value(TRY(WebAssemblyModule::create(realm, result.release_value(), imports)));
}

TESTJS_GLOBAL_FUNCTION(compare_typed_arrays, compareTypedArrays)
{
    auto* lhs = TRY(vm.argument(0).to_object(global_object));
    if (!is<JS::TypedArrayBase>(lhs))
        return vm.throw_completion<JS::TypeError>("Expected a TypedArray");
    auto& lhs_array = static_cast<JS::TypedArrayBase&>(*lhs);
    auto* rhs = TRY(vm.argument(1).to_object(global_object));
    if (!is<JS::TypedArrayBase>(rhs))
        return vm.throw_completion<JS::TypeError>("Expected a TypedArray");
    auto& rhs_array = static_cast<JS::TypedArrayBase&>(*rhs);
    return JS::Value(lhs_array.viewed_array_buffer()->buffer() == rhs_array.viewed_array_buffer()->buffer());
}

void WebAssemblyModule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    define_native_function("getExport", get_export, 1, JS::default_attributes);
    define_native_function("invoke", wasm_invoke, 1, JS::default_attributes);
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyModule::get_export)
{
    auto name = TRY(vm.argument(0).to_string(global_object));
    auto this_value = vm.this_value();
    auto* object = TRY(this_value.to_object(global_object));
    if (!is<WebAssemblyModule>(object))
        return vm.throw_completion<JS::TypeError>("Not a WebAssemblyModule");
    auto instance = static_cast<WebAssemblyModule*>(object);
    for (auto& entry : instance->module_instance().exports()) {
        if (entry.name() == name) {
            auto& value = entry.value();
            if (auto ptr = value.get_pointer<Wasm::FunctionAddress>())
                return JS::Value(static_cast<unsigned long>(ptr->value()));
            if (auto v = value.get_pointer<Wasm::GlobalAddress>()) {
                return m_machine.store().get(*v)->value().value().visit(
                    [&](auto const& value) -> JS::Value { return JS::Value(static_cast<double>(value)); },
                    [&](i32 value) { return JS::Value(static_cast<double>(value)); },
                    [&](i64 value) -> JS::Value { return JS::js_bigint(vm, Crypto::SignedBigInteger::create_from(value)); },
                    [&](Wasm::Reference const& reference) -> JS::Value {
                        return reference.ref().visit(
                            [&](const Wasm::Reference::Null&) -> JS::Value { return JS::js_null(); },
                            [&](const auto& ref) -> JS::Value { return JS::Value(static_cast<double>(ref.address.value())); });
                    });
            }
            return vm.throw_completion<JS::TypeError>(String::formatted("'{}' does not refer to a function or a global", name));
        }
    }
    return vm.throw_completion<JS::TypeError>(String::formatted("'{}' could not be found", name));
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyModule::wasm_invoke)
{
    auto address = static_cast<unsigned long>(TRY(vm.argument(0).to_double(global_object)));
    Wasm::FunctionAddress function_address { address };
    auto function_instance = WebAssemblyModule::machine().store().get(function_address);
    if (!function_instance)
        return vm.throw_completion<JS::TypeError>("Invalid function address");

    Wasm::FunctionType const* type { nullptr };
    function_instance->visit([&](auto& value) { type = &value.type(); });
    if (!type)
        return vm.throw_completion<JS::TypeError>("Invalid function found at given address");

    Vector<Wasm::Value> arguments;
    if (type->parameters().size() + 1 > vm.argument_count())
        return vm.throw_completion<JS::TypeError>(String::formatted("Expected {} arguments for call, but found {}", type->parameters().size() + 1, vm.argument_count()));
    size_t index = 1;
    for (auto& param : type->parameters()) {
        auto argument = vm.argument(index++);
        double double_value = 0;
        if (!argument.is_bigint())
            double_value = TRY(argument.to_double(global_object));
        switch (param.kind()) {
        case Wasm::ValueType::Kind::I32:
            arguments.append(Wasm::Value(param, static_cast<u64>(double_value)));
            break;
        case Wasm::ValueType::Kind::I64:
            if (argument.is_bigint()) {
                auto value = TRY(argument.to_bigint_int64(global_object));
                arguments.append(Wasm::Value(param, bit_cast<u64>(value)));
            } else {
                arguments.append(Wasm::Value(param, static_cast<u64>(double_value)));
            }
            break;
        case Wasm::ValueType::Kind::F32:
            arguments.append(Wasm::Value(static_cast<float>(double_value)));
            break;
        case Wasm::ValueType::Kind::F64:
            arguments.append(Wasm::Value(static_cast<double>(double_value)));
            break;
        case Wasm::ValueType::Kind::FunctionReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Func { static_cast<u64>(double_value) } }));
            break;
        case Wasm::ValueType::Kind::ExternReference:
            arguments.append(Wasm::Value(Wasm::Reference { Wasm::Reference::Func { static_cast<u64>(double_value) } }));
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
    if (result.is_trap())
        return vm.throw_completion<JS::TypeError>(String::formatted("Execution trapped: {}", result.trap().reason));

    if (result.values().is_empty())
        return JS::js_null();

    JS::Value return_value;
    result.values().first().value().visit(
        [&](auto const& value) { return_value = JS::Value(static_cast<double>(value)); },
        [&](i32 value) { return_value = JS::Value(static_cast<double>(value)); },
        [&](i64 value) { return_value = JS::Value(JS::js_bigint(vm, Crypto::SignedBigInteger::create_from(value))); },
        [&](Wasm::Reference const& reference) {
            reference.ref().visit(
                [&](const Wasm::Reference::Null&) { return_value = JS::js_null(); },
                [&](const auto& ref) { return_value = JS::Value(static_cast<double>(ref.address.value())); });
        });
    return return_value;
}
