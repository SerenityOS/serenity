/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/LocationConstructor.h>
#include <LibWeb/Bindings/LocationPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>

namespace Web::Bindings {

LocationConstructor::LocationConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

LocationConstructor::~LocationConstructor() = default;

JS::ThrowCompletionOr<JS::Value> LocationConstructor::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Location");
}

JS::ThrowCompletionOr<JS::Object*> LocationConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "Location");
}

void LocationConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<LocationPrototype>("Location"), 0);
    define_direct_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

}
