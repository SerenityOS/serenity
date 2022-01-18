/*
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GeneratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

FunctionConstructor::FunctionConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Function.as_string(), *global_object.function_prototype())
{
}

void FunctionConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 20.2.2.2 Function.prototype, https://tc39.es/ecma262/#sec-function.prototype
    define_direct_property(vm.names.prototype, global_object.function_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

FunctionConstructor::~FunctionConstructor()
{
}

// 20.2.1.1.1 CreateDynamicFunction ( constructor, newTarget, kind, args ), https://tc39.es/ecma262/#sec-createdynamicfunction
ThrowCompletionOr<ECMAScriptFunctionObject*> FunctionConstructor::create_dynamic_function(GlobalObject& global_object, FunctionObject& constructor, FunctionObject* new_target, FunctionKind kind, MarkedValueList const& args)
{
    auto& vm = global_object.vm();

    // 1. Assert: The execution context stack has at least two elements.
    VERIFY(vm.execution_context_stack().size() >= 2);

    // 2. Let callerContext be the second to top element of the execution context stack.
    // 3. Let callerRealm be callerContext's Realm.
    // 4. Let calleeRealm be the current Realm Record.
    // NOTE: All of these are only needed for the next step.

    // 5. Perform ? HostEnsureCanCompileStrings(callerRealm, calleeRealm).
    // NOTE: We don't have this yet.

    // 6. If newTarget is undefined, set newTarget to constructor.
    if (new_target == nullptr)
        new_target = &constructor;

    StringView prefix;
    Object* (GlobalObject::*fallback_prototype)() = nullptr;

    switch (kind) {
    // 7. If kind is normal, then
    case FunctionKind::Normal:
        // a. Let prefix be "function".
        prefix = "function"sv;

        // b. Let exprSym be the grammar symbol FunctionExpression.
        // c. Let bodySym be the grammar symbol FunctionBody[~Yield, ~Await].
        // d. Let parameterSym be the grammar symbol FormalParameters[~Yield, ~Await].

        // e. Let fallbackProto be "%Function.prototype%".
        fallback_prototype = &GlobalObject::function_prototype;
        break;

    // 8. Else if kind is generator, then
    case FunctionKind::Generator:
        // a. Let prefix be "function*".
        prefix = "function*"sv;

        // b. Let exprSym be the grammar symbol GeneratorExpression.
        // c. Let bodySym be the grammar symbol GeneratorBody.
        // d. Let parameterSym be the grammar symbol FormalParameters[+Yield, ~Await].

        // e. Let fallbackProto be "%GeneratorFunction.prototype%".
        fallback_prototype = &GlobalObject::generator_function_prototype;
        break;

    // 9. Else if kind is async, then
    case FunctionKind::Async:
        // a. Let prefix be "async function".
        prefix = "async function"sv;

        // b. Let exprSym be the grammar symbol AsyncFunctionExpression.
        // c. Let bodySym be the grammar symbol AsyncFunctionBody.
        // d. Let parameterSym be the grammar symbol FormalParameters[~Yield, +Await].

        // e. Let fallbackProto be "%AsyncFunction.prototype%".
        fallback_prototype = &GlobalObject::async_function_prototype;
        break;

    // 10. Else,
    case FunctionKind::AsyncGenerator:
        // a. Assert: kind is asyncGenerator.

        // b. Let prefix be "async function*".
        prefix = "async function*"sv;

        // c. Let exprSym be the grammar symbol AsyncGeneratorExpression.
        // d. Let bodySym be the grammar symbol AsyncGeneratorBody.
        // e. Let parameterSym be the grammar symbol FormalParameters[+Yield, +Await].

        // f. Let fallbackProto be "%AsyncGeneratorFunction.prototype%".
        fallback_prototype = &GlobalObject::async_generator_function_prototype;
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    // 11. Let argCount be the number of elements in args.
    auto arg_count = args.size();

    // 12. Let P be the empty String.
    String parameters_string = "";

    Optional<Value> body_arg;

    // 13. If argCount = 0, let bodyArg be the empty String.
    if (arg_count == 0) {
        // Optimization: Instead of creating a js_string() here, we just check if body_arg is empty in step 16.
    }
    // 14. Else if argCount = 1, let bodyArg be args[0].
    else if (arg_count == 1) {
        body_arg = args[0];
    }
    // 15. Else,
    else {
        // a. Assert: argCount > 1.
        VERIFY(arg_count > 1);

        // b. Let firstArg be args[0].
        // c. Set P to ? ToString(firstArg).
        // NOTE: Also done in the loop. We start at 0 instead and then join() with a comma.

        // d. Let k be 1.
        size_t k = 0;

        // e. Repeat, while k < argCount - 1,
        Vector<String> parameters;
        for (; k < arg_count - 1; ++k) {
            // i. Let nextArg be args[k].
            auto next_arg = args[k];

            // ii. Let nextArgString be ? ToString(nextArg).
            // iii. Set P to the string-concatenation of P, "," (a comma), and nextArgString.
            parameters.append(TRY(next_arg.to_string(global_object)));

            // iv. Set k to k + 1.
        }
        parameters_string = String::join(',', parameters);

        // f. Let bodyArg be args[k].
        body_arg = args[k];
    }

    // 16. Let bodyString be the string-concatenation of 0x000A (LINE FEED), ? ToString(bodyArg), and 0x000A (LINE FEED).
    auto body_string = String::formatted("\n{}\n", body_arg.has_value() ? TRY(body_arg->to_string(global_object)) : "");

    // 17. Let sourceString be the string-concatenation of prefix, " anonymous(", P, 0x000A (LINE FEED), ") {", bodyString, and "}".
    // 18. Let sourceText be ! StringToCodePoints(sourceString).
    auto source_text = String::formatted("{} anonymous({}\n) {{{}}}", prefix, parameters_string, body_string);

    u8 parse_options = FunctionNodeParseOptions::CheckForFunctionAndName;
    if (kind == FunctionKind::Async || kind == FunctionKind::AsyncGenerator)
        parse_options |= FunctionNodeParseOptions::IsAsyncFunction;
    if (kind == FunctionKind::Generator || kind == FunctionKind::AsyncGenerator)
        parse_options |= FunctionNodeParseOptions::IsGeneratorFunction;

    // 19. Let parameters be ParseText(! StringToCodePoints(P), parameterSym).
    i32 function_length = 0;
    auto parameters_parser = Parser { Lexer { parameters_string } };
    auto parameters = parameters_parser.parse_formal_parameters(function_length, parse_options);

    // 20. If parameters is a List of errors, throw a SyntaxError exception.
    if (parameters_parser.has_errors()) {
        auto error = parameters_parser.errors()[0];
        return vm.throw_completion<SyntaxError>(global_object, error.to_string());
    }

    // 21. Let body be ParseText(! StringToCodePoints(bodyString), bodySym).
    bool contains_direct_call_to_eval = false;
    auto body_parser = Parser { Lexer { body_string } };
    // Set up some parser state to accept things like return await, and yield in the plain function body.
    body_parser.m_state.in_function_context = true;
    if ((parse_options & FunctionNodeParseOptions::IsAsyncFunction) != 0)
        body_parser.m_state.await_expression_is_valid = true;
    if ((parse_options & FunctionNodeParseOptions::IsGeneratorFunction) != 0)
        body_parser.m_state.in_generator_function_context = true;
    (void)body_parser.parse_function_body(parameters, kind, contains_direct_call_to_eval);

    // 22. If body is a List of errors, throw a SyntaxError exception.
    if (body_parser.has_errors()) {
        auto error = body_parser.errors()[0];
        return vm.throw_completion<SyntaxError>(global_object, error.to_string());
    }

    // 23. NOTE: The parameters and body are parsed separately to ensure that each is valid alone. For example, new Function("/*", "*/ ) {") is not legal.
    // 24. NOTE: If this step is reached, sourceText must have the syntax of exprSym (although the reverse implication does not hold). The purpose of the next two steps is to enforce any Early Error rules which apply to exprSym directly.

    // 25. Let expr be ParseText(sourceText, exprSym).
    auto source_parser = Parser { Lexer { source_text } };
    // This doesn't need any parse_options, it determines those & the function type based on the tokens that were found.
    auto expr = source_parser.parse_function_node<FunctionExpression>();

    // 26. If expr is a List of errors, throw a SyntaxError exception.
    if (source_parser.has_errors()) {
        auto error = source_parser.errors()[0];
        return vm.throw_completion<SyntaxError>(global_object, error.to_string());
    }

    // 27. Let proto be ? GetPrototypeFromConstructor(newTarget, fallbackProto).
    auto* prototype = TRY(get_prototype_from_constructor(global_object, *new_target, fallback_prototype));

    // 28. Let realmF be the current Realm Record.
    auto* realm = vm.current_realm();

    // 29. Let scope be realmF.[[GlobalEnv]].
    auto* scope = &realm->global_environment();

    // 30. Let privateScope be null.
    PrivateEnvironment* private_scope = nullptr;

    // 31. Let F be ! OrdinaryFunctionCreate(proto, sourceText, parameters, body, non-lexical-this, scope, privateScope).
    auto* function = ECMAScriptFunctionObject::create(global_object, "anonymous", *prototype, move(source_text), expr->body(), expr->parameters(), expr->function_length(), scope, private_scope, expr->kind(), expr->is_strict_mode(), expr->might_need_arguments_object(), contains_direct_call_to_eval);

    // FIXME: Remove the name argument from create() and do this instead.
    // 32. Perform SetFunctionName(F, "anonymous").

    // 33. If kind is generator, then
    if (kind == FunctionKind::Generator) {
        // a. Let prototype be ! OrdinaryObjectCreate(%GeneratorFunction.prototype.prototype%).
        prototype = Object::create(global_object, global_object.generator_prototype());

        // b. Perform DefinePropertyOrThrow(F, "prototype", PropertyDescriptor { [[Value]]: prototype, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: false }).
        function->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }
    // 34. Else if kind is asyncGenerator, then
    else if (kind == FunctionKind::AsyncGenerator) {
        // FIXME: We only have %AsyncGeneratorFunction.prototype%, not %AsyncGeneratorFunction.prototype.prototype%!
        // a. Let prototype be ! OrdinaryObjectCreate(%AsyncGeneratorFunction.prototype.prototype%).
        // prototype = Object::create(global_object, global_object.async_generator_prototype());

        // b. Perform DefinePropertyOrThrow(F, "prototype", PropertyDescriptor { [[Value]]: prototype, [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: false }).
        // function->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }
    // 35. Else if kind is normal, perform MakeConstructor(F).
    else if (kind == FunctionKind::Normal) {
        // FIXME: Implement MakeConstructor
        prototype = Object::create(global_object, global_object.object_prototype());
        prototype->define_direct_property(vm.names.constructor, function, Attribute::Writable | Attribute::Configurable);
        function->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }

    // 36. NOTE: Functions whose kind is async are not constructible and do not have a [[Construct]] internal method or a "prototype" property.

    // 37. Return F.
    return function;
}

// 20.2.1.1 Function ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-function-p1-p2-pn-body
ThrowCompletionOr<Value> FunctionConstructor::call()
{
    return TRY(construct(*this));
}

// 20.2.1.1 Function ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-function-p1-p2-pn-body
ThrowCompletionOr<Object*> FunctionConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 1. Let C be the active function object.
    auto* constructor = vm.active_function_object();

    // 2. Let args be the argumentsList that was passed to this function by [[Call]] or [[Construct]].
    auto& args = vm.running_execution_context().arguments;

    // 3. Return ? CreateDynamicFunction(C, NewTarget, normal, args).
    return TRY(create_dynamic_function(global_object, *constructor, &new_target, FunctionKind::Normal, args));
}

}
