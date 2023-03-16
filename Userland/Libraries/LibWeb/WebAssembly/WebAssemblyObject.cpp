/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/ScopeGuard.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/ThrowableStringBuilder.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/AbstractMachine/Validator.h>
#include <LibWeb/Bindings/InstancePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MemoryPrototype.h>
#include <LibWeb/Bindings/ModulePrototype.h>
#include <LibWeb/Bindings/TablePrototype.h>
#include <LibWeb/WebAssembly/Instance.h>
#include <LibWeb/WebAssembly/Memory.h>
#include <LibWeb/WebAssembly/Module.h>
#include <LibWeb/WebAssembly/Table.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

Vector<NonnullOwnPtr<WebAssemblyObject::CompiledWebAssemblyModule>> WebAssemblyObject::s_compiled_modules;
Vector<NonnullOwnPtr<Wasm::ModuleInstance>> WebAssemblyObject::s_instantiated_modules;
Vector<WebAssemblyObject::ModuleCache> WebAssemblyObject::s_module_caches;
WebAssemblyObject::GlobalModuleCache WebAssemblyObject::s_global_cache;
Wasm::AbstractMachine WebAssemblyObject::s_abstract_machine;

JS::ThrowCompletionOr<size_t> parse_module(JS::VM& vm, JS::Object* buffer_object)
{
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
        return vm.throw_completion<JS::TypeError>("Not a BufferSource"sv);
    }
    FixedMemoryStream stream { data };
    auto module_result = Wasm::Module::parse(stream);
    if (module_result.is_error()) {
        // FIXME: Throw CompileError instead.
        return vm.throw_completion<JS::TypeError>(Wasm::parse_error_to_deprecated_string(module_result.error()));
    }

    if (auto validation_result = WebAssemblyObject::s_abstract_machine.validate(module_result.value()); validation_result.is_error()) {
        // FIXME: Throw CompileError instead.
        return vm.throw_completion<JS::TypeError>(validation_result.error().error_string);
    }

    WebAssemblyObject::s_compiled_modules.append(make<WebAssemblyObject::CompiledWebAssemblyModule>(module_result.release_value()));
    return WebAssemblyObject::s_compiled_modules.size() - 1;
}

