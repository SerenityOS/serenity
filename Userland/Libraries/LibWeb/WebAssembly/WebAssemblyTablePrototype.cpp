/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/WebAssembly/WebAssemblyTableObject.h>
#include <LibWeb/WebAssembly/WebAssemblyTablePrototype.h>

namespace Web::Bindings {

JS::ThrowCompletionOr<void> WebAssemblyTablePrototype::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Object::initialize(realm));
    define_native_accessor(realm, "length", length_getter, {}, JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_function(realm, "grow", grow, 1, JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_function(realm, "get", get, 1, JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable);
    define_native_function(realm, "set", set, 1, JS::Attribute::Writable | JS::Attribute::Enumerable | JS::Attribute::Configurable);

    return {};
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyTablePrototype::grow)
{
    auto delta = TRY(vm.argument(0).to_u32(vm));

    auto* this_object = TRY(vm.this_value().to_object(vm));
    if (!is<WebAssemblyTableObject>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Table");
    auto* table_object = static_cast<WebAssemblyTableObject*>(this_object);
    auto address = table_object->address();
    auto* table = WebAssemblyObject::s_abstract_machine.store().get(address);
    if (!table)
        return JS::js_undefined();

    auto initial_size = table->elements().size();
    auto value_value = vm.argument(1);
    auto reference_value = TRY([&]() -> JS::ThrowCompletionOr<Wasm::Value> {
        if (value_value.is_undefined())
            return Wasm::Value(table->type().element_type(), 0ull);

        return to_webassembly_value(vm, value_value, table->type().element_type());
    }());

    auto& reference = reference_value.value().get<Wasm::Reference>();

    if (!table->grow(delta, reference))
        return vm.throw_completion<JS::RangeError>("Failed to grow table");

    return JS::Value(static_cast<u32>(initial_size));
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyTablePrototype::get)
{
    auto index = TRY(vm.argument(0).to_u32(vm));

    auto* this_object = TRY(vm.this_value().to_object(vm));
    if (!is<WebAssemblyTableObject>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Table");
    auto* table_object = static_cast<WebAssemblyTableObject*>(this_object);
    auto address = table_object->address();
    auto* table = WebAssemblyObject::s_abstract_machine.store().get(address);
    if (!table)
        return JS::js_undefined();

    if (table->elements().size() <= index)
        return vm.throw_completion<JS::RangeError>("Table element index out of range");

    auto& ref = table->elements()[index];
    if (!ref.has_value())
        return JS::js_undefined();

    Wasm::Value wasm_value { ref.value() };
    return to_js_value(vm, wasm_value);
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyTablePrototype::set)
{
    auto index = TRY(vm.argument(0).to_u32(vm));

    auto* this_object = TRY(vm.this_value().to_object(vm));
    if (!is<WebAssemblyTableObject>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Table");
    auto* table_object = static_cast<WebAssemblyTableObject*>(this_object);
    auto address = table_object->address();
    auto* table = WebAssemblyObject::s_abstract_machine.store().get(address);
    if (!table)
        return JS::js_undefined();

    if (table->elements().size() <= index)
        return vm.throw_completion<JS::RangeError>("Table element index out of range");

    auto value_value = vm.argument(1);
    auto reference_value = TRY([&]() -> JS::ThrowCompletionOr<Wasm::Value> {
        if (value_value.is_undefined())
            return Wasm::Value(table->type().element_type(), 0ull);

        return to_webassembly_value(vm, value_value, table->type().element_type());
    }());

    auto& reference = reference_value.value().get<Wasm::Reference>();
    table->elements()[index] = reference;

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(WebAssemblyTablePrototype::length_getter)
{
    auto* this_object = TRY(vm.this_value().to_object(vm));
    if (!is<WebAssemblyTableObject>(this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Table");
    auto* table_object = static_cast<WebAssemblyTableObject*>(this_object);
    auto address = table_object->address();
    auto* table = WebAssemblyObject::s_abstract_machine.store().get(address);
    if (!table)
        return JS::js_undefined();

    return JS::Value(table->elements().size());
}

}
