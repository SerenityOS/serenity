/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/WindowConstructor.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/WindowPrototype.h>

namespace Web::Bindings {

WindowConstructor::WindowConstructor(JS::Realm& realm)
    : NativeFunction(*realm.global_object().function_prototype())
{
}

WindowConstructor::~WindowConstructor() = default;

JS::ThrowCompletionOr<JS::Value> WindowConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Window");
}

JS::ThrowCompletionOr<JS::Object*> WindowConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "Window");
}

void WindowConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<WindowPrototype>("Window"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

}
