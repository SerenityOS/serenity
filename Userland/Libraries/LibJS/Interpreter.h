/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <LibJS/AST.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Script.h>
#include <LibJS/SourceTextModule.h>

namespace JS {

struct ExecutingASTNodeChain {
    ExecutingASTNodeChain* previous { nullptr };
    ASTNode const& node;
};

class Interpreter : public Weakable<Interpreter> {
public:
    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args) requires(IsBaseOf<GlobalObject, GlobalObjectType>)
    {
        DeferGC defer_gc(vm.heap());
        auto interpreter = adopt_own(*new Interpreter(vm));
        VM::InterpreterExecutionScope scope(*interpreter);

        GlobalObject* global_object { nullptr };

        interpreter->m_global_execution_context = MUST(Realm::initialize_host_defined_realm(
            vm,
            [&](Realm& realm) -> GlobalObject* {
                global_object = interpreter->heap().allocate_without_realm<GlobalObjectType>(realm, forward<Args>(args)...);
                return global_object;
            },
            nullptr));

        // NOTE: These are not in the spec.
        static FlyString global_execution_context_name = "(global execution context)";
        interpreter->m_global_execution_context->function_name = global_execution_context_name;

        interpreter->m_global_object = make_handle(global_object);
        interpreter->m_realm = make_handle(global_object->associated_realm());

        return interpreter;
    }

    static NonnullOwnPtr<Interpreter> create_with_existing_realm(Realm&);

    ~Interpreter() = default;

    ThrowCompletionOr<Value> run(Script&);
    ThrowCompletionOr<Value> run(SourceTextModule&);

    GlobalObject& global_object();
    GlobalObject const& global_object() const;

    Realm& realm();
    Realm const& realm() const;

    ALWAYS_INLINE VM& vm() { return *m_vm; }
    ALWAYS_INLINE const VM& vm() const { return *m_vm; }
    ALWAYS_INLINE Heap& heap() { return vm().heap(); }

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

    ASTNode const* current_node() const { return m_ast_node_chain ? &m_ast_node_chain->node : nullptr; }

private:
    explicit Interpreter(VM&);

    ExecutingASTNodeChain* m_ast_node_chain { nullptr };

    NonnullRefPtr<VM> m_vm;

    Handle<GlobalObject> m_global_object;
    Handle<Realm> m_realm;

    // This is here to keep the global execution context alive for the entire lifespan of the Interpreter.
    OwnPtr<ExecutionContext> m_global_execution_context;
};

}
