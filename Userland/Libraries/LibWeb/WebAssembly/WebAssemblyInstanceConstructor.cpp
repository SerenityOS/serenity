/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObject.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyInstanceConstructor::WebAssemblyInstanceConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

WebAssemblyInstanceConstructor::~WebAssemblyInstanceConstructor()
{
}

JS::ThrowCompletionOr<JS::Value> WebAssemblyInstanceConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Instance");
}

JS::ThrowCompletionOr<JS::Object*> WebAssemblyInstanceConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();
    auto* module_argument = TRY(vm.argument(0).to_object(global_object));
    if (!is<WebAssemblyModuleObject>(module_argument))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "WebAssembly.Module");
    auto& module_object = static_cast<WebAssemblyModuleObject&>(*module_argument);
    auto result = TRY(WebAssemblyObject::instantiate_module(module_object.module(), vm, global_object));
    return heap().allocate<WebAssemblyInstanceObject>(global_object, global_object, result);
}

void WebAssemblyInstanceConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
