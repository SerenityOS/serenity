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

struct ExecutingASTNodeChain {
    ExecutingASTNodeChain* previous { nullptr };
    const ASTNode& node;
};

class Interpreter : public Weakable<Interpreter> {
public:
    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args)
    {
        DeferGC defer_gc(vm.heap());
        auto interpreter = adopt_own(*new Interpreter(vm));
        VM::InterpreterExecutionScope scope(*interpreter);
        interpreter->m_global_object = make_handle(static_cast<Object*>(interpreter->heap().allocate_without_global_object<GlobalObjectType>(forward<Args>(args)...)));
        static_cast<GlobalObjectType*>(interpreter->m_global_object.cell())->initialize_global_object();
        return interpreter;
    }

    static NonnullOwnPtr<Interpreter> create_with_existing_global_object(GlobalObject&);

    ~Interpreter();

    void run(GlobalObject&, const Program&);

    GlobalObject& global_object();
    const GlobalObject& global_object() const;

    ALWAYS_INLINE VM& vm() { return *m_vm; }
    ALWAYS_INLINE const VM& vm() const { return *m_vm; }
    ALWAYS_INLINE Heap& heap() { return vm().heap(); }
    ALWAYS_INLINE Exception* exception() { return vm().exception(); }

    ScopeObject* current_scope() { return vm().current_scope(); }
    LexicalEnvironment* current_environment();

    void enter_scope(const ScopeNode&, ScopeType, GlobalObject&);
    void exit_scope(const ScopeNode&);

    void push_ast_node(ExecutingASTNodeChain& chain_node)
    {
        chain_node.previous = m_ast_node_chain;
        m_ast_node_chain = &chain_node;
    }

    void pop_ast_node()
    {
        VERIFY(m_ast_node_chain);
        m_ast_node_chain = m_ast_node_chain->previous;
    }

    const ASTNode* current_node() const { return m_ast_node_chain ? &m_ast_node_chain->node : nullptr; }
    ExecutingASTNodeChain* executing_ast_node_chain() { return m_ast_node_chain; }
    const ExecutingASTNodeChain* executing_ast_node_chain() const { return m_ast_node_chain; }

    Value execute_statement(GlobalObject&, const Statement&, ScopeType = ScopeType::Block);

private:
    explicit Interpreter(VM&);

    void push_scope(ScopeFrame frame);

    Vector<ScopeFrame> m_scope_stack;
    ExecutingASTNodeChain* m_ast_node_chain { nullptr };

    NonnullRefPtr<VM> m_vm;

    Handle<Object> m_global_object;
};

}
