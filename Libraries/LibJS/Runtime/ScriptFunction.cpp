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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ScriptFunction::ScriptFunction(const ScopeNode& body, Vector<String> parameters)
    : m_body(body)
    , m_parameters(move(parameters))
{
}

ScriptFunction::~ScriptFunction()
{
}

Value ScriptFunction::call(Interpreter& interpreter, const Vector<Value>& argument_values)
{
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

}
