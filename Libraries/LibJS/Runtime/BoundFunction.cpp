/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
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
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BoundFunction::BoundFunction(Function& target_function, Value bound_this, Vector<Value> arguments, i32 length, Object* constructor_prototype)
    : Function::Function(*interpreter().global_object().function_prototype(), bound_this, move(arguments))
    , m_target_function(&target_function)
    , m_constructor_prototype(constructor_prototype)
    , m_name(String::format("bound %s", target_function.name().characters()))
{
    put("length", Value(length));
}

BoundFunction::~BoundFunction()
{
}

Value BoundFunction::call(Interpreter& interpreter)
{
    return m_target_function->call(interpreter);
}

Value BoundFunction::construct(Interpreter& interpreter)
{
    if (auto this_value = interpreter.this_value(); m_constructor_prototype && this_value.is_object()) {
        this_value.as_object().set_prototype(m_constructor_prototype);
    }
    return m_target_function->construct(interpreter);
}

LexicalEnvironment* BoundFunction::create_environment()
{
    return m_target_function->create_environment();
}

void BoundFunction::visit_children(Visitor& visitor)
{
    Function::visit_children(visitor);
    visitor.visit(m_target_function);
    visitor.visit(m_constructor_prototype);
}

}
