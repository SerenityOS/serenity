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
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/Realm.h>
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
        auto* global_object = static_cast<GlobalObject*>(interpreter->heap().allocate_without_global_object<GlobalObjectType>(forward<Args>(args)...));
        auto* realm = Realm::create(vm);
        realm->set_global_object(*global_object, global_object);
        interpreter->m_global_object = make_handle(global_object);
        interpreter->m_realm = make_handle(realm);
        static_cast<GlobalObjectType*>(global_object)->initialize_global_object();
        return interpreter;
    }

    static NonnullOwnPtr<Interpreter> create_with_existing_realm(Realm&);

    ~Interpreter();

    void run(GlobalObject&, const Program&);

    GlobalObject& global_object();
    const GlobalObject& global_object() const;

    Realm& realm();
    Realm const& realm() const;

    ALWAYS_INLINE VM& vm() { return *m_vm; }
    ALWAYS_INLINE const VM& vm() const { return *m_vm; }
    ALWAYS_INLINE Heap& heap() { return vm().heap(); }
    ALWAYS_INLINE Exception* exception() { return vm().exception(); }

    Environment* lexical_environment() { return vm().lexical_environment(); }

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

private:
    explicit Interpreter(VM&);

    ExecutingASTNodeChain* m_ast_node_chain { nullptr };

    NonnullRefPtr<VM> m_vm;

    Handle<GlobalObject> m_global_object;
    Handle<Realm> m_realm;
};

}
