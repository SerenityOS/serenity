/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ErrorConstructor::ErrorConstructor(GlobalObject& global_object)
    : NativeFunction("Error", *global_object.function_prototype())
{
}

void ErrorConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.error_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);
}

ErrorConstructor::~ErrorConstructor()
{
}

Value ErrorConstructor::call()
{
    return construct(*this);
}

Value ErrorConstructor::construct(Function&)
{
    auto& vm = this->vm();
    String message = "";
    if (!vm.call_frame().arguments.is_empty() && !vm.call_frame().arguments[0].is_undefined()) {
        message = vm.call_frame().arguments[0].to_string(global_object());
        if (vm.exception())
            return {};
    }
    return Error::create(global_object(), "Error", message);
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                            \
    ConstructorName::ConstructorName(GlobalObject& global_object)                                        \
        : NativeFunction(*global_object.function_prototype())                                            \
    {                                                                                                    \
    }                                                                                                    \
    void ConstructorName::initialize(GlobalObject& global_object)                                        \
    {                                                                                                    \
        NativeFunction::initialize(global_object);                                                       \
        define_property("prototype", global_object.snake_name##_prototype(), 0);                         \
        define_property("length", Value(1), Attribute::Configurable);                                    \
    }                                                                                                    \
    ConstructorName::~ConstructorName() { }                                                              \
    Value ConstructorName::call()                                                                        \
    {                                                                                                    \
        return construct(*this);                                                                         \
    }                                                                                                    \
    Value ConstructorName::construct(Function&)                                                          \
    {                                                                                                    \
        String message = "";                                                                             \
        if (!vm().call_frame().arguments.is_empty() && !vm().call_frame().arguments[0].is_undefined()) { \
            message = vm().call_frame().arguments[0].to_string(global_object());                         \
            if (vm().exception())                                                                        \
                return {};                                                                               \
        }                                                                                                \
        return ClassName::create(global_object(), message);                                              \
    }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
