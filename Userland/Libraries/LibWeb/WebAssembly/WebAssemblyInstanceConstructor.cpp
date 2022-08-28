/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObject.h>
#include <LibWeb/WebAssembly/WebAssemblyInstanceObjectPrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyInstanceConstructor::WebAssemblyInstanceConstructor(JS::Realm& realm)
    : NativeFunction(*realm.intrinsics().function_prototype())
{
}

WebAssemblyInstanceConstructor::~WebAssemblyInstanceConstructor() = default;

JS::ThrowCompletionOr<JS::Value> WebAssemblyInstanceConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Instance");
}

JS::ThrowCompletionOr<JS::Object*> WebAssemblyInstanceConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    auto* module_argument = TRY(vm.argument(0).to_object(vm));
    if (!is<WebAssemblyModuleObject>(module_argument))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WebAssembly.Module");
    auto& module_object = static_cast<WebAssemblyModuleObject&>(*module_argument);
    auto result = TRY(WebAssemblyObject::instantiate_module(vm, module_object.module()));
    return heap().allocate<WebAssemblyInstanceObject>(realm, realm, result);
}

void WebAssemblyInstanceConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = verify_cast<HTML::Window>(realm.global_object());

    NativeFunction::initialize(realm);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WebAssemblyInstancePrototype>("WebAssemblyInstancePrototype"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);
}

}
