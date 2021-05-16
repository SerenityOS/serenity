/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyObject::WebAssemblyObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void WebAssemblyObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);

    define_native_function("validate", validate, 1);
    define_native_function("compile", compile, 1);
    define_native_function("instantiate", instantiate, 1);
}

NonnullOwnPtrVector<WebAssemblyObject::CompiledWebAssemblyModule> WebAssemblyObject::s_compiled_modules;
NonnullOwnPtrVector<Wasm::ModuleInstance> WebAssemblyObject::s_instantiated_modules;
Wasm::AbstractMachine WebAssemblyObject::s_abstract_machine;

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::validate)
{
    // FIXME: Implement this once module validation is implemented in LibWasm.
    dbgln("Hit WebAssemblyObject::validate() stub!");
    return JS::Value { true };
}

static Result<size_t, JS::Value> parse_module(JS::GlobalObject& global_object, JS::Object* buffer)
{
    ByteBuffer* bytes;
    if (is<JS::ArrayBuffer>(buffer)) {
        auto array_buffer = static_cast<JS::ArrayBuffer*>(buffer);
        bytes = &array_buffer->buffer();
    } else if (is<JS::TypedArrayBase>(buffer)) {
        auto array = static_cast<JS::TypedArrayBase*>(buffer);
        bytes = &array->viewed_array_buffer()->buffer();
    } else {
        auto error = JS::TypeError::create(global_object, String::formatted("{} is not an ArrayBuffer", buffer->class_name()));
        return JS::Value { error };
    }
    InputMemoryStream stream { *bytes };
    auto module_result = Wasm::Module::parse(stream);
    if (module_result.is_error()) {
        // FIXME: Throw CompileError instead.
        auto error = JS::TypeError::create(global_object, Wasm::parse_error_to_string(module_result.error()));
        return JS::Value { error };
    }

    WebAssemblyObject::s_compiled_modules.append(make<WebAssemblyObject::CompiledWebAssemblyModule>(module_result.release_value()));
    return WebAssemblyObject::s_compiled_modules.size() - 1;
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::compile)
{
    // FIXME: This shouldn't block!
    auto buffer = vm.argument(0).to_object(global_object);
    JS::Value rejection_value;
    if (vm.exception()) {
        rejection_value = vm.exception()->value();
        vm.clear_exception();
    }
    auto promise = JS::Promise::create(global_object);
    if (!rejection_value.is_empty()) {
        promise->reject(rejection_value);
        return promise;
    }
    auto result = parse_module(global_object, buffer);
    if (result.is_error())
        promise->reject(result.error());
    else
        promise->fulfill(vm.heap().allocate<WebAssemblyModuleObject>(global_object, global_object, result.value()));
    return promise;
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::instantiate)
{
    // FIXME: This shouldn't block!
    auto buffer = vm.argument(0).to_object(global_object);
    JS::Value rejection_value;
    if (vm.exception()) {
        rejection_value = vm.exception()->value();
        vm.clear_exception();
    }
    auto promise = JS::Promise::create(global_object);
    if (!rejection_value.is_empty()) {
        promise->reject(rejection_value);
        return promise;
    }

    const Wasm::Module* module { nullptr };
    if (is<JS::ArrayBuffer>(buffer) || is<JS::TypedArrayBase>(buffer)) {
        auto result = parse_module(global_object, buffer);
        if (result.is_error()) {
            promise->reject(result.error());
            return promise;
        }
        module = &WebAssemblyObject::s_compiled_modules.at(result.value()).module;
    } else if (is<WebAssemblyModuleObject>(buffer)) {
        module = &static_cast<WebAssemblyModuleObject*>(buffer)->module();
    } else {
        auto error = JS::TypeError::create(global_object, String::formatted("{} is not an ArrayBuffer or a Module", buffer->class_name()));
        promise->reject(error);
        return promise;
    }
    VERIFY(module);

    HashMap<Wasm::Linker::Name, Wasm::ExternValue> import_values;
    auto import_argument = vm.argument(1);
    if (!import_argument.is_undefined()) {
        [[maybe_unused]] auto import_object = import_argument.to_object(global_object);
        if (vm.exception()) {
            rejection_value = vm.exception()->value();
            vm.clear_exception();
        }
        auto promise = JS::Promise::create(global_object);
        if (!rejection_value.is_empty()) {
            promise->reject(rejection_value);
            return promise;
        }

        // FIXME: Populate the import values.
    }

    Wasm::Linker linker { *module };
    linker.link(import_values);
    auto link_result = linker.finish();
    if (link_result.is_error()) {
        // FIXME: Throw a LinkError.
        StringBuilder builder;
        builder.append("LinkError: Missing ");
        builder.join(' ', link_result.error().missing_imports);
        auto error = JS::TypeError::create(global_object, builder.build());
        promise->reject(error);
        return promise;
    }

    auto instance_result = s_abstract_machine.instantiate(*module, link_result.release_value());
    if (instance_result.is_error()) {
        auto error = JS::TypeError::create(global_object, instance_result.error().error);
        promise->reject(error);
        return promise;
    }

    s_instantiated_modules.append(instance_result.release_value());
    promise->fulfill(vm.heap().allocate<WebAssemblyInstanceObject>(global_object, global_object, s_instantiated_modules.size() - 1));
    return promise;
}

WebAssemblyModuleObject::WebAssemblyModuleObject(JS::GlobalObject& global_object, size_t index)
    : Object(*global_object.object_prototype())
    , m_index(index)
{
}

