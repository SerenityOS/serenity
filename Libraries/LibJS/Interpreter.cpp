/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Badge.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/Value.h>

//#define INTERPRETER_DEBUG

namespace JS {

Interpreter::Interpreter(VM& vm)
    : m_vm(vm)
    , m_console(*this)
{
}

Interpreter::~Interpreter()
{
}

Value Interpreter::run(GlobalObject& global_object, const Program& program)
{
    ASSERT(!vm().exception());

    VM::InterpreterExecutionScope scope(*this);

    CallFrame global_call_frame;
    global_call_frame.this_value = &global_object;
    global_call_frame.function_name = "(global execution context)";
    global_call_frame.environment = heap().allocate<LexicalEnvironment>(global_object, LexicalEnvironment::EnvironmentRecordType::Global);
    global_call_frame.environment->bind_this_value(&global_object);
    if (vm().exception())
        return {};
    vm().call_stack().append(move(global_call_frame));

    auto result = program.execute(*this, global_object);
    vm().pop_call_frame();
    return result;
}

GlobalObject& Interpreter::global_object()
{
    return static_cast<GlobalObject&>(*m_global_object.cell());
}

const GlobalObject& Interpreter::global_object() const
{
    return static_cast<const GlobalObject&>(*m_global_object.cell());
}

Value Interpreter::call_internal(Function& function, Value this_value, Optional<MarkedValueList> arguments)
{
    ASSERT(!exception());

    VM::InterpreterExecutionScope scope(*this);

    auto& call_frame = vm().push_call_frame();
    call_frame.function_name = function.name();
    call_frame.this_value = function.bound_this().value_or(this_value);
    call_frame.arguments = function.bound_arguments();
    if (arguments.has_value())
        call_frame.arguments.append(arguments.value().values());
    call_frame.environment = function.create_environment();

    ASSERT(call_frame.environment->this_binding_status() == LexicalEnvironment::ThisBindingStatus::Uninitialized);
    call_frame.environment->bind_this_value(call_frame.this_value);

    auto result = function.call(*this);
    vm().pop_call_frame();
    return result;
}

}
