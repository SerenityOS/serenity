/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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

#include <AK/Function.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ScriptFunction::ScriptFunction(const ScopeNode& body, Vector<FlyString> parameters)
    : m_body(body)
    , m_parameters(move(parameters))
{
    put("prototype", heap().allocate<Object>());
    put_native_property("length", length_getter, length_setter);
}

ScriptFunction::~ScriptFunction()
{
}

Value ScriptFunction::call(Interpreter& interpreter)
{
    auto& argument_values = interpreter.call_frame().arguments;
    Vector<Argument> arguments;
    for (size_t i = 0; i < m_parameters.size(); ++i) {
        auto name = parameters()[i];
        auto value = js_undefined();
        if (i < argument_values.size())
            value = argument_values[i];
        arguments.append({ move(name), move(value) });
    }
    return interpreter.run(m_body, arguments, ScopeType::Function);
}

Value ScriptFunction::construct(Interpreter& interpreter)
{
    return call(interpreter);
}

Value ScriptFunction::length_getter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_function())
        return interpreter.throw_exception<Error>("TypeError", "Not a function");
    return Value(static_cast<i32>(static_cast<const ScriptFunction*>(this_object)->parameters().size()));
}

void ScriptFunction::length_setter(Interpreter&, Value)
{
}

}
