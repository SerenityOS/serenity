/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
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

JS::Value WebAssemblyInstanceConstructor::call()
{
    vm().throw_exception<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "WebAssemblyInstance");
    return {};
}

JS::Value WebAssemblyInstanceConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto module_argument = vm.argument(0).to_object(global_object);
    if (vm.exception())
        return {};

    if (!is<WebAssemblyModuleObject>(module_argument)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "WebAssembly.Module");
        return {};
    }

    auto& module_object = static_cast<WebAssemblyModuleObject&>(*module_argument);
    auto result = WebAssemblyObject::instantiate_module(module_object.module(), vm, global_object);
    if (result.is_error()) {
        vm.throw_exception(global_object, result.release_error());
        return {};
    }
    return heap().allocate<WebAssemblyInstanceObject>(global_object, global_object, result.value());
}

void WebAssemblyInstanceConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype"));
    define_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
