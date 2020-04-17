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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorConstructor.h>

namespace JS {

ErrorConstructor::ErrorConstructor()
    : NativeFunction("Error")
{
    put("prototype", interpreter().error_prototype());
    put("length", Value(1));
}

ErrorConstructor::~ErrorConstructor()
{
}

Value ErrorConstructor::call(Interpreter& interpreter)
{
    return construct(interpreter);
}

Value ErrorConstructor::construct(Interpreter& interpreter)
{
    String message = "";
    if (!interpreter.call_frame().arguments.is_empty() && !interpreter.call_frame().arguments[0].is_undefined())
        message = interpreter.call_frame().arguments[0].to_string();
    return Error::create(interpreter.global_object(), "Error", message);
}

#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName)                                        \
    ConstructorName::ConstructorName()                                                                               \
    {                                                                                                                \
        put("prototype", interpreter().snake_name##_prototype());                                                    \
        put("length", Value(1));                                                                                     \
    }                                                                                                                \
    ConstructorName::~ConstructorName() {}                                                                           \
    Value ConstructorName::call(Interpreter& interpreter)                                                            \
    {                                                                                                                \
        return construct(interpreter);                                                                               \
    }                                                                                                                \
    Value ConstructorName::construct(Interpreter& interpreter)                                                       \
    {                                                                                                                \
        String message = "";                                                                                         \
        if (!interpreter.call_frame().arguments.is_empty() && !interpreter.call_frame().arguments[0].is_undefined()) \
            message = interpreter.call_frame().arguments[0].to_string();                                             \
        return ClassName::create(interpreter.global_object(), message);                                              \
    }

JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE

}