WebAssemblyInstanceObject::WebAssemblyInstanceObject(JS::GlobalObject& global_object, size_t index)
    : Object(*global_object.object_prototype())
    , m_index(index)
{
}

static JS::NativeFunction* create_native_function(Wasm::FunctionAddress address, String name, JS::GlobalObject& global_object);

static JS::Value to_js_value(Wasm::Value& wasm_value, JS::GlobalObject& global_object)
{
    switch (wasm_value.type().kind()) {
    case Wasm::ValueType::I64:
        // FIXME: This is extremely silly...
        return global_object.heap().allocate<JS::BigInt>(global_object, Crypto::SignedBigInteger::from_base10(String::number(wasm_value.to<i64>().value())));
    case Wasm::ValueType::I32:
        return JS::Value(wasm_value.to<i32>().value());
    case Wasm::ValueType::F64:
        return JS::Value(static_cast<double>(wasm_value.to<float>().value()));
    case Wasm::ValueType::F32:
        return JS::Value(wasm_value.to<double>().value());
    case Wasm::ValueType::FunctionReference:
        // FIXME: What's the name of a function reference that isn't exported?
        return create_native_function(wasm_value.to<Wasm::FunctionAddress>().value(), "FIXME_IHaveNoIdeaWhatThisShouldBeCalled", global_object);
    case Wasm::ValueType::ExternReference:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

static Optional<Wasm::Value> to_webassembly_value(JS::Value value, const Wasm::ValueType& type, JS::GlobalObject& global_object)
{
    static Crypto::SignedBigInteger two_64 = "1"_sbigint.shift_left(64);
    auto& vm = global_object.vm();

    switch (type.kind()) {
    case Wasm::ValueType::I64: {
        auto bigint = value.to_bigint(global_object);
        if (vm.exception())
            return {};
        auto value = bigint->big_integer().divided_by(two_64).remainder;
        VERIFY(value.trimmed_length() <= 2);
        BigEndian<i64> integer { 0 };
        value.export_data({ &integer, 2 });
        return Wasm::Value { static_cast<i64>(integer) };
    }
    case Wasm::ValueType::I32: {
        auto _i32 = value.to_i32(global_object);
        if (vm.exception())
            return {};
        return Wasm::Value { static_cast<i32>(_i32) };
    }
    case Wasm::ValueType::F64: {
        auto number = value.to_double(global_object);
        if (vm.exception())
            return {};
        return Wasm::Value { static_cast<double>(number) };
    }
    case Wasm::ValueType::F32: {
        auto number = value.to_double(global_object);
        if (vm.exception())
            return {};
        return Wasm::Value { static_cast<float>(number) };
    }
    case Wasm::ValueType::FunctionReference:
    case Wasm::ValueType::ExternReference:
        TODO();
    }

    VERIFY_NOT_REACHED();
}

JS::NativeFunction* create_native_function(Wasm::FunctionAddress address, String name, JS::GlobalObject& global_object)
{
    // FIXME: Cache these.
    return JS::NativeFunction::create(
        global_object,
        name,
        [address](JS::VM& vm, JS::GlobalObject& global_object) -> JS::Value {
            Vector<Wasm::Value> values;
            Optional<Wasm::FunctionType> type;
            WebAssemblyObject::s_abstract_machine.store().get(address)->visit([&](const auto& value) { type = value.type(); });

            // Grab as many values as needed and convert them.
            size_t index = 0;
            for (auto& type : type.value().parameters()) {
                auto result = to_webassembly_value(vm.argument(index++), type, global_object);
                if (result.has_value())
                    values.append(result.release_value());
                else
                    return {};
            }

            auto result = WebAssemblyObject::s_abstract_machine.invoke(address, move(values));
            // FIXME: Use the convoluted mapping of errors defined in the spec.
            if (result.is_trap()) {
                vm.throw_exception<JS::TypeError>(global_object, "Wasm execution trapped (WIP)");
                return {};
            }

            if (result.values().is_empty())
                return JS::js_undefined();

            if (result.values().size() == 1)
                return to_js_value(result.values().first(), global_object);

            Vector<JS::Value> result_values;
            for (auto& entry : result.values())
                result_values.append(to_js_value(entry, global_object));

            return JS::Array::create_from(global_object, result_values);
        });
}

void WebAssemblyInstancePrototype::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_property("exports", exports_getter, nullptr);
}

void WebAssemblyInstanceObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);

    VERIFY(!m_exports_object);
    m_exports_object = JS::Object::create_empty(global_object);
    m_exports_object->set_prototype(nullptr);
    auto& instance = this->instance();
    for (auto& export_ : instance.exports()) {
        export_.value().visit(
            [&](const Wasm::FunctionAddress& address) {
                auto function = create_native_function(address, export_.name(), global_object);
                m_exports_object->define_property(export_.name(), function);
            },
            [&](const auto&) {
                // FIXME: Implement other exports!
            });
    }

    m_exports_object->set_integrity_level(IntegrityLevel::Frozen);
}

JS_DEFINE_NATIVE_GETTER(WebAssemblyInstancePrototype::exports_getter)
{
    auto this_value = vm.this_value(global_object);
    auto this_object = this_value.to_object(global_object);
    if (vm.exception())
        return {};
    if (!is<WebAssemblyInstanceObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAn, "WebAssemblyInstance");
        return {};
    }
    auto object = static_cast<WebAssemblyInstanceObject*>(this_object);
    return object->m_exports_object;
}

void WebAssemblyInstanceObject::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(m_exports_object);
}

}
