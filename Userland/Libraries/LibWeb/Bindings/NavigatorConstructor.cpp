/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/NavigatorConstructor.h>
#include <LibWeb/Bindings/NavigatorPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>

namespace Web::Bindings {

NavigatorConstructor::NavigatorConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

NavigatorConstructor::~NavigatorConstructor() = default;

JS::ThrowCompletionOr<JS::Value> NavigatorConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Navigator");
}

JS::ThrowCompletionOr<JS::Object*> NavigatorConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "Navigator");
}

void NavigatorConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<NavigatorPrototype>("Navigator"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

}
