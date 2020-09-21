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

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

NonnullRefPtr<VM> VM::create()
{
    return adopt(*new VM);
}

VM::VM()
    : m_heap(*this)
{
}

VM::~VM()
{
}

Interpreter& VM::interpreter()
{
    if (m_interpreters.is_empty()) {
        asm volatile("ud2");
    }
//    ASSERT(!m_interpreters.is_empty());
    return *m_interpreters.last();
}

Interpreter* VM::interpreter_if_exists()
{
    if (m_interpreters.is_empty())
        return nullptr;
    return m_interpreters.last();
}

void VM::push_interpreter(Interpreter& interpreter)
{
    m_interpreters.append(&interpreter);
}

void VM::pop_interpreter(Interpreter& interpreter)
{
    ASSERT(!m_interpreters.is_empty());
    auto* popped_interpreter = m_interpreters.take_last();
    ASSERT(popped_interpreter == &interpreter);
}

VM::InterpreterScope::InterpreterScope(Interpreter& interpreter)
    : m_interpreter(interpreter)
{
    m_interpreter.vm().push_interpreter(m_interpreter);
}

VM::InterpreterScope::~InterpreterScope()
{
    m_interpreter.vm().pop_interpreter(m_interpreter);
}

void VM::gather_roots(HashTable<Cell*>& roots)
{
    for (auto* interpreter : m_interpreters)
        interpreter->gather_roots(roots);
}

}
