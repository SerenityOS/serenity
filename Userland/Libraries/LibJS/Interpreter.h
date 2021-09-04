/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

struct ExecutingASTNodeChain {
    ExecutingASTNodeChain* previous { nullptr };
    ASTNode const& node;
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

    void run(GlobalObject&, Program const&);

    GlobalObject& global_object();
    GlobalObject const& global_object() const;

    ALWAYS_INLINE VM& vm() { return *m_vm; }
    ALWAYS_INLINE VM const& vm() const { return *m_vm; }
    ALWAYS_INLINE Heap& heap() { return vm().heap(); }
    ALWAYS_INLINE Exception* exception() { return vm().exception(); }

    Environment* lexical_environment() { return vm().lexical_environment(); }

    FunctionEnvironment* current_function_environment();

    void enter_scope(ScopeNode const&, ScopeType, GlobalObject&);
    void exit_scope(ScopeNode const&);

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

    ASTNode const* current_node() const { return m_ast_node_chain ? &m_ast_node_chain->node : nullptr; }
    ExecutingASTNodeChain* executing_ast_node_chain() { return m_ast_node_chain; }
    ExecutingASTNodeChain const* executing_ast_node_chain() const { return m_ast_node_chain; }

    Value execute_statement(GlobalObject&, Statement const&, ScopeType = ScopeType::Block);

private:
    explicit Interpreter(VM&);

    void push_scope(ScopeFrame frame);

    Vector<ScopeFrame> m_scope_stack;
    ExecutingASTNodeChain* m_ast_node_chain { nullptr };

    NonnullRefPtr<VM> m_vm;

    Handle<Object> m_global_object;
};

}
