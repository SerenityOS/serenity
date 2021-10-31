/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>
#include <LibWeb/WebAssembly/WebAssemblyModulePrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyModuleConstructor::WebAssemblyModuleConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

WebAssemblyModuleConstructor::~WebAssemblyModuleConstructor()
{
}

JS::ThrowCompletionOr<JS::Value> WebAssemblyModuleConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Module");
}

JS::ThrowCompletionOr<JS::Object*> WebAssemblyModuleConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* buffer_object = TRY(vm.argument(0).to_object(global_object));
    auto result = TRY(parse_module(global_object, buffer_object));

    return heap().allocate<WebAssemblyModuleObject>(global_object, global_object, result);
}

void WebAssemblyModuleConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyModulePrototype>("WebAssemblyModulePrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
