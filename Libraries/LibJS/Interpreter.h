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

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>
#include <LibJS/AST.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Interpreter : public Weakable<Interpreter> {
public:
    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args)
    {
        DeferGC defer_gc(vm.heap());
        auto interpreter = adopt_own(*new Interpreter(vm));
        VM::InterpreterExecutionScope scope(*interpreter);
        interpreter->m_global_object = make_handle(static_cast<Object*>(interpreter->heap().allocate_without_global_object<GlobalObjectType>(forward<Args>(args)...)));
        static_cast<GlobalObjectType*>(interpreter->m_global_object.cell())->initialize();
        return interpreter;
    }

    static NonnullOwnPtr<Interpreter> create_with_existing_global_object(GlobalObject&);

    ~Interpreter();

    Value run(GlobalObject&, const Program&);

    GlobalObject& global_object();
    const GlobalObject& global_object() const;

    VM& vm() { return *m_vm; }
    const VM& vm() const { return *m_vm; }
    Heap& heap() { return vm().heap(); }
    Exception* exception() { return vm().exception(); }

    LexicalEnvironment* current_environment() { return vm().current_environment(); }

    void enter_scope(const ScopeNode&, ArgumentVector, ScopeType, GlobalObject&);
    void exit_scope(const ScopeNode&);

    Value execute_statement(GlobalObject&, const Statement&, ArgumentVector = {}, ScopeType = ScopeType::Block);

private:
    explicit Interpreter(VM&);

    void push_scope(ScopeFrame frame);

    Vector<ScopeFrame> m_scope_stack;

    NonnullRefPtr<VM> m_vm;

    Handle<Object> m_global_object;
};

}
