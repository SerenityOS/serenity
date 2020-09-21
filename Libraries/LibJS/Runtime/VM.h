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

#pragma once

#include <AK/RefCounted.h>
#include <LibJS/Heap/Heap.h>

namespace JS {

class VM : public RefCounted<VM> {
public:
    static NonnullRefPtr<VM> create();
    ~VM();

    Heap& heap() { return m_heap; }
    const Heap& heap() const { return m_heap; }

    Interpreter& interpreter();
    Interpreter* interpreter_if_exists();

    void push_interpreter(Interpreter&);
    void pop_interpreter(Interpreter&);

    Exception* exception()
    {
        return m_exception;
    }
    void set_exception(Badge<Interpreter>, Exception* exception) { m_exception = exception; }
    void clear_exception() { m_exception = nullptr; }

    class InterpreterExecutionScope {
    public:
        InterpreterExecutionScope(Interpreter&);
        ~InterpreterExecutionScope();

    private:
        Interpreter& m_interpreter;
    };

    void gather_roots(HashTable<Cell*>&);

private:
    VM();

    Exception* m_exception { nullptr };

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;
};

}
