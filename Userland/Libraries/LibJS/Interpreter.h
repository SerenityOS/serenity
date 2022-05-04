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
    // 9.6 InitializeHostDefinedRealm ( ), https://tc39.es/ecma262/#sec-initializehostdefinedrealm
    template<typename GlobalObjectType, typename GlobalThisObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args) requires(IsBaseOf<GlobalObject, GlobalObjectType>&& IsBaseOf<Object, GlobalThisObjectType>)
    {
        DeferGC defer_gc(vm.heap());
        auto interpreter = adopt_own(*new Interpreter(vm));
        VM::InterpreterExecutionScope scope(*interpreter);

        // 1. Let realm be CreateRealm().
        auto* realm = Realm::create(vm);

        // 2. Let newContext be a new execution context.
        auto& new_context = interpreter->m_global_execution_context;

        // 3. Set the Function of newContext to null.
        // NOTE: This was done during execution context construction.

        // 4. Set the Realm of newContext to realm.
        new_context.realm = realm;

        // 5. Set the ScriptOrModule of newContext to null.
        // NOTE: This was done during execution context construction.

        // 6. Push newContext onto the execution context stack; newContext is now the running execution context.
        vm.push_execution_context(new_context);

        // 7. If the host requires use of an exotic object to serve as realm's global object, let global be such an object created in a host-defined manner.
        //    Otherwise, let global be undefined, indicating that an ordinary object should be created as the global object.
        auto* global_object = static_cast<GlobalObject*>(interpreter->heap().allocate_without_global_object<GlobalObjectType>(forward<Args>(args)...));

        // 8. If the host requires that the this binding in realm's global scope return an object other than the global object, let thisValue be such an object created
        //    in a host-defined manner. Otherwise, let thisValue be undefined, indicating that realm's global this binding should be the global object.
        Object* this_value;
        if constexpr (IsSame<GlobalObjectType, GlobalThisObjectType>) {
            this_value = global_object;
        } else {
            // FIXME: Should we pass args in here? Let's er on the side of caution and say yes.
            this_value = static_cast<Object*>(interpreter->heap().allocate_without_global_object<GlobalThisObjectType>(forward<Args>(args)...));
        }

        // 9. Perform SetRealmGlobalObject(realm, global, thisValue).
        realm->set_global_object(*global_object, this_value);

        // NOTE: These are not in the spec.
        static FlyString global_execution_context_name = "(global execution context)";
        interpreter->m_global_execution_context.function_name = global_execution_context_name;

        interpreter->m_global_object = make_handle(global_object);
        interpreter->m_realm = make_handle(realm);

        // 10. Let globalObj be ? SetDefaultGlobalBindings(realm).
        // 11. Create any host-defined global object properties on globalObj.
        static_cast<GlobalObjectType*>(global_object)->initialize_global_object();

        // 12. Return unused.
        return interpreter;
    }

    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args) requires IsBaseOf<GlobalObject, GlobalObjectType>
    {
        // NOTE: This function is here to facilitate step 8 of InitializeHostDefinedRealm. (Callers don't have to specify the same type twice if not necessary)
        return create<GlobalObjectType, GlobalObjectType>(vm, args...);
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
    ExecutionContext m_global_execution_context;
};

}
