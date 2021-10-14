/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NonnullOwnPtr<Interpreter> Interpreter::create_with_existing_realm(Realm& realm)
{
    auto& global_object = realm.global_object();
    DeferGC defer_gc(global_object.heap());
    auto interpreter = adopt_own(*new Interpreter(global_object.vm()));
    interpreter->m_global_object = make_handle(&global_object);
    interpreter->m_realm = make_handle(&realm);
    return interpreter;
}

Interpreter::Interpreter(VM& vm)
    : m_vm(vm)
    , m_global_execution_context(vm.heap())
{
}

Interpreter::~Interpreter()
{
}

// 16.1.6 ScriptEvaluation ( scriptRecord ), https://tc39.es/ecma262/#sec-runtime-semantics-scriptevaluation
void Interpreter::run(Script& script_record)
{
    auto& vm = this->vm();
    VERIFY(!vm.exception());

    VM::InterpreterExecutionScope scope(*this);

    // FIXME: Replace last value with returning a completion as per the spec.
    vm.set_last_value(Badge<Interpreter> {}, {});

    // 1. Let globalEnv be scriptRecord.[[Realm]].[[GlobalEnv]].
    auto& global_environment = script_record.realm().global_environment();

    // NOTE: This isn't in the spec but we require it.
    auto& global_object = script_record.realm().global_object();

    // 2. Let scriptContext be a new ECMAScript code execution context.
    ExecutionContext script_context(vm.heap());

    // 3. Set the Function of scriptContext to null. (This was done in the construction of script_context)

    // 4. Set the Realm of scriptContext to scriptRecord.[[Realm]].
    script_context.realm = &script_record.realm();

    // 5. Set the ScriptOrModule of scriptContext to scriptRecord.
    script_context.script_or_module = script_record.make_weak_ptr();

    // 6. Set the VariableEnvironment of scriptContext to globalEnv.
    script_context.variable_environment = &global_environment;

    // 7. Set the LexicalEnvironment of scriptContext to globalEnv.
    script_context.lexical_environment = &global_environment;

    // FIXME: 8. Set the PrivateEnvironment of scriptContext to null.

    // NOTE: This isn't in the spec, but we require it.
    script_context.is_strict_mode = script_record.parse_node().is_strict_mode();

    // FIXME: 9. Suspend the currently running execution context.

    // 10. Push scriptContext onto the execution context stack; scriptContext is now the running execution context.
    vm.push_execution_context(script_context, global_object);

    // FIXME: Handle running out of stack space.
    VERIFY(!vm.exception());

    // 11. Let scriptBody be scriptRecord.[[ECMAScriptCode]].
    auto& script_body = script_record.parse_node();

    // 12. Let result be GlobalDeclarationInstantiation(scriptBody, globalEnv).
    auto result = script_body.global_declaration_instantiation(*this, global_object, global_environment);

    // 13. If result.[[Type]] is normal, then
    if (!result.is_throw_completion()) {
        // FIXME: a. Set result to the result of evaluating scriptBody.
        auto value = script_body.execute(*this, global_object);
        vm.set_last_value(Badge<Interpreter> {}, value.value_or(js_undefined()));
    }

    // FIXME: 14. If result.[[Type]] is normal and result.[[Value]] is empty, then
    //          a. Set result to NormalCompletion(undefined).

    // FIXME: We unconditionally stop the unwind here this should be done using completions leaving
    //        the VM in a cleaner state after executing. For example it does still store the exception.
    vm.stop_unwind();

    // FIXME: 15. Suspend scriptContext and remove it from the execution context stack.
    vm.pop_execution_context();

    // 16. Assert: The execution context stack is not empty.
    VERIFY(!vm.execution_context_stack().is_empty());

    // FIXME: 17. Resume the context that is now on the top of the execution context stack as the running execution context.

    // FIXME: 18. Return Completion(result).

    // At this point we may have already run any queued promise jobs via on_call_stack_emptied,
    // in which case this is a no-op.
    // FIXME: These three should be moved out of Interpreter::run and give the host an option to run these, as it's up to the host when these get run.
    //        https://tc39.es/ecma262/#sec-jobs for jobs and https://tc39.es/ecma262/#_ref_3508 for ClearKeptObjects
    //        finish_execution_generation is particularly an issue for LibWeb, as the HTML spec wants to run it specifically after performing a microtask checkpoint.
    //        The promise and registry cleanup queues don't cause LibWeb an issue, as LibWeb overrides the hooks that push onto these queues.
    vm.run_queued_promise_jobs();

    vm.run_queued_finalization_registry_cleanup_jobs();

    vm.finish_execution_generation();
}

void Interpreter::run(SourceTextModule&)
{
    TODO();
}

GlobalObject& Interpreter::global_object()
{
    return static_cast<GlobalObject&>(*m_global_object.cell());
}

const GlobalObject& Interpreter::global_object() const
{
    return static_cast<const GlobalObject&>(*m_global_object.cell());
}

Realm& Interpreter::realm()
{
    return static_cast<Realm&>(*m_realm.cell());
}

const Realm& Interpreter::realm() const
{
    return static_cast<const Realm&>(*m_realm.cell());
}

}
