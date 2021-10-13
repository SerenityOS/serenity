/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/WrappedFunction.h>

namespace JS {

ShadowRealm::ShadowRealm(Realm& shadow_realm, ExecutionContext execution_context, Object& prototype)
    : Object(prototype)
    , m_shadow_realm(shadow_realm)
    , m_execution_context(move(execution_context))
{
}

void ShadowRealm::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(&m_shadow_realm);
}

// 3.1.1 PerformShadowRealmEval ( sourceText, callerRealm, evalRealm ), https://tc39.es/proposal-shadowrealm/#sec-performshadowrealmeval
ThrowCompletionOr<Value> perform_shadow_realm_eval(GlobalObject& global_object, StringView source_text, Realm& caller_realm, Realm& eval_realm)
{
    auto& vm = global_object.vm();

    // 1. Assert: Type(sourceText) is String.
    // 2. Assert: callerRealm is a Realm Record.
    // 3. Assert: evalRealm is a Realm Record.

    // 4. Perform ? HostEnsureCanCompileStrings(callerRealm, evalRealm).
    // FIXME: We don't have this host-defined abstract operation yet.

    // 5. Perform the following substeps in an implementation-defined order, possibly interleaving parsing and error detection:

    // a. Let script be ParseText(! StringToCodePoints(sourceText), Script).
    auto parser = Parser(Lexer(source_text));
    auto program = parser.parse_program();

    // b. If script is a List of errors, throw a SyntaxError exception.
    if (parser.has_errors()) {
        auto& error = parser.errors()[0];
        return vm.throw_completion<JS::SyntaxError>(global_object, error.to_string());
    }

    // c. If script Contains ScriptBody is false, return undefined.
    if (program->children().is_empty())
        return js_undefined();

    // d. Let body be the ScriptBody of script.
    // e. If body Contains NewTarget is true, throw a SyntaxError exception.
    // f. If body Contains SuperProperty is true, throw a SyntaxError exception.
    // g. If body Contains SuperCall is true, throw a SyntaxError exception.
    // FIXME: Implement these, we probably need a generic way of scanning the AST for certain nodes.

    // 6. Let strictEval be IsStrict of script.
    auto strict_eval = program->is_strict_mode();

    // 7. Let runningContext be the running execution context.
    // NOTE: This would be unused due to step 11 and is omitted for that reason.

    // 8. Let lexEnv be NewDeclarativeEnvironment(evalRealm.[[GlobalEnv]]).
    Environment* lexical_environment = new_declarative_environment(eval_realm.global_environment());

    // 9. Let varEnv be evalRealm.[[GlobalEnv]].
    Environment* variable_environment = &eval_realm.global_environment();

    // 10. If strictEval is true, set varEnv to lexEnv.
    if (strict_eval)
        variable_environment = lexical_environment;

    // 11. If runningContext is not already suspended, suspend runningContext.
    // NOTE: We don't support this concept yet.

    // 12. Let evalContext be a new ECMAScript code execution context.
    auto eval_context = ExecutionContext { vm.heap() };

    // 13. Set evalContext's Function to null.
    eval_context.function = nullptr;

    // 14. Set evalContext's Realm to evalRealm.
    eval_context.realm = &eval_realm;

    // 15. Set evalContext's ScriptOrModule to null.
    // FIXME: Our execution context struct currently does not track this item.

    // 16. Set evalContext's VariableEnvironment to varEnv.
    eval_context.variable_environment = variable_environment;

    // 17. Set evalContext's LexicalEnvironment to lexEnv.
    eval_context.lexical_environment = lexical_environment;

    // Non-standard
    eval_context.is_strict_mode = strict_eval;

    // 18. Push evalContext onto the execution context stack; evalContext is now the running execution context.
    vm.push_execution_context(eval_context, eval_realm.global_object());

    // 19. Let result be EvalDeclarationInstantiation(body, varEnv, lexEnv, null, strictEval).
    auto eval_result = eval_declaration_instantiation(vm, eval_realm.global_object(), program, variable_environment, lexical_environment, strict_eval);

    Completion result;

    // 20. If result.[[Type]] is normal, then
    if (!eval_result.is_throw_completion()) {
        // TODO: Optionally use bytecode interpreter?
        // FIXME: We need to use evaluate_statements() here because Program::execute() calls global_declaration_instantiation() when it shouldn't
        // a. Set result to the result of evaluating body.
        auto result_value = program->evaluate_statements(vm.interpreter(), eval_realm.global_object());
        if (auto* exception = vm.exception())
            result = throw_completion(exception->value());
        else if (!result_value.is_empty())
            result = normal_completion(result_value);
        else
            result = Completion {}; // Normal completion with no value
    }

    // 21. If result.[[Type]] is normal and result.[[Value]] is empty, then
    if (result.type() == Completion::Type::Normal && !result.has_value()) {
        // a. Set result to NormalCompletion(undefined).
        result = normal_completion(js_undefined());
    }

    // 22. Suspend evalContext and remove it from the execution context stack.
    // NOTE: We don't support this concept yet.
    vm.pop_execution_context();

    // 23. Resume the context that is now on the top of the execution context stack as the running execution context.
    // NOTE: We don't support this concept yet.

    // 24. If result.[[Type]] is not normal, throw a TypeError exception.
    if (result.type() != Completion::Type::Normal)
        return vm.throw_completion<TypeError>(global_object, ErrorType::ShadowRealmEvaluateAbruptCompletion);

    // 25. Return ? GetWrappedValue(callerRealm, result.[[Value]]).
    return get_wrapped_value(global_object, caller_realm, result.value());

    // NOTE: Also see "Editor's Note" in the spec regarding the TypeError above.
}

// 3.1.3 GetWrappedValue ( callerRealm, value ), https://tc39.es/proposal-shadowrealm/#sec-getwrappedvalue
ThrowCompletionOr<Value> get_wrapped_value(GlobalObject& global_object, Realm& caller_realm, Value value)
{
    auto& vm = global_object.vm();

    // 1. Assert: callerRealm is a Realm Record.

    // 2. If Type(value) is Object, then
    if (value.is_object()) {
        // a. If IsCallable(value) is false, throw a TypeError exception.
        if (!value.is_function())
            return vm.throw_completion<TypeError>(global_object, ErrorType::ShadowRealmWrappedValueNonFunctionObject, value);

        // b. Return ! WrappedFunctionCreate(callerRealm, value).
        return { WrappedFunction::create(global_object, caller_realm, value.as_function()) };
    }

    // 3. Return value.
    return value;
}

}
