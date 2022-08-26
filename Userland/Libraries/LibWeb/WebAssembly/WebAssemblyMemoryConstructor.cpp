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

WebAssemblyMemoryConstructor::WebAssemblyMemoryConstructor(JS::Realm& realm)
    : NativeFunction(*realm.intrinsics().function_prototype())
{
}

WebAssemblyMemoryConstructor::~WebAssemblyMemoryConstructor() = default;

JS::ThrowCompletionOr<JS::Value> WebAssemblyMemoryConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Memory");
}

JS::ThrowCompletionOr<JS::Object*> WebAssemblyMemoryConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    auto descriptor = TRY(vm.argument(0).to_object(vm));
    auto initial_value = TRY(descriptor->get("initial"));
    auto maximum_value = TRY(descriptor->get("maximum"));

    if (!initial_value.is_number())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "Number");

    u32 initial = TRY(initial_value.to_u32(vm));

    Optional<u32> maximum;

    if (!maximum_value.is_undefined())
        maximum = TRY(maximum_value.to_u32(vm));

    auto address = WebAssemblyObject::s_abstract_machine.store().allocate(Wasm::MemoryType { Wasm::Limits { initial, maximum } });
    if (!address.has_value())
        return vm.throw_completion<JS::TypeError>("Wasm Memory allocation failed");

    return vm.heap().allocate<WebAssemblyMemoryObject>(realm, realm, *address);
}

void WebAssemblyMemoryConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(realm.global_object());

    NativeFunction::initialize(realm);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyMemoryPrototype>("WebAssemblyMemoryPrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
