/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ErrorConstructor::ErrorConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Error, *global_object.function_prototype())
{
}

void ErrorConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.error_prototype(), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);
}

Value ErrorConstructor::call()
{
    return construct(*this);
}

Value ErrorConstructor::construct(Function&)
{
    auto& vm = this->vm();
    // FIXME: Use OrdinaryCreateFromConstructor(newTarget, "%Error.prototype%")
    auto* error = Error::create(global_object());

    u8 attr = Attribute::Writable | Attribute::Configurable;

    if (!vm.argument(0).is_undefined()) {
        auto message = vm.argument(0).to_string(global_object());
        if (vm.exception())
            return {};
        error->define_property(vm.names.message, js_string(vm, message), attr);
    }

    error->install_error_cause(vm.argument(1));
    if (vm.exception())
        return {};

    return error;
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName::ConstructorName(GlobalObject& global_object)                        \
        : NativeFunction(*static_cast<Object*>(global_object.error_constructor()))       \
    {                                                                                    \
    }                                                                                    \
                                                                                         \
    void ConstructorName::initialize(GlobalObject& global_object)                        \
    {                                                                                    \
        auto& vm = this->vm();                                                           \
        NativeFunction::initialize(global_object);                                       \
        define_property(vm.names.prototype, global_object.snake_name##_prototype(), 0);  \
        define_property(vm.names.length, Value(1), Attribute::Configurable);             \
    }                                                                                    \
                                                                                         \
    ConstructorName::~ConstructorName() { }                                              \
                                                                                         \
    Value ConstructorName::call()                                                        \
    {                                                                                    \
        return construct(*this);                                                         \
    }                                                                                    \
                                                                                         \
    Value ConstructorName::construct(Function&)                                          \
    {                                                                                    \
        auto& vm = this->vm();                                                           \
        /* FIXME: Use OrdinaryCreateFromConstructor(                                     \
         * FIXME:     newTarget, "%NativeError.prototype%"). */                          \
        auto* error = ClassName::create(global_object());                                \
                                                                                         \
        u8 attr = Attribute::Writable | Attribute::Configurable;                         \
                                                                                         \
        if (!vm.argument(0).is_undefined()) {                                            \
            auto message = vm.argument(0).to_string(global_object());                    \
            if (vm.exception())                                                          \
                return {};                                                               \
            error->define_property(vm.names.message, js_string(vm, message), attr);      \
        }                                                                                \
                                                                                         \
        error->install_error_cause(vm.argument(1));                                      \
        if (vm.exception())                                                              \
            return {};                                                                   \
                                                                                         \
        return error;                                                                    \
    }

JS_ENUMERATE_NATIVE_ERRORS
#undef __JS_ENUMERATE

}
