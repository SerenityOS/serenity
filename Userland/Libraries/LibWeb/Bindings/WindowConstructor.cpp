/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/WindowConstructor.h>
#include <LibWeb/Bindings/WindowPrototype.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

WindowConstructor::WindowConstructor(JS::Realm& realm)
    : NativeFunction(*realm.intrinsics().function_prototype())
{
}

WindowConstructor::~WindowConstructor() = default;

JS::ThrowCompletionOr<JS::Value> WindowConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::ConstructorWithoutNew, "Window");
}

JS::ThrowCompletionOr<JS::Object*> WindowConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<JS::TypeError>(JS::ErrorType::NotAConstructor, "Window");
}

void WindowConstructor::initialize(JS::Realm& realm)
{
    auto& vm = this->vm();
    auto& window = verify_cast<HTML::Window>(realm.global_object());

    NativeFunction::initialize(realm);
    define_direct_property(vm.names.prototype, &window.cached_web_prototype("Window"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

}
