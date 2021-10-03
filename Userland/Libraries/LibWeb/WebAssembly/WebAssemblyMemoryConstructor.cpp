/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyMemoryConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyMemoryPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyMemoryConstructor::WebAssemblyMemoryConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

WebAssemblyMemoryConstructor::~WebAssemblyMemoryConstructor()
{
}

JS::Value WebAssemblyMemoryConstructor::call()
{
    vm().throw_exception<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Memory");
    return {};
}

JS::Value WebAssemblyMemoryConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto descriptor = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    auto initial_value = TRY_OR_DISCARD(descriptor->get("initial"));
    auto maximum_value = TRY_OR_DISCARD(descriptor->get("maximum"));

    if (initial_value.is_empty()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Number");
        return {};
    }

    auto initial = initial_value.to_u32(global_object);
    if (vm.exception())
        return {};

    Optional<u32> maximum;

    if (!maximum_value.is_empty()) {
        maximum = maximum_value.to_u32(global_object);
        if (vm.exception())
            return {};
    }

    auto address = WebAssemblyObject::s_abstract_machine.store().allocate(Wasm::MemoryType { Wasm::Limits { initial, maximum } });
    if (!address.has_value()) {
        vm.throw_exception<JS::TypeError>(global_object, "Wasm Memory allocation failed");
        return {};
    }

    return vm.heap().allocate<WebAssemblyMemoryObject>(global_object, global_object, *address);
}

void WebAssemblyMemoryConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyMemoryPrototype>("WebAssemblyMemoryPrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
