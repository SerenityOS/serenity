/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ProxyConstructor.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

ProxyConstructor::ProxyConstructor(GlobalObject& global_object)
    : NativeFunction("Proxy", *global_object.function_prototype())
{
}

void ProxyConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.proxy_prototype(), 0);
    define_property("length", Value(2), Attribute::Configurable);
}

ProxyConstructor::~ProxyConstructor()
{
}

Value ProxyConstructor::call(Interpreter& interpreter)
{
    interpreter.throw_exception<TypeError>(ErrorType::ProxyCallWithNew);
    return {};
}

Value ProxyConstructor::construct(Interpreter& interpreter, Function&)
{
    if (interpreter.argument_count() < 2) {
        interpreter.throw_exception<TypeError>(ErrorType::ProxyTwoArguments);
        return {};
    }

    auto target = interpreter.argument(0);
    auto handler = interpreter.argument(1);

    if (!target.is_object()) {
        interpreter.throw_exception<TypeError>(ErrorType::ProxyConstructorBadType, "target", target.to_string_without_side_effects().characters());
        return {};
    }
    if (!handler.is_object()) {
        interpreter.throw_exception<TypeError>(ErrorType::ProxyConstructorBadType, "handler", handler.to_string_without_side_effects().characters());
        return {};
    }
    return ProxyObject::create(global_object(), target.as_object(), handler.as_object());
}

}