JS::ThrowCompletionOr<size_t> WebAssemblyObject::instantiate_module(JS::VM& vm, Wasm::Module const& module)
{
    Wasm::Linker linker { module };
    HashMap<Wasm::Linker::Name, Wasm::ExternValue> resolved_imports;
    auto import_argument = vm.argument(1);
    if (!import_argument.is_undefined()) {
        auto* import_object = TRY(import_argument.to_object(vm));
        dbgln("Trying to resolve stuff because import object was specified");
        for (Wasm::Linker::Name const& import_name : linker.unresolved_imports()) {
            dbgln("Trying to resolve {}::{}", import_name.module, import_name.name);
            auto value_or_error = import_object->get(import_name.module);
            if (value_or_error.is_error())
                break;
            auto value = value_or_error.release_value();
            auto object_or_error = value.to_object(vm);
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
                                argument_values.append(to_js_value(vm, entry));

                            auto result = TRY(JS::call(vm, function, JS::js_undefined(), move(argument_values)));
                            if (type.results().is_empty())
                                return Wasm::Result { Vector<Wasm::Value> {} };

                            if (type.results().size() == 1)
                                return Wasm::Result { Vector<Wasm::Value> { TRY(to_webassembly_value(vm, result, type.results().first())) } };

                            auto method = TRY(result.get_method(vm, vm.names.iterator));
                            if (method == JS::js_undefined())
                                return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotIterable, TRY_OR_THROW_OOM(vm, result.to_string_without_side_effects()));

                            auto values = TRY(JS::iterable_to_list(vm, result, method));

                            if (values.size() != type.results().size())
                                return vm.throw_completion<JS::TypeError>(DeprecatedString::formatted("Invalid number of return values for multi-value wasm return of {} objects", type.results().size()));

                            Vector<Wasm::Value> wasm_values;
                            TRY_OR_THROW_OOM(vm, wasm_values.try_ensure_capacity(values.size()));

                            size_t i = 0;
                            for (auto& value : values)
                                wasm_values.append(TRY(to_webassembly_value(vm, value, type.results()[i++])));

                            return Wasm::Result { move(wasm_values) };
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
                            return vm.throw_completion<JS::TypeError>("LinkError: Import resolution attempted to cast a Number to a BigInteger"sv);
                        }
                        if (import_.is_bigint() && type.type().kind() != Wasm::ValueType::I64) {
                            // FIXME: Throw a LinkError instead.
                            return vm.throw_completion<JS::TypeError>("LinkError: Import resolution attempted to cast a BigInteger to a Number"sv);
                        }
                        auto cast_value = TRY(to_webassembly_value(vm, import_, type.type()));
                        address = s_abstract_machine.store().allocate({ type.type(), false }, cast_value);
                    } else {
                        // FIXME: https://webassembly.github.io/spec/js-api/#read-the-imports step 5.2
                        //        if v implements Global
                        //            let globaladdr be v.[[Global]]

                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>("LinkError: Invalid value for global type"sv);
                    }

                    resolved_imports.set(import_name, Wasm::ExternValue { *address });
                    return {};
                },
                [&](Wasm::MemoryType const&) -> JS::ThrowCompletionOr<void> {
                    if (!import_.is_object() || !is<WebAssembly::Memory>(import_.as_object())) {
                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>("LinkError: Expected an instance of WebAssembly.Memory for a memory import"sv);
                    }
                    auto address = static_cast<WebAssembly::Memory const&>(import_.as_object()).address();
                    resolved_imports.set(import_name, Wasm::ExternValue { address });
                    return {};
                },
                [&](Wasm::TableType const&) -> JS::ThrowCompletionOr<void> {
                    if (!import_.is_object() || !is<WebAssembly::Table>(import_.as_object())) {
                        // FIXME: Throw a LinkError instead
                        return vm.throw_completion<JS::TypeError>("LinkError: Expected an instance of WebAssembly.Table for a table import"sv);
                    }
                    auto address = static_cast<WebAssembly::Table const&>(import_.as_object()).address();
                    resolved_imports.set(import_name, Wasm::ExternValue { address });
                    return {};
                },
                [&](auto const&) -> JS::ThrowCompletionOr<void> {
                    // FIXME: Implement these.
                    dbgln("Unimplemented import of non-function attempted");
                    return vm.throw_completion<JS::TypeError>("LinkError: Not Implemented"sv);
                }));
        }
    }

    linker.link(resolved_imports);
    auto link_result = linker.finish();
    if (link_result.is_error()) {
        // FIXME: Throw a LinkError.
        JS::ThrowableStringBuilder builder(vm);
        MUST_OR_THROW_OOM(builder.append("LinkError: Missing "sv));
        MUST_OR_THROW_OOM(builder.join(' ', link_result.error().missing_imports));
        return vm.throw_completion<JS::TypeError>(MUST_OR_THROW_OOM(builder.to_string()));
    }

    auto instance_result = s_abstract_machine.instantiate(module, link_result.release_value());
    if (instance_result.is_error()) {
        // FIXME: Throw a LinkError instead.
        return vm.throw_completion<JS::TypeError>(instance_result.error().error);
    }

    s_instantiated_modules.append(instance_result.release_value());
    s_module_caches.empend();
    return s_instantiated_modules.size() - 1;
}

