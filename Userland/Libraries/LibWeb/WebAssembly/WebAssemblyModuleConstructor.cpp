/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleConstructor.h>
#include <LibWeb/WebAssembly/WebAssemblyModuleObject.h>
#include <LibWeb/WebAssembly/WebAssemblyModulePrototype.h>
#include <LibWeb/WebAssembly/WebAssemblyObject.h>

namespace Web::Bindings {

WebAssemblyModuleConstructor::WebAssemblyModuleConstructor(JS::Realm& realm)
    : NativeFunction(*realm.intrinsics().function_prototype())
{
}

WebAssemblyModuleConstructor::~WebAssemblyModuleConstructor() = default;

JS::ThrowCompletionOr<JS::Value> WebAssemblyModuleConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "WebAssembly.Module");
}

JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Object>> WebAssemblyModuleConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    auto* buffer_object = TRY(vm.argument(0).to_object(vm));
    auto result = TRY(parse_module(vm, buffer_object));

    return MUST_OR_THROW_OOM(heap().allocate<WebAssemblyModuleObject>(realm, realm, result));
}

JS::ThrowCompletionOr<void> WebAssemblyModuleConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();

    MUST_OR_THROW_OOM(NativeFunction::initialize(realm));
    define_direct_property(vm.names.prototype, &Bindings::ensure_web_prototype<WebAssemblyModulePrototype>(realm, "WebAssembly.Module"), 0);
    define_direct_property(vm.names.length, JS::Value(1), JS::Attribute::Configurable);

    return {};
}

}
