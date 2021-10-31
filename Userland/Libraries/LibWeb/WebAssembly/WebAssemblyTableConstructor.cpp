/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>
#include <LibWeb/WebAssembly/WebAssemblyTableConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyTableObject.h>
#include <LibWeb/WebAssembly/WebAssemblyTablePrototype.h>

namespace Web::Bindings {

WebAssemblyTableConstructor::WebAssemblyTableConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

WebAssemblyTableConstructor::~WebAssemblyTableConstructor()
{
}

JS::ThrowCompletionOr<JS::Value> WebAssemblyTableConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Table");
}

JS::ThrowCompletionOr<JS::Object*> WebAssemblyTableConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto descriptor = TRY(vm.argument(0).to_object(global_object));
    auto element_value = TRY(descriptor->get("element"));
    if (!element_value.is_string())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::InvalidHint, element_value.to_string_without_side_effects());
    auto& element = element_value.as_string().string();

    Optional<Wasm::ValueType> reference_type;
    if (element == "anyfunc"sv)
        reference_type = Wasm::ValueType(Wasm::ValueType::FunctionReference);
    else if (element == "externref"sv)
        reference_type = Wasm::ValueType(Wasm::ValueType::ExternReference);

    if (!reference_type.has_value())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::InvalidHint, element);

    auto initial_value = TRY(descriptor->get("initial"));
    auto maximum_value = TRY(descriptor->get("maximum"));

    auto initial = TRY(initial_value.to_u32(global_object));

    Optional<u32> maximum;

    if (!maximum_value.is_undefined())
        maximum = TRY(maximum_value.to_u32(global_object));

    if (maximum.has_value() && maximum.value() < initial)
        return vm.throw_completion<JS::RangeError>(global_object, "maximum should be larger than or equal to initial");

    auto value_value = TRY(descriptor->get("value"));
    auto reference_value = TRY([&]() -> JS::ThrowCompletionOr<Wasm::Value> {
        if (value_value.is_undefined())
            return Wasm::Value(*reference_type, 0ull);

        return to_webassembly_value(global_object, value_value, *reference_type);
    }());

    auto& reference = reference_value.value().get<Wasm::Reference>();

    auto address = WebAssemblyObject::s_abstract_machine.store().allocate(Wasm::TableType { *reference_type, Wasm::Limits { initial, maximum } });
    if (!address.has_value())
        return vm.throw_completion<JS::TypeError>(global_object, "Wasm Table allocation failed");

    auto& table = *WebAssemblyObject::s_abstract_machine.store().get(*address);
    for (auto& element : table.elements())
        element = reference;

    return vm.heap().allocate<WebAssemblyTableObject>(global_object, global_object, *address);
}

void WebAssemblyTableConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyTablePrototype>("WebAssemblyTablePrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
