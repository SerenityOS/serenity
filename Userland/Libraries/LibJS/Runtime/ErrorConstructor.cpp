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
    String message;
    if (!vm.argument(0).is_undefined()) {
        message = vm.argument(0).to_string(global_object());
        if (vm.exception())
            return {};
    }
    return Error::create(global_object(), message);
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    ConstructorName::ConstructorName(GlobalObject& global_object)                        \
        : NativeFunction(*global_object.function_prototype())                            \
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
        String message = "";                                                             \
        if (!vm.argument(0).is_undefined()) {                                            \
            message = vm.argument(0).to_string(global_object());                         \
            if (vm.exception())                                                          \
                return {};                                                               \
        }                                                                                \
        return ClassName::create(global_object(), message);                              \
    }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