JS::Value to_js_value(JS::VM& vm, Wasm::Value& wasm_value)
{
    auto& realm = *vm.current_realm();
    switch (wasm_value.type().kind()) {
    case Wasm::ValueType::I64:
        return realm.heap().allocate<JS::BigInt>(realm, ::Crypto::SignedBigInteger { wasm_value.to<i64>().value() }).release_allocated_value_but_fixme_should_propagate_errors();
    case Wasm::ValueType::I32:
        return JS::Value(wasm_value.to<i32>().value());
    case Wasm::ValueType::F64:
        return JS::Value(wasm_value.to<double>().value());
    case Wasm::ValueType::F32:
        return JS::Value(static_cast<double>(wasm_value.to<float>().value()));
    case Wasm::ValueType::FunctionReference:
        // FIXME: What's the name of a function reference that isn't exported?
        return create_native_function(vm, wasm_value.to<Wasm::Reference::Func>().value().address, "FIXME_IHaveNoIdeaWhatThisShouldBeCalled");
    case Wasm::ValueType::NullFunctionReference:
        return JS::js_null();
    case Wasm::ValueType::ExternReference:
    case Wasm::ValueType::NullExternReference:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

JS::ThrowCompletionOr<Wasm::Value> to_webassembly_value(JS::VM& vm, JS::Value value, Wasm::ValueType const& type)
{
    static ::Crypto::SignedBigInteger two_64 = "1"_sbigint.shift_left(64);

    switch (type.kind()) {
    case Wasm::ValueType::I64: {
        auto bigint = TRY(value.to_bigint(vm));
        auto value = bigint->big_integer().divided_by(two_64).remainder;
        VERIFY(value.unsigned_value().trimmed_length() <= 2);
        i64 integer = static_cast<i64>(value.unsigned_value().to_u64());
        if (value.is_negative())
            integer = -integer;
        return Wasm::Value { integer };
    }
    case Wasm::ValueType::I32: {
        auto _i32 = TRY(value.to_i32(vm));
        return Wasm::Value { static_cast<i32>(_i32) };
    }
    case Wasm::ValueType::F64: {
        auto number = TRY(value.to_double(vm));
        return Wasm::Value { static_cast<double>(number) };
    }
    case Wasm::ValueType::F32: {
        auto number = TRY(value.to_double(vm));
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

        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Exported function");
    }
    case Wasm::ValueType::ExternReference:
    case Wasm::ValueType::NullExternReference:
        TODO();
    }

    VERIFY_NOT_REACHED();
}

JS::NativeFunction* create_native_function(JS::VM& vm, Wasm::FunctionAddress address, DeprecatedString const& name)
{
    auto& realm = *vm.current_realm();
    Optional<Wasm::FunctionType> type;
    WebAssemblyObject::s_abstract_machine.store().get(address)->visit([&](auto const& value) { type = value.type(); });
    if (auto entry = WebAssemblyObject::s_global_cache.function_instances.get(address); entry.has_value())
        return *entry;

    auto function = JS::NativeFunction::create(
        realm,
        name,
        [address, type = type.release_value()](JS::VM& vm) -> JS::ThrowCompletionOr<JS::Value> {
            auto& realm = *vm.current_realm();
            Vector<Wasm::Value> values;
            values.ensure_capacity(type.parameters().size());

            // Grab as many values as needed and convert them.
            size_t index = 0;
            for (auto& type : type.parameters())
                values.append(TRY(to_webassembly_value(vm, vm.argument(index++), type)));

            auto result = WebAssemblyObject::s_abstract_machine.invoke(address, move(values));
            // FIXME: Use the convoluted mapping of errors defined in the spec.
            if (result.is_trap())
                return vm.throw_completion<JS::TypeError>(TRY_OR_THROW_OOM(vm, String::formatted("Wasm execution trapped (WIP): {}", result.trap().reason)));

            if (result.values().is_empty())
                return JS::js_undefined();

            if (result.values().size() == 1)
                return to_js_value(vm, result.values().first());

            return JS::Value(JS::Array::create_from<Wasm::Value>(realm, result.values(), [&](Wasm::Value value) {
                return to_js_value(vm, value);
            }));
        });

    WebAssemblyObject::s_global_cache.function_instances.set(address, function);
    return function;
}

}
