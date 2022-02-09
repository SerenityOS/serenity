/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebAssemblyInstanceObject.h"
#include "WebAssemblyMemoryPrototype.h"
#include "WebAssemblyModuleConstructor.h"
#include "WebAssemblyModuleObject.h"
#include "WebAssemblyModulePrototype.h"
#include "WebAssemblyTableObject.h"
#include "WebAssemblyTablePrototype.h"
#include <AK/ScopeGuard.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/AbstractMachine/Validator.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyObject::WebAssemblyObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
    s_abstract_machine.enable_instruction_count_limit();
}

void WebAssemblyObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);

    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_function("validate", validate, 1, attr);
    define_native_function("compile", compile, 1, attr);
    define_native_function("instantiate", instantiate, 1, attr);

    auto& vm = global_object.vm();

    auto& window = static_cast<WindowObject&>(global_object);
    auto& memory_constructor = window.ensure_web_constructor<WebAssemblyMemoryConstructor>("WebAssembly.Memory");
    memory_constructor.define_direct_property(vm.names.name, js_string(vm, "WebAssembly.Memory"), JS::Attribute::Configurable);
    auto& memory_prototype = window.ensure_web_prototype<WebAssemblyMemoryPrototype>("WebAssemblyMemoryPrototype");
    memory_prototype.define_direct_property(vm.names.constructor, &memory_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
    define_direct_property("Memory", &memory_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);

    auto& instance_constructor = window.ensure_web_constructor<WebAssemblyInstanceConstructor>("WebAssembly.Instance");
    instance_constructor.define_direct_property(vm.names.name, js_string(vm, "WebAssembly.Instance"), JS::Attribute::Configurable);
    auto& instance_prototype = window.ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype");
    instance_prototype.define_direct_property(vm.names.constructor, &instance_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
    define_direct_property("Instance", &instance_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);

    auto& module_constructor = window.ensure_web_constructor<WebAssemblyModuleConstructor>("WebAssembly.Module");
    module_constructor.define_direct_property(vm.names.name, js_string(vm, "WebAssembly.Module"), JS::Attribute::Configurable);
    auto& module_prototype = window.ensure_web_prototype<WebAssemblyModulePrototype>("WebAssemblyModulePrototype");
    module_prototype.define_direct_property(vm.names.constructor, &module_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
    define_direct_property("Module", &module_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);

    auto& table_constructor = window.ensure_web_constructor<WebAssemblyTableConstructor>("WebAssembly.Table");
    table_constructor.define_direct_property(vm.names.name, js_string(vm, "WebAssembly.Table"), JS::Attribute::Configurable);
    auto& table_prototype = window.ensure_web_prototype<WebAssemblyTablePrototype>("WebAssemblyTablePrototype");
    table_prototype.define_direct_property(vm.names.constructor, &table_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
    define_direct_property("Table", &table_constructor, JS::Attribute::Writable | JS::Attribute::Configurable);
}

NonnullOwnPtrVector<WebAssemblyObject::CompiledWebAssemblyModule> WebAssemblyObject::s_compiled_modules;
NonnullOwnPtrVector<Wasm::ModuleInstance> WebAssemblyObject::s_instantiated_modules;
Vector<WebAssemblyObject::ModuleCache> WebAssemblyObject::s_module_caches;
WebAssemblyObject::GlobalModuleCache WebAssemblyObject::s_global_cache;
Wasm::AbstractMachine WebAssemblyObject::s_abstract_machine;

void WebAssemblyObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    for (auto& entry : s_global_cache.function_instances)
        visitor.visit(entry.value);
    for (auto& module_cache : s_module_caches) {
        for (auto& entry : module_cache.function_instances)
            visitor.visit(entry.value);
        for (auto& entry : module_cache.memory_instances)
            visitor.visit(entry.value);
    }
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::validate)
{
    // 1. Let stableBytes be a copy of the bytes held by the buffer bytes.
    // Note: There's no need to copy the bytes here as the buffer data cannot change while we're compiling the module.
    auto buffer = TRY(vm.argument(0).to_object(global_object));

    // 2. Compile stableBytes as a WebAssembly module and store the results as module.
    auto maybe_module = parse_module(global_object, buffer);

    // 3. If module is error, return false.
    if (maybe_module.is_error())
        return JS::Value(false);

    // Drop the module from the cache, we're never going to refer to it.
    ScopeGuard drop_from_cache {
        [&] {
            (void)s_compiled_modules.take_last();
        }
    };

    // 3 continued - our "compile" step is lazy with validation, explicitly do the validation.
    if (s_abstract_machine.validate(s_compiled_modules[maybe_module.value()].module).is_error())
        return JS::Value(false);

    // 4. Return true.
    return JS::Value(true);
}

JS::ThrowCompletionOr<size_t> parse_module(JS::GlobalObject& global_object, JS::Object* buffer_object)
{
    auto& vm = global_object.vm();

    ReadonlyBytes data;
    if (is<JS::ArrayBuffer>(buffer_object)) {
        auto& buffer = static_cast<JS::ArrayBuffer&>(*buffer_object);
        data = buffer.buffer();
    } else if (is<JS::TypedArrayBase>(buffer_object)) {
        auto& buffer = static_cast<JS::TypedArrayBase&>(*buffer_object);
        data = buffer.viewed_array_buffer()->buffer().span().slice(buffer.byte_offset(), buffer.byte_length());
    } else if (is<JS::DataView>(buffer_object)) {
        auto& buffer = static_cast<JS::DataView&>(*buffer_object);
        data = buffer.viewed_array_buffer()->buffer().span().slice(buffer.byte_offset(), buffer.byte_length());
    } else {
        return vm.throw_completion<JS::TypeError>(global_object, "Not a BufferSource");
    }
    InputMemoryStream stream { data };
    auto module_result = Wasm::Module::parse(stream);
    ScopeGuard drain_errors {
        [&] {
            stream.handle_any_error();
        }
    };
    if (module_result.is_error()) {
        // FIXME: Throw CompileError instead.
        return vm.throw_completion<JS::TypeError>(global_object, Wasm::parse_error_to_string(module_result.error()));
    }

    if (auto validation_result = WebAssemblyObject::s_abstract_machine.validate(module_result.value()); validation_result.is_error()) {
        // FIXME: Throw CompileError instead.
        return vm.throw_completion<JS::TypeError>(global_object, validation_result.error().error_string);
    }

    WebAssemblyObject::s_compiled_modules.append(make<WebAssemblyObject::CompiledWebAssemblyModule>(module_result.release_value()));
    return WebAssemblyObject::s_compiled_modules.size() - 1;
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::compile)
{
    // FIXME: This shouldn't block!
    auto buffer_or_error = vm.argument(0).to_object(global_object);
    JS::Value rejection_value;
    if (buffer_or_error.is_error())
        rejection_value = *buffer_or_error.throw_completion().value();

    auto promise = JS::Promise::create(global_object);
    if (!rejection_value.is_empty()) {
        promise->reject(rejection_value);
        return promise;
    }
    auto* buffer = buffer_or_error.release_value();
    auto result = parse_module(global_object, buffer);
    if (result.is_error())
        promise->reject(*result.release_error().value());
    else
        promise->fulfill(vm.heap().allocate<WebAssemblyModuleObject>(global_object, global_object, result.release_value()));
    return promise;
}

JS::ThrowCompletionOr<size_t> WebAssemblyObject::instantiate_module(Wasm::Module const& module, JS::VM& vm, JS::GlobalObject& global_object)
{
    Wasm::Linker linker { module };
    HashMap<Wasm::Linker::Name, Wasm::ExternValue> resolved_imports;
    auto import_argument = vm.argument(1);
    if (!import_argument.is_undefined()) {
        auto* import_object = TRY(import_argument.to_object(global_object));
        dbgln("Trying to resolve stuff because import object was specified");
        for (const Wasm::Linker::Name& import_name : linker.unresolved_imports()) {
            dbgln("Trying to resolve {}::{}", import_name.module, import_name.name);
            auto value_or_error = import_object->get(import_name.module);
            if (value_or_error.is_error())
                break;
            auto value = value_or_error.release_value();
            auto object_or_error = value.to_object(global_object);
            if (object_or_error.is_error())
                break;
            auto* object = object_or_error.release_value();
            auto import_or_error = object->get(import_name.name);
            if (import_or_error.is_error())
                break;
            auto import_ = import_or_error.release_value();
            TRY(import_name.type.visit(
                [&](Wasm::TypeIndex index) -> JS::ThrowCompletionOr<void> {
                    dbgln("Trying to resolve a function {}::{}, type index {}", import_name.module, import_name.name, index.value());
                    auto& type = module.type(index);
                    // FIXME: IsCallable()
                    if (!import_.is_function())
                        return {};
                    auto& function = import_.as_function();
                    // FIXME: If this is a function created by create_native_function(),
                    //        just extract its address and resolve to that.
                    Wasm::HostFunction host_function {
                        [&](auto&, auto& arguments) -> Wasm::Result {
                            JS::MarkedVector<JS::Value> argument_values { vm.heap() };
                            for (auto& entry : arguments)
                                argument_values.append(to_js_value(global_object, entry));

                            auto result_or_error = JS::call(global_object, function, JS::js_undefined(), move(argument_values));
                            if (result_or_error.is_error()) {
                                return Wasm::Trap();
                            }
                            if (type.results().is_empty())
                                return Wasm::Result { Vector<Wasm::Value> {} };

                            if (type.results().size() == 1) {
                                auto value_or_error = to_webassembly_value(global_object, result_or_error.release_value(), type.results().first());
                                if (value_or_error.is_error())
                                    return Wasm::Trap {};

                                return Wasm::Result { Vector<Wasm::Value> { value_or_error.release_value() } };
                            }

                            // FIXME: Multiple returns
                            TODO();
                        },
                        type
                    };
                    auto address = s_abstract_machine.store().allocate(move(host_function));
                    dbgln("Resolved to {}", address->value());
                    // FIXME: LinkError instead.
                    VERIFY(address.has_value());

                    resolved_imports.set(import_name, Wasm::ExternValue { Wasm::FunctionAddress { *address } });
                    return {};
                },
                [&](Wasm::GlobalType const& type) -> JS::ThrowCompletionOr<void> {
                    Optional<Wasm::GlobalAddress> address;
                    // https://webassembly.github.io/spec/js-api/#read-the-imports step 5.1
                    if (import_.is_number() || import_.is_bigint()) {
                        if (import_.is_number() && type.type().kind() == Wasm::ValueType::I64) {
                            // FIXME: Throw a LinkError instead.
                            return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Import resolution attempted to cast a Number to a BigInteger");
                        }
                        if (import_.is_bigint() && type.type().kind() != Wasm::ValueType::I64) {
                            // FIXME: Throw a LinkError instead.
                            return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Import resolution attempted to cast a BigInteger to a Number");
                        }
                        auto cast_value = TRY(to_webassembly_value(global_object, import_, type.type()));
                        address = s_abstract_machine.store().allocate({ type.type(), false }, cast_value);
                    } else {
                        // FIXME: https://webassembly.github.io/spec/js-api/#read-the-imports step 5.2
                        //        if v implements Global
                        //            let globaladdr be v.[[Global]]

                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Invalid value for global type");
                    }

                    resolved_imports.set(import_name, Wasm::ExternValue { *address });
                    return {};
                },
                [&](Wasm::MemoryType const&) -> JS::ThrowCompletionOr<void> {
                    if (!import_.is_object() || !is<WebAssemblyMemoryObject>(import_.as_object())) {
                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Expected an instance of WebAssembly.Memory for a memory import");
                    }
                    auto address = static_cast<WebAssemblyMemoryObject const&>(import_.as_object()).address();
                    resolved_imports.set(import_name, Wasm::ExternValue { address });
                    return {};
                },
                [&](Wasm::TableType const&) -> JS::ThrowCompletionOr<void> {
                    if (!import_.is_object() || !is<WebAssemblyTableObject>(import_.as_object())) {
                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Expected an instance of WebAssembly.Table for a table import");
                    }
                    auto address = static_cast<WebAssemblyTableObject const&>(import_.as_object()).address();
                    resolved_imports.set(import_name, Wasm::ExternValue { address });
                    return {};
                },
                [&](const auto&) -> JS::ThrowCompletionOr<void> {
                    // FIXME: Implement these.
                    dbgln("Unimplemented import of non-function attempted");
                    return vm.throw_completion<JS::TypeError>(global_object, "LinkError: Not Implemented");
                }));
        }
    }

    linker.link(resolved_imports);
    auto link_result = linker.finish();
    if (link_result.is_error()) {
        // FIXME: Throw a LinkError.
        StringBuilder builder;
        builder.append("LinkError: Missing ");
        builder.join(' ', link_result.error().missing_imports);
        return vm.throw_completion<JS::TypeError>(global_object, builder.build());
    }

    auto instance_result = s_abstract_machine.instantiate(module, link_result.release_value());
    if (instance_result.is_error()) {
        // FIXME: Throw a LinkError instead.
        return vm.throw_completion<JS::TypeError>(global_object, instance_result.error().error);
    }

    s_instantiated_modules.append(instance_result.release_value());
    s_module_caches.empend();
    return s_instantiated_modules.size() - 1;
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyObject::instantiate)
{
    // FIXME: This shouldn't block!
    auto buffer_or_error = vm.argument(0).to_object(global_object);
    auto promise = JS::Promise::create(global_object);
    bool should_return_module = false;
    if (buffer_or_error.is_error()) {
        auto rejection_value = *buffer_or_error.throw_completion().value();
        promise->reject(rejection_value);
        return promise;
    }
    auto* buffer = buffer_or_error.release_value();

    const Wasm::Module* module { nullptr };
    if (is<JS::ArrayBuffer>(buffer) || is<JS::TypedArrayBase>(buffer)) {
        auto result = parse_module(global_object, buffer);
        if (result.is_error()) {
            promise->reject(*result.release_error().value());
            return promise;
        }
        module = &WebAssemblyObject::s_compiled_modules.at(result.release_value()).module;
        should_return_module = true;
    } else if (is<WebAssemblyModuleObject>(buffer)) {
        module = &static_cast<WebAssemblyModuleObject*>(buffer)->module();
    } else {
        auto error = JS::TypeError::create(global_object, String::formatted("{} is not an ArrayBuffer or a Module", buffer->class_name()));
        promise->reject(error);
        return promise;
    }
    VERIFY(module);

    auto result = instantiate_module(*module, vm, global_object);
    if (result.is_error()) {
        promise->reject(*result.release_error().value());
    } else {
        auto instance_object = vm.heap().allocate<WebAssemblyInstanceObject>(global_object, global_object, result.release_value());
        if (should_return_module) {
            auto object = JS::Object::create(global_object, nullptr);
            object->define_direct_property("module", vm.heap().allocate<WebAssemblyModuleObject>(global_object, global_object, s_compiled_modules.size() - 1), JS::default_attributes);
            object->define_direct_property("instance", instance_object, JS::default_attributes);
            promise->fulfill(object);
        } else {
            promise->fulfill(instance_object);
        }
    }
    return promise;
}

JS::Value to_js_value(JS::GlobalObject& global_object, Wasm::Value& wasm_value)
{
    switch (wasm_value.type().kind()) {
    case Wasm::ValueType::I64:
        return global_object.heap().allocate<JS::BigInt>(global_object, ::Crypto::SignedBigInteger::create_from(wasm_value.to<i64>().value()));
    case Wasm::ValueType::I32:
        return JS::Value(wasm_value.to<i32>().value());
    case Wasm::ValueType::F64:
        return JS::Value(wasm_value.to<double>().value());
    case Wasm::ValueType::F32:
        return JS::Value(static_cast<double>(wasm_value.to<float>().value()));
    case Wasm::ValueType::FunctionReference:
        // FIXME: What's the name of a function reference that isn't exported?
        return create_native_function(global_object, wasm_value.to<Wasm::Reference::Func>().value().address, "FIXME_IHaveNoIdeaWhatThisShouldBeCalled");
    case Wasm::ValueType::NullFunctionReference:
        return JS::js_null();
    case Wasm::ValueType::ExternReference:
    case Wasm::ValueType::NullExternReference:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

JS::ThrowCompletionOr<Wasm::Value> to_webassembly_value(JS::GlobalObject& global_object, JS::Value value, const Wasm::ValueType& type)
{
    static ::Crypto::SignedBigInteger two_64 = "1"_sbigint.shift_left(64);
    auto& vm = global_object.vm();

    switch (type.kind()) {
    case Wasm::ValueType::I64: {
        auto bigint = TRY(value.to_bigint(global_object));
        auto value = bigint->big_integer().divided_by(two_64).remainder;
        VERIFY(value.unsigned_value().trimmed_length() <= 2);
        i64 integer = static_cast<i64>(value.unsigned_value().to_u64());
        if (value.is_negative())
            integer = -integer;
        return Wasm::Value { integer };
    }
    case Wasm::ValueType::I32: {
        auto _i32 = TRY(value.to_i32(global_object));
        return Wasm::Value { static_cast<i32>(_i32) };
    }
    case Wasm::ValueType::F64: {
        auto number = TRY(value.to_double(global_object));
        return Wasm::Value { static_cast<double>(number) };
    }
    case Wasm::ValueType::F32: {
        auto number = TRY(value.to_double(global_object));
        return Wasm::Value { static_cast<float>(number) };
    }
    case Wasm::ValueType::FunctionReference:
    case Wasm::ValueType::NullFunctionReference: {
        if (value.is_null())
            return Wasm::Value { Wasm::ValueType(Wasm::ValueType::NullExternReference), 0ull };

        if (value.is_function()) {
            auto& function = value.as_function();
            for (auto& entry : WebAssemblyObject::s_global_cache.function_instances) {
                if (entry.value == &function)
                    return Wasm::Value { Wasm::Reference { Wasm::Reference::Func { entry.key } } };
            }
        }

        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Exported function");
    }
    case Wasm::ValueType::ExternReference:
    case Wasm::ValueType::NullExternReference:
        TODO();
    }

    VERIFY_NOT_REACHED();
}

JS::NativeFunction* create_native_function(JS::GlobalObject& global_object, Wasm::FunctionAddress address, String const& name)
{
    Optional<Wasm::FunctionType> type;
    WebAssemblyObject::s_abstract_machine.store().get(address)->visit([&](const auto& value) { type = value.type(); });
    if (auto entry = WebAssemblyObject::s_global_cache.function_instances.get(address); entry.has_value())
        return *entry;

    auto function = JS::NativeFunction::create(
        global_object,
        name,
        [address, type = type.release_value()](JS::VM& vm, JS::GlobalObject& global_object) -> JS::ThrowCompletionOr<JS::Value> {
            Vector<Wasm::Value> values;
            values.ensure_capacity(type.parameters().size());

            // Grab as many values as needed and convert them.
            size_t index = 0;
            for (auto& type : type.parameters())
                values.append(TRY(to_webassembly_value(global_object, vm.argument(index++), type)));

            auto result = WebAssemblyObject::s_abstract_machine.invoke(address, move(values));
            // FIXME: Use the convoluted mapping of errors defined in the spec.
            if (result.is_trap())
                return vm.throw_completion<JS::TypeError>(global_object, String::formatted("Wasm execution trapped (WIP): {}", result.trap().reason));

            if (result.values().is_empty())
                return JS::js_undefined();

            if (result.values().size() == 1)
                return to_js_value(global_object, result.values().first());

            Vector<JS::Value> result_values;
            for (auto& entry : result.values())
                result_values.append(to_js_value(global_object, entry));

            return JS::Value(JS::Array::create_from(global_object, result_values));
        });

    WebAssemblyObject::s_global_cache.function_instances.set(address, function);
    return function;
}

WebAssemblyMemoryObject::WebAssemblyMemoryObject(JS::GlobalObject& global_object, Wasm::MemoryAddress address)
    : Object(static_cast<WindowObject&>(global_object).ensure_web_prototype<WebAssemblyMemoryPrototype>("WebAssemblyMemoryPrototype"))
    , m_address(address)
{
}

}
