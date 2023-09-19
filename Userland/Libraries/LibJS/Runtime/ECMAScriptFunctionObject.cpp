/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/AsyncFunctionDriverWrapper.h>
#include <LibJS/Runtime/AsyncGenerator.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GeneratorObject.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NonnullGCPtr<ECMAScriptFunctionObject> ECMAScriptFunctionObject::create(Realm& realm, DeprecatedFlyString name, DeprecatedString source_text, Statement const& ecmascript_code, Vector<FunctionParameter> parameters, i32 m_function_length, Vector<DeprecatedFlyString> local_variables_names, Environment* parent_environment, PrivateEnvironment* private_environment, FunctionKind kind, bool is_strict, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function, Variant<PropertyKey, PrivateName, Empty> class_field_initializer_name)
{
    Object* prototype = nullptr;
    switch (kind) {
    case FunctionKind::Normal:
        prototype = realm.intrinsics().function_prototype();
        break;
    case FunctionKind::Generator:
        prototype = realm.intrinsics().generator_function_prototype();
        break;
    case FunctionKind::Async:
        prototype = realm.intrinsics().async_function_prototype();
        break;
    case FunctionKind::AsyncGenerator:
        prototype = realm.intrinsics().async_generator_function_prototype();
        break;
    }
    return realm.heap().allocate<ECMAScriptFunctionObject>(realm, move(name), move(source_text), ecmascript_code, move(parameters), m_function_length, move(local_variables_names), parent_environment, private_environment, *prototype, kind, is_strict, might_need_arguments_object, contains_direct_call_to_eval, is_arrow_function, move(class_field_initializer_name));
}

NonnullGCPtr<ECMAScriptFunctionObject> ECMAScriptFunctionObject::create(Realm& realm, DeprecatedFlyString name, Object& prototype, DeprecatedString source_text, Statement const& ecmascript_code, Vector<FunctionParameter> parameters, i32 m_function_length, Vector<DeprecatedFlyString> local_variables_names, Environment* parent_environment, PrivateEnvironment* private_environment, FunctionKind kind, bool is_strict, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function, Variant<PropertyKey, PrivateName, Empty> class_field_initializer_name)
{
    return realm.heap().allocate<ECMAScriptFunctionObject>(realm, move(name), move(source_text), ecmascript_code, move(parameters), m_function_length, move(local_variables_names), parent_environment, private_environment, prototype, kind, is_strict, might_need_arguments_object, contains_direct_call_to_eval, is_arrow_function, move(class_field_initializer_name));
}

ECMAScriptFunctionObject::ECMAScriptFunctionObject(DeprecatedFlyString name, DeprecatedString source_text, Statement const& ecmascript_code, Vector<FunctionParameter> formal_parameters, i32 function_length, Vector<DeprecatedFlyString> local_variables_names, Environment* parent_environment, PrivateEnvironment* private_environment, Object& prototype, FunctionKind kind, bool strict, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function, Variant<PropertyKey, PrivateName, Empty> class_field_initializer_name)
    : FunctionObject(prototype)
    , m_name(move(name))
    , m_function_length(function_length)
    , m_local_variables_names(move(local_variables_names))
    , m_environment(parent_environment)
    , m_private_environment(private_environment)
    , m_formal_parameters(move(formal_parameters))
    , m_ecmascript_code(ecmascript_code)
    , m_realm(&prototype.shape().realm())
    , m_source_text(move(source_text))
    , m_class_field_initializer_name(move(class_field_initializer_name))
    , m_strict(strict)
    , m_might_need_arguments_object(might_need_arguments_object)
    , m_contains_direct_call_to_eval(contains_direct_call_to_eval)
    , m_is_arrow_function(is_arrow_function)
    , m_kind(kind)
{
    // NOTE: This logic is from OrdinaryFunctionCreate, https://tc39.es/ecma262/#sec-ordinaryfunctioncreate

    // 9. If thisMode is lexical-this, set F.[[ThisMode]] to lexical.
    if (m_is_arrow_function)
        m_this_mode = ThisMode::Lexical;
    // 10. Else if Strict is true, set F.[[ThisMode]] to strict.
    else if (m_strict)
        m_this_mode = ThisMode::Strict;
    else
        // 11. Else, set F.[[ThisMode]] to global.
        m_this_mode = ThisMode::Global;

    // 15. Set F.[[ScriptOrModule]] to GetActiveScriptOrModule().
    m_script_or_module = vm().get_active_script_or_module();

    // 15.1.3 Static Semantics: IsSimpleParameterList, https://tc39.es/ecma262/#sec-static-semantics-issimpleparameterlist
    m_has_simple_parameter_list = all_of(m_formal_parameters, [&](auto& parameter) {
        if (parameter.is_rest)
            return false;
        if (parameter.default_value)
            return false;
        if (!parameter.binding.template has<NonnullRefPtr<Identifier const>>())
            return false;
        return true;
    });

    // NOTE: The following steps are from FunctionDeclarationInstantiation that could be executed once
    //       and then reused in all subsequent function instantiations.

    // 2. Let code be func.[[ECMAScriptCode]].
    ScopeNode const* scope_body = nullptr;
    if (is<ScopeNode>(*m_ecmascript_code))
        scope_body = static_cast<ScopeNode const*>(m_ecmascript_code.ptr());

    // 3. Let strict be func.[[Strict]].

    // 4. Let formals be func.[[FormalParameters]].
    auto const& formals = m_formal_parameters;

    // 5. Let parameterNames be the BoundNames of formals.
    // 6. If parameterNames has any duplicate entries, let hasDuplicates be true. Otherwise, let hasDuplicates be false.

    // NOTE: This loop performs step 5, 6, and 8.
    for (auto const& parameter : formals) {
        if (parameter.default_value)
            m_has_parameter_expressions = true;

        parameter.binding.visit(
            [&](Identifier const& identifier) {
                if (m_parameter_names.set(identifier.string()) != AK::HashSetResult::InsertedNewEntry)
                    m_has_duplicates = true;
            },
            [&](NonnullRefPtr<BindingPattern const> const& pattern) {
                if (pattern->contains_expression())
                    m_has_parameter_expressions = true;

                // NOTE: Nothing in the callback throws an exception.
                MUST(pattern->for_each_bound_identifier([&](auto& identifier) {
                    if (m_parameter_names.set(identifier.string()) != AK::HashSetResult::InsertedNewEntry)
                        m_has_duplicates = true;
                }));
            });
    }

    // 15. Let argumentsObjectNeeded be true.
    m_arguments_object_needed = m_might_need_arguments_object;

    // 16. If func.[[ThisMode]] is lexical, then
    if (this_mode() == ThisMode::Lexical) {
        // a. NOTE: Arrow functions never have an arguments object.
        // b. Set argumentsObjectNeeded to false.
        m_arguments_object_needed = false;
    }
    // 17. Else if parameterNames contains "arguments", then
    else if (m_parameter_names.contains(vm().names.arguments.as_string())) {
        // a. Set argumentsObjectNeeded to false.
        m_arguments_object_needed = false;
    }

    HashTable<DeprecatedFlyString> function_names;

    // 18. Else if hasParameterExpressions is false, then
    //     a. If functionNames contains "arguments" or lexicalNames contains "arguments", then
    //         i. Set argumentsObjectNeeded to false.
    // NOTE: The block below is a combination of step 14 and step 18.
    if (scope_body) {
        // NOTE: Nothing in the callback throws an exception.
        MUST(scope_body->for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) {
            if (function_names.set(function.name()) == AK::HashSetResult::InsertedNewEntry)
                m_functions_to_initialize.append(function);
        }));

        auto const& arguments_name = vm().names.arguments.as_string();

        if (!m_has_parameter_expressions && function_names.contains(arguments_name))
            m_arguments_object_needed = false;

        if (!m_has_parameter_expressions && m_arguments_object_needed) {
            // NOTE: Nothing in the callback throws an exception.
            MUST(scope_body->for_each_lexically_declared_identifier([&](auto const& identifier) {
                if (identifier.string() == arguments_name)
                    m_arguments_object_needed = false;
            }));
        }
    } else {
        m_arguments_object_needed = false;
    }

    size_t* environment_size = nullptr;

    size_t parameter_environment_bindings_count = 0;
    // 19. If strict is true or hasParameterExpressions is false, then
    if (m_strict || !m_has_parameter_expressions) {
        // a. NOTE: Only a single Environment Record is needed for the parameters, since calls to eval in strict mode code cannot create new bindings which are visible outside of the eval.
        // b. Let env be the LexicalEnvironment of calleeContext
        // NOTE: Here we are only interested in the size of the environment.
        environment_size = &m_function_environment_bindings_count;
    }
    // 20. Else,
    else {
        // a. NOTE: A separate Environment Record is needed to ensure that bindings created by direct eval calls in the formal parameter list are outside the environment where parameters are declared.
        // b. Let calleeEnv be the LexicalEnvironment of calleeContext.
        // c. Let env be NewDeclarativeEnvironment(calleeEnv).
        environment_size = &parameter_environment_bindings_count;
    }

    *environment_size += m_parameter_names.size();

    HashTable<DeprecatedFlyString> parameter_bindings;

    // 22. If argumentsObjectNeeded is true, then
    if (m_arguments_object_needed) {
        // f. Let parameterBindings be the list-concatenation of parameterNames and « "arguments" ».
        parameter_bindings = m_parameter_names;
        parameter_bindings.set(vm().names.arguments.as_string());

        (*environment_size)++;
    } else {
        parameter_bindings = m_parameter_names;
        // a. Let parameterBindings be parameterNames.
    }

    HashTable<DeprecatedFlyString> instantiated_var_names;

    size_t* var_environment_size = nullptr;

    // 27. If hasParameterExpressions is false, then
    if (!m_has_parameter_expressions) {
        // b. Let instantiatedVarNames be a copy of the List parameterBindings.
        instantiated_var_names = parameter_bindings;

        if (scope_body) {
            // c. For each element n of varNames, do
            MUST(scope_body->for_each_var_declared_identifier([&](auto const& id) {
                // i. If instantiatedVarNames does not contain n, then
                if (instantiated_var_names.set(id.string()) == AK::HashSetResult::InsertedNewEntry) {
                    // 1. Append n to instantiatedVarNames.
                    // Following steps will be executed in function_declaration_instantiation:
                    // 2. Perform ! env.CreateMutableBinding(n, false).
                    // 3. Perform ! env.InitializeBinding(n, undefined).
                    m_var_names_to_initialize_binding.append({
                        .identifier = id,
                        .parameter_binding = parameter_bindings.contains(id.string()),
                        .function_name = function_names.contains(id.string()),
                    });

                    if (!id.is_local())
                        (*environment_size)++;
                }
            }));
        }

        // d. Let varEnv be env
        var_environment_size = environment_size;
    } else {
        // a. NOTE: A separate Environment Record is needed to ensure that closures created by expressions in the formal parameter list do not have visibility of declarations in the function body.

        // b. Let varEnv be NewDeclarativeEnvironment(env).
        // NOTE: Here we are only interested in the size of the environment.
        var_environment_size = &m_var_environment_bindings_count;

        // 28. Else,
        // NOTE: Steps a, b, c and d are executed in function_declaration_instantiation.
        // e. For each element n of varNames, do
        if (scope_body) {
            MUST(scope_body->for_each_var_declared_identifier([&](auto const& id) {
                // 1. Append n to instantiatedVarNames.
                // Following steps will be executed in function_declaration_instantiation:
                // 2. Perform ! env.CreateMutableBinding(n, false).
                // 3. Perform ! env.InitializeBinding(n, undefined).
                if (instantiated_var_names.set(id.string()) == AK::HashSetResult::InsertedNewEntry) {
                    m_var_names_to_initialize_binding.append({
                        .identifier = id,
                        .parameter_binding = parameter_bindings.contains(id.string()),
                        .function_name = function_names.contains(id.string()),
                    });

                    if (!id.is_local())
                        (*var_environment_size)++;
                }
            }));
        }
    }

    // 29. NOTE: Annex B.3.2.1 adds additional steps at this point.
    // B.3.2.1 Changes to FunctionDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation
    if (!m_strict && scope_body) {
        MUST(scope_body->for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) {
            auto function_name = function_declaration.name();
            if (parameter_bindings.contains(function_name))
                return;

            if (!instantiated_var_names.contains(function_name) && function_name != vm().names.arguments.as_string()) {
                m_function_names_to_initialize_binding.append(function_name);
                instantiated_var_names.set(function_name);
                (*var_environment_size)++;
            }

            function_declaration.set_should_do_additional_annexB_steps();
        }));
    }

    size_t* lex_environment_size = nullptr;

    // 30. If strict is false, then
    if (!m_strict) {
        bool can_elide_declarative_environment = !m_contains_direct_call_to_eval && (!scope_body || !scope_body->has_lexical_declarations());
        if (can_elide_declarative_environment) {
            lex_environment_size = var_environment_size;
        } else {
            // a. Let lexEnv be NewDeclarativeEnvironment(varEnv).
            lex_environment_size = &m_lex_environment_bindings_count;
        }
    } else {
        // a. let lexEnv be varEnv.
        // NOTE: Here we are only interested in the size of the environment.
        lex_environment_size = var_environment_size;
    }

    if (scope_body) {
        MUST(scope_body->for_each_lexically_declared_identifier([&](auto const& id) {
            if (!id.is_local())
                (*lex_environment_size)++;
        }));
    }
}

void ECMAScriptFunctionObject::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);
    // Note: The ordering of these properties must be: length, name, prototype which is the order
    //       they are defined in the spec: https://tc39.es/ecma262/#sec-function-instances .
    //       This is observable through something like: https://tc39.es/ecma262/#sec-ordinaryownpropertykeys
    //       which must give the properties in chronological order which in this case is the order they
    //       are defined in the spec.

    MUST(define_property_or_throw(vm.names.length, { .value = Value(m_function_length), .writable = false, .enumerable = false, .configurable = true }));
    MUST(define_property_or_throw(vm.names.name, { .value = PrimitiveString::create(vm, m_name.is_null() ? "" : m_name), .writable = false, .enumerable = false, .configurable = true }));

    if (!m_is_arrow_function) {
        Object* prototype = nullptr;
        switch (m_kind) {
        case FunctionKind::Normal:
            prototype = vm.heap().allocate<Object>(realm, realm.intrinsics().new_ordinary_function_prototype_object_shape());
            MUST(prototype->define_property_or_throw(vm.names.constructor, { .value = this, .writable = true, .enumerable = false, .configurable = true }));
            break;
        case FunctionKind::Generator:
            // prototype is "g1.prototype" in figure-2 (https://tc39.es/ecma262/img/figure-2.png)
            prototype = Object::create(realm, realm.intrinsics().generator_function_prototype_prototype());
            break;
        case FunctionKind::Async:
            break;
        case FunctionKind::AsyncGenerator:
            prototype = Object::create(realm, realm.intrinsics().async_generator_function_prototype_prototype());
            break;
        }
        // 27.7.4 AsyncFunction Instances, https://tc39.es/ecma262/#sec-async-function-instances
        // AsyncFunction instances do not have a prototype property as they are not constructible.
        if (m_kind != FunctionKind::Async)
            define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    }
}

// 10.2.1 [[Call]] ( thisArgument, argumentsList ), https://tc39.es/ecma262/#sec-ecmascript-function-objects-call-thisargument-argumentslist
ThrowCompletionOr<Value> ECMAScriptFunctionObject::internal_call(Value this_argument, MarkedVector<Value> arguments_list)
{
    auto& vm = this->vm();

    // 1. Let callerContext be the running execution context.
    // NOTE: No-op, kept by the VM in its execution context stack.

    ExecutionContext callee_context(heap());

    callee_context.local_variables.resize(m_local_variables_names.size());

    // Non-standard
    callee_context.arguments.extend(move(arguments_list));
    callee_context.instruction_stream_iterator = vm.bytecode_interpreter().instruction_stream_iterator();

    // 2. Let calleeContext be PrepareForOrdinaryCall(F, undefined).
    // NOTE: We throw if the end of the native stack is reached, so unlike in the spec this _does_ need an exception check.
    TRY(prepare_for_ordinary_call(callee_context, nullptr));

    // 3. Assert: calleeContext is now the running execution context.
    VERIFY(&vm.running_execution_context() == &callee_context);

    // 4. If F.[[IsClassConstructor]] is true, then
    if (m_is_class_constructor) {
        // a. Let error be a newly created TypeError object.
        // b. NOTE: error is created in calleeContext with F's associated Realm Record.
        auto throw_completion = vm.throw_completion<TypeError>(ErrorType::ClassConstructorWithoutNew, m_name);

        // c. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
        vm.pop_execution_context();

        // d. Return ThrowCompletion(error).
        return throw_completion;
    }

    // 5. Perform OrdinaryCallBindThis(F, calleeContext, thisArgument).
    ordinary_call_bind_this(callee_context, this_argument);

    // 6. Let result be Completion(OrdinaryCallEvaluateBody(F, argumentsList)).
    auto result = ordinary_call_evaluate_body();

    // 7. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
    vm.pop_execution_context();

    // 8. If result.[[Type]] is return, return result.[[Value]].
    if (result.type() == Completion::Type::Return)
        return *result.value();

    // 9. ReturnIfAbrupt(result).
    if (result.is_abrupt()) {
        VERIFY(result.is_error());
        return result;
    }

    // 10. Return undefined.
    return js_undefined();
}

// 10.2.2 [[Construct]] ( argumentsList, newTarget ), https://tc39.es/ecma262/#sec-ecmascript-function-objects-construct-argumentslist-newtarget
ThrowCompletionOr<NonnullGCPtr<Object>> ECMAScriptFunctionObject::internal_construct(MarkedVector<Value> arguments_list, FunctionObject& new_target)
{
    auto& vm = this->vm();

    // 1. Let callerContext be the running execution context.
    // NOTE: No-op, kept by the VM in its execution context stack.

    // 2. Let kind be F.[[ConstructorKind]].
    auto kind = m_constructor_kind;

    GCPtr<Object> this_argument;

    // 3. If kind is base, then
    if (kind == ConstructorKind::Base) {
        // a. Let thisArgument be ? OrdinaryCreateFromConstructor(newTarget, "%Object.prototype%").
        this_argument = TRY(ordinary_create_from_constructor<Object>(vm, new_target, &Intrinsics::object_prototype, ConstructWithPrototypeTag::Tag));
    }

    ExecutionContext callee_context(heap());

    callee_context.local_variables.resize(m_local_variables_names.size());

    // Non-standard
    callee_context.arguments.extend(move(arguments_list));
    callee_context.instruction_stream_iterator = vm.bytecode_interpreter().instruction_stream_iterator();

    // 4. Let calleeContext be PrepareForOrdinaryCall(F, newTarget).
    // NOTE: We throw if the end of the native stack is reached, so unlike in the spec this _does_ need an exception check.
    TRY(prepare_for_ordinary_call(callee_context, &new_target));

    // 5. Assert: calleeContext is now the running execution context.
    VERIFY(&vm.running_execution_context() == &callee_context);

    // 6. If kind is base, then
    if (kind == ConstructorKind::Base) {
        // a. Perform OrdinaryCallBindThis(F, calleeContext, thisArgument).
        ordinary_call_bind_this(callee_context, this_argument);

        // b. Let initializeResult be Completion(InitializeInstanceElements(thisArgument, F)).
        auto initialize_result = this_argument->initialize_instance_elements(*this);

        // c. If initializeResult is an abrupt completion, then
        if (initialize_result.is_throw_completion()) {
            // i. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
            vm.pop_execution_context();

            // ii. Return ? initializeResult.
            return initialize_result.throw_completion();
        }
    }

    // 7. Let constructorEnv be the LexicalEnvironment of calleeContext.
    auto constructor_env = callee_context.lexical_environment;

    // 8. Let result be Completion(OrdinaryCallEvaluateBody(F, argumentsList)).
    auto result = ordinary_call_evaluate_body();

    // 9. Remove calleeContext from the execution context stack and restore callerContext as the running execution context.
    vm.pop_execution_context();

    // 10. If result.[[Type]] is return, then
    if (result.type() == Completion::Type::Return) {
        // FIXME: This is leftover from untangling the call/construct mess - doesn't belong here in any way, but removing it breaks derived classes.
        // Likely fixed by making ClassDefinitionEvaluation fully spec compliant.
        if (kind == ConstructorKind::Derived && result.value()->is_object()) {
            auto prototype = TRY(new_target.get(vm.names.prototype));
            if (prototype.is_object())
                TRY(result.value()->as_object().internal_set_prototype_of(&prototype.as_object()));
        }
        // EOF (End of FIXME)

        // a. If Type(result.[[Value]]) is Object, return result.[[Value]].
        if (result.value()->is_object())
            return result.value()->as_object();

        // b. If kind is base, return thisArgument.
        if (kind == ConstructorKind::Base)
            return *this_argument;

        // c. If result.[[Value]] is not undefined, throw a TypeError exception.
        if (!result.value()->is_undefined())
            return vm.throw_completion<TypeError>(ErrorType::DerivedConstructorReturningInvalidValue);
    }
    // 11. Else, ReturnIfAbrupt(result).
    else if (result.is_abrupt()) {
        VERIFY(result.is_error());
        return result;
    }

    // 12. Let thisBinding be ? constructorEnv.GetThisBinding().
    auto this_binding = TRY(constructor_env->get_this_binding(vm));

    // 13. Assert: Type(thisBinding) is Object.
    VERIFY(this_binding.is_object());

    // 14. Return thisBinding.
    return this_binding.as_object();
}

void ECMAScriptFunctionObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_environment);
    visitor.visit(m_private_environment);
    visitor.visit(m_realm);
    visitor.visit(m_home_object);

    for (auto& field : m_fields) {
        if (auto* property_key_ptr = field.name.get_pointer<PropertyKey>(); property_key_ptr && property_key_ptr->is_symbol())
            visitor.visit(property_key_ptr->as_symbol());
    }

    m_script_or_module.visit(
        [](Empty) {},
        [&](auto& script_or_module) {
            visitor.visit(script_or_module.ptr());
        });
}

// 10.2.7 MakeMethod ( F, homeObject ), https://tc39.es/ecma262/#sec-makemethod
void ECMAScriptFunctionObject::make_method(Object& home_object)
{
    // 1. Set F.[[HomeObject]] to homeObject.
    m_home_object = &home_object;

    // 2. Return unused.
}

// 10.2.11 FunctionDeclarationInstantiation ( func, argumentsList ), https://tc39.es/ecma262/#sec-functiondeclarationinstantiation
ThrowCompletionOr<void> ECMAScriptFunctionObject::function_declaration_instantiation()
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    // 1. Let calleeContext be the running execution context.
    auto& callee_context = vm.running_execution_context();

    // 2. Let code be func.[[ECMAScriptCode]].
    ScopeNode const* scope_body = nullptr;
    if (is<ScopeNode>(*m_ecmascript_code))
        scope_body = static_cast<ScopeNode const*>(m_ecmascript_code.ptr());

    // NOTE: Following steps were executed in ECMAScriptFunctionObject constructor.
    //       3. Let strict be func.[[Strict]].
    //       4. Let formals be func.[[FormalParameters]].
    //       5. Let parameterNames be the BoundNames of formals.
    //       6. If parameterNames has any duplicate entries, let hasDuplicates be true. Otherwise, let hasDuplicates be false.

    // 7. Let simpleParameterList be IsSimpleParameterList of formals.
    bool const simple_parameter_list = has_simple_parameter_list();

    // NOTE: Following steps were executed in ECMAScriptFunctionObject constructor.
    //       8. Let hasParameterExpressions be ContainsExpression of formals.
    //       9. Let varNames be the VarDeclaredNames of code.
    //       10. Let varDeclarations be the VarScopedDeclarations of code.
    //       11. Let lexicalNames be the LexicallyDeclaredNames of code.
    //       12. Let functionNames be a new empty List.
    //       13. Let functionsToInitialize be a new empty List.
    //       14. For each element d of varDeclarations, in reverse List order, do
    //       15. Let argumentsObjectNeeded be true.
    //       16. If func.[[ThisMode]] is lexical, then
    //       17. Else if parameterNames contains "arguments", then
    //       18. Else if hasParameterExpressions is false, then

    GCPtr<Environment> environment;

    // 19. If strict is true or hasParameterExpressions is false, then
    if (m_strict || !m_has_parameter_expressions) {
        // a. NOTE: Only a single Environment Record is needed for the parameters, since calls to eval in strict mode code cannot create new bindings which are visible outside of the eval.
        // b. Let env be the LexicalEnvironment of calleeContext.
        environment = callee_context.lexical_environment;
    }
    // 20. Else,
    else {
        // a. NOTE: A separate Environment Record is needed to ensure that bindings created by direct eval calls in the formal parameter list are outside the environment where parameters are declared.

        // b. Let calleeEnv be the LexicalEnvironment of calleeContext.
        auto callee_env = callee_context.lexical_environment;

        // c. Let env be NewDeclarativeEnvironment(calleeEnv).
        environment = new_declarative_environment(*callee_env);

        // d. Assert: The VariableEnvironment of calleeContext is calleeEnv.
        VERIFY(callee_context.variable_environment == callee_context.lexical_environment);

        // e. Set the LexicalEnvironment of calleeContext to env.
        callee_context.lexical_environment = environment;
    }

    // 21. For each String paramName of parameterNames, do
    for (auto const& parameter_name : m_parameter_names) {
        // a. Let alreadyDeclared be ! env.HasBinding(paramName).

        // b. NOTE: Early errors ensure that duplicate parameter names can only occur in non-strict functions that do not have parameter default values or rest parameters.

        // c. If alreadyDeclared is false, then
        // NOTE: alreadyDeclared is always false because we use hash table for parameterNames
        // i. Perform ! env.CreateMutableBinding(paramName, false).
        MUST(environment->create_mutable_binding(vm, parameter_name, false));

        // ii. If hasDuplicates is true, then
        if (m_has_duplicates) {
            // 1. Perform ! env.InitializeBinding(paramName, undefined).
            MUST(environment->initialize_binding(vm, parameter_name, js_undefined(), Environment::InitializeBindingHint::Normal));
        }
    }

    // 22. If argumentsObjectNeeded is true, then
    if (m_arguments_object_needed) {
        Object* arguments_object;

        // a. If strict is true or simpleParameterList is false, then
        if (m_strict || !simple_parameter_list) {
            // i. Let ao be CreateUnmappedArgumentsObject(argumentsList).
            arguments_object = create_unmapped_arguments_object(vm, vm.running_execution_context().arguments);
        }
        // b. Else,
        else {
            // i. NOTE: A mapped argument object is only provided for non-strict functions that don't have a rest parameter, any parameter default value initializers, or any destructured parameters.

            // ii. Let ao be CreateMappedArgumentsObject(func, formals, argumentsList, env).
            arguments_object = create_mapped_arguments_object(vm, *this, formal_parameters(), vm.running_execution_context().arguments, *environment);
        }

        // c. If strict is true, then
        if (m_strict) {
            // i. Perform ! env.CreateImmutableBinding("arguments", false).
            MUST(environment->create_immutable_binding(vm, vm.names.arguments.as_string(), false));

            // ii. NOTE: In strict mode code early errors prevent attempting to assign to this binding, so its mutability is not observable.
        }
        // b. Else,
        else {
            // i. Perform ! env.CreateMutableBinding("arguments", false).
            MUST(environment->create_mutable_binding(vm, vm.names.arguments.as_string(), false));
        }

        // c. Perform ! env.InitializeBinding("arguments", ao).
        MUST(environment->initialize_binding(vm, vm.names.arguments.as_string(), arguments_object, Environment::InitializeBindingHint::Normal));

        // f. Let parameterBindings be the list-concatenation of parameterNames and « "arguments" ».
    }
    // 23. Else,
    else {
        // a. Let parameterBindings be parameterNames.
    }

    // NOTE: We now treat parameterBindings as parameterNames.

    // 24. Let iteratorRecord be CreateListIteratorRecord(argumentsList).
    // 25. If hasDuplicates is true, then
    //     a. Perform ? IteratorBindingInitialization of formals with arguments iteratorRecord and undefined.
    // 26. Else,
    //     a. Perform ? IteratorBindingInitialization of formals with arguments iteratorRecord and env.
    // NOTE: The spec makes an iterator here to do IteratorBindingInitialization but we just do it manually
    auto& execution_context_arguments = vm.running_execution_context().arguments;

    size_t default_parameter_index = 0;
    for (size_t i = 0; i < m_formal_parameters.size(); ++i) {
        auto& parameter = m_formal_parameters[i];
        if (parameter.default_value)
            ++default_parameter_index;

        TRY(parameter.binding.visit(
            [&](auto const& param) -> ThrowCompletionOr<void> {
                Value argument_value;
                if (parameter.is_rest) {
                    auto array = MUST(Array::create(realm, 0));
                    for (size_t rest_index = i; rest_index < execution_context_arguments.size(); ++rest_index)
                        array->indexed_properties().append(execution_context_arguments[rest_index]);
                    argument_value = array;
                } else if (i < execution_context_arguments.size() && !execution_context_arguments[i].is_undefined()) {
                    argument_value = execution_context_arguments[i];
                } else if (parameter.default_value) {
                    auto value_and_frame = vm.bytecode_interpreter().run_and_return_frame(*m_default_parameter_bytecode_executables[default_parameter_index - 1], nullptr);
                    if (value_and_frame.value.is_error())
                        return value_and_frame.value.release_error();
                    // Resulting value is in the accumulator.
                    argument_value = value_and_frame.frame->registers.at(0);
                } else {
                    argument_value = js_undefined();
                }

                Environment* used_environment = m_has_duplicates ? nullptr : environment;

                if constexpr (IsSame<NonnullRefPtr<Identifier const> const&, decltype(param)>) {
                    if (param->is_local()) {
                        callee_context.local_variables[param->local_variable_index()] = argument_value;
                        return {};
                    }
                    Reference reference = TRY(vm.resolve_binding(param->string(), used_environment));
                    // Here the difference from hasDuplicates is important
                    if (m_has_duplicates)
                        return reference.put_value(vm, argument_value);
                    return reference.initialize_referenced_binding(vm, argument_value);
                }
                if constexpr (IsSame<NonnullRefPtr<BindingPattern const> const&, decltype(param)>) {
                    // Here the difference from hasDuplicates is important
                    return vm.binding_initialization(param, argument_value, used_environment);
                }
            }));
    }

    GCPtr<Environment> var_environment;

    // 27. If hasParameterExpressions is false, then
    if (!m_has_parameter_expressions) {
        // a. NOTE: Only a single Environment Record is needed for the parameters and top-level vars.

        // b. Let instantiatedVarNames be a copy of the List parameterBindings.
        // NOTE: Done in implementation of step 27.c.i.1 below

        if (scope_body) {
            // NOTE: Due to the use of MUST with `create_mutable_binding` and `initialize_binding` below,
            //       an exception should not result from `for_each_var_declared_name`.

            // c. For each element n of varNames, do
            for (auto const& variable_to_initialize : m_var_names_to_initialize_binding) {
                auto const& id = variable_to_initialize.identifier;
                // NOTE: Following steps were executed in ECMAScriptFunctionObject constructor.
                //       i. If instantiatedVarNames does not contain n, then
                //       1. Append n to instantiatedVarNames.
                if (id.is_local()) {
                    callee_context.local_variables[id.local_variable_index()] = js_undefined();
                } else {
                    // 2. Perform ! env.CreateMutableBinding(n, false).
                    // 3. Perform ! env.InitializeBinding(n, undefined).
                    MUST(environment->create_mutable_binding(vm, id.string(), false));
                    MUST(environment->initialize_binding(vm, id.string(), js_undefined(), Environment::InitializeBindingHint::Normal));
                }
            }
        }

        // d.Let varEnv be env
        var_environment = environment;
    }
    // 28. Else,
    else {
        // a. NOTE: A separate Environment Record is needed to ensure that closures created by expressions in the formal parameter list do not have visibility of declarations in the function body.

        // b. Let varEnv be NewDeclarativeEnvironment(env).
        var_environment = new_declarative_environment(*environment);
        static_cast<DeclarativeEnvironment*>(var_environment.ptr())->ensure_capacity(m_var_environment_bindings_count);

        // c. Set the VariableEnvironment of calleeContext to varEnv.
        callee_context.variable_environment = var_environment;

        // d. Let instantiatedVarNames be a new empty List.
        // NOTE: Already done above.

        if (scope_body) {
            // NOTE: Due to the use of MUST with `create_mutable_binding`, `get_binding_value` and `initialize_binding` below,
            //       an exception should not result from `for_each_var_declared_name`.

            // e. For each element n of varNames, do
            for (auto const& variable_to_initialize : m_var_names_to_initialize_binding) {
                auto const& id = variable_to_initialize.identifier;

                // NOTE: Following steps were executed in ECMAScriptFunctionObject constructor.
                //       i. If instantiatedVarNames does not contain n, then
                //       1. Append n to instantiatedVarNames.

                // 2. Perform ! varEnv.CreateMutableBinding(n, false).
                // NOTE: We ignore locals because they are stored in ExecutionContext instead of environment.
                if (!id.is_local())
                    MUST(var_environment->create_mutable_binding(vm, id.string(), false));

                Value initial_value;

                // 3. If parameterBindings does not contain n, or if functionNames contains n, then
                if (!variable_to_initialize.parameter_binding || variable_to_initialize.function_name) {
                    // a. Let initialValue be undefined.
                    initial_value = js_undefined();
                }
                // 4. Else,
                else {
                    // a. Let initialValue be ! env.GetBindingValue(n, false).
                    if (id.is_local()) {
                        initial_value = callee_context.local_variables[id.local_variable_index()];
                    } else {
                        initial_value = MUST(environment->get_binding_value(vm, id.string(), false));
                    }
                }

                // 5. Perform ! varEnv.InitializeBinding(n, initialValue).
                if (id.is_local()) {
                    // NOTE: Local variables are supported only in bytecode interpreter
                    callee_context.local_variables[id.local_variable_index()] = initial_value;
                } else {
                    MUST(var_environment->initialize_binding(vm, id.string(), initial_value, Environment::InitializeBindingHint::Normal));
                }

                // 6. NOTE: A var with the same name as a formal parameter initially has the same value as the corresponding initialized parameter.
            }
        }
    }

    // 29. NOTE: Annex B.3.2.1 adds additional steps at this point.
    // B.3.2.1 Changes to FunctionDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation
    if (!m_strict && scope_body) {
        // NOTE: Due to the use of MUST with `create_mutable_binding` and `initialize_binding` below,
        //       an exception should not result from `for_each_function_hoistable_with_annexB_extension`.
        for (auto const& function_name : m_function_names_to_initialize_binding) {
            MUST(var_environment->create_mutable_binding(vm, function_name, false));
            MUST(var_environment->initialize_binding(vm, function_name, js_undefined(), Environment::InitializeBindingHint::Normal));
        }
    }

    GCPtr<Environment> lex_environment;

    // 30. If strict is false, then
    if (!m_strict) {
        // Optimization: We avoid creating empty top-level declarative environments in non-strict mode, if both of these conditions are true:
        //               1. there is no direct call to eval() within this function
        //               2. there are no lexical declarations that would go into the environment
        bool can_elide_declarative_environment = !m_contains_direct_call_to_eval && (!scope_body || !scope_body->has_lexical_declarations());
        if (can_elide_declarative_environment) {
            lex_environment = var_environment;
        } else {
            // a. Let lexEnv be NewDeclarativeEnvironment(varEnv).
            // b. NOTE: Non-strict functions use a separate Environment Record for top-level lexical declarations so that a direct eval
            //          can determine whether any var scoped declarations introduced by the eval code conflict with pre-existing top-level
            //          lexically scoped declarations. This is not needed for strict functions because a strict direct eval always places
            //          all declarations into a new Environment Record.
            lex_environment = new_declarative_environment(*var_environment);
            static_cast<DeclarativeEnvironment*>(lex_environment.ptr())->ensure_capacity(m_lex_environment_bindings_count);
        }
    }
    // 31. Else,
    else {
        // a. let lexEnv be varEnv.
        lex_environment = var_environment;
    }

    // 32. Set the LexicalEnvironment of calleeContext to lexEnv.
    callee_context.lexical_environment = lex_environment;

    if (!scope_body)
        return {};

    // 33. Let lexDeclarations be the LexicallyScopedDeclarations of code.
    // 34. For each element d of lexDeclarations, do
    // NOTE: Due to the use of MUST in the callback, an exception should not result from `for_each_lexically_scoped_declaration`.
    MUST(scope_body->for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        // NOTE: Due to the use of MUST with `create_immutable_binding` and `create_mutable_binding` below,
        //       an exception should not result from `for_each_bound_name`.

        // a. NOTE: A lexically declared name cannot be the same as a function/generator declaration, formal parameter, or a var name. Lexically declared names are only instantiated here but not initialized.

        // b. For each element dn of the BoundNames of d, do
        MUST(declaration.for_each_bound_identifier([&](auto const& id) {
            if (id.is_local()) {
                // NOTE: Local variables are supported only in bytecode interpreter
                return;
            }

            // i. If IsConstantDeclaration of d is true, then
            if (declaration.is_constant_declaration()) {
                // 1. Perform ! lexEnv.CreateImmutableBinding(dn, true).
                MUST(lex_environment->create_immutable_binding(vm, id.string(), true));
            }
            // ii. Else,
            else {
                // 1. Perform ! lexEnv.CreateMutableBinding(dn, false).
                MUST(lex_environment->create_mutable_binding(vm, id.string(), false));
            }
        }));
    }));

    // 35. Let privateEnv be the PrivateEnvironment of calleeContext.
    auto private_environment = callee_context.private_environment;

    // 36. For each Parse Node f of functionsToInitialize, do
    for (auto& declaration : m_functions_to_initialize) {
        // a. Let fn be the sole element of the BoundNames of f.
        // b. Let fo be InstantiateFunctionObject of f with arguments lexEnv and privateEnv.
        auto function = ECMAScriptFunctionObject::create(realm, declaration.name(), declaration.source_text(), declaration.body(), declaration.parameters(), declaration.function_length(), declaration.local_variables_names(), lex_environment, private_environment, declaration.kind(), declaration.is_strict_mode(), declaration.might_need_arguments_object(), declaration.contains_direct_call_to_eval());

        // c. Perform ! varEnv.SetMutableBinding(fn, fo, false).
        if (declaration.name_identifier()->is_local()) {
            callee_context.local_variables[declaration.name_identifier()->local_variable_index()] = function;
        } else {
            MUST(var_environment->set_mutable_binding(vm, declaration.name(), function, false));
        }
    }

    if (is<DeclarativeEnvironment>(*lex_environment))
        static_cast<DeclarativeEnvironment*>(lex_environment.ptr())->shrink_to_fit();
    if (is<DeclarativeEnvironment>(*var_environment))
        static_cast<DeclarativeEnvironment*>(var_environment.ptr())->shrink_to_fit();

    // 37. Return unused.
    return {};
}

// 10.2.1.1 PrepareForOrdinaryCall ( F, newTarget ), https://tc39.es/ecma262/#sec-prepareforordinarycall
ThrowCompletionOr<void> ECMAScriptFunctionObject::prepare_for_ordinary_call(ExecutionContext& callee_context, Object* new_target)
{
    auto& vm = this->vm();

    // Non-standard
    callee_context.is_strict_mode = m_strict;

    // 1. Let callerContext be the running execution context.
    // 2. Let calleeContext be a new ECMAScript code execution context.

    // NOTE: In the specification, PrepareForOrdinaryCall "returns" a new callee execution context.
    // To avoid heap allocations, we put our ExecutionContext objects on the C++ stack instead.
    // Whoever calls us should put an ExecutionContext on their stack and pass that as the `callee_context`.

    // 3. Set the Function of calleeContext to F.
    callee_context.function = this;
    callee_context.function_name = m_name;

    // 4. Let calleeRealm be F.[[Realm]].
    auto callee_realm = m_realm;
    // NOTE: This non-standard fallback is needed until we can guarantee that literally
    // every function has a realm - especially in LibWeb that's sometimes not the case
    // when a function is created while no JS is running, as we currently need to rely on
    // that (:acid2:, I know - see set_event_handler_attribute() for an example).
    // If there's no 'current realm' either, we can't continue and crash.
    if (!callee_realm)
        callee_realm = vm.current_realm();
    VERIFY(callee_realm);

    // 5. Set the Realm of calleeContext to calleeRealm.
    callee_context.realm = callee_realm;

    // 6. Set the ScriptOrModule of calleeContext to F.[[ScriptOrModule]].
    callee_context.script_or_module = m_script_or_module;

    // 7. Let localEnv be NewFunctionEnvironment(F, newTarget).
    auto local_environment = new_function_environment(*this, new_target);
    local_environment->ensure_capacity(m_function_environment_bindings_count);

    // 8. Set the LexicalEnvironment of calleeContext to localEnv.
    callee_context.lexical_environment = local_environment;

    // 9. Set the VariableEnvironment of calleeContext to localEnv.
    callee_context.variable_environment = local_environment;

    // 10. Set the PrivateEnvironment of calleeContext to F.[[PrivateEnvironment]].
    callee_context.private_environment = m_private_environment;

    // 11. If callerContext is not already suspended, suspend callerContext.
    // FIXME: We don't have this concept yet.

    // 12. Push calleeContext onto the execution context stack; calleeContext is now the running execution context.
    TRY(vm.push_execution_context(callee_context, {}));

    // 13. NOTE: Any exception objects produced after this point are associated with calleeRealm.
    // 14. Return calleeContext.
    // NOTE: See the comment after step 2 above about how contexts are allocated on the C++ stack.
    return {};
}

// 10.2.1.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument ), https://tc39.es/ecma262/#sec-ordinarycallbindthis
void ECMAScriptFunctionObject::ordinary_call_bind_this(ExecutionContext& callee_context, Value this_argument)
{
    auto& vm = this->vm();

    // 1. Let thisMode be F.[[ThisMode]].
    auto this_mode = m_this_mode;

    // If thisMode is lexical, return unused.
    if (this_mode == ThisMode::Lexical)
        return;

    // 3. Let calleeRealm be F.[[Realm]].
    auto callee_realm = m_realm;
    // NOTE: This non-standard fallback is needed until we can guarantee that literally
    // every function has a realm - especially in LibWeb that's sometimes not the case
    // when a function is created while no JS is running, as we currently need to rely on
    // that (:acid2:, I know - see set_event_handler_attribute() for an example).
    // If there's no 'current realm' either, we can't continue and crash.
    if (!callee_realm)
        callee_realm = vm.current_realm();
    VERIFY(callee_realm);

    // 4. Let localEnv be the LexicalEnvironment of calleeContext.
    auto local_env = callee_context.lexical_environment;

    Value this_value;

    // 5. If thisMode is strict, let thisValue be thisArgument.
    if (this_mode == ThisMode::Strict) {
        this_value = this_argument;
    }
    // 6. Else,
    else {
        // a. If thisArgument is undefined or null, then
        if (this_argument.is_nullish()) {
            // i. Let globalEnv be calleeRealm.[[GlobalEnv]].
            // ii. Assert: globalEnv is a global Environment Record.
            auto& global_env = callee_realm->global_environment();

            // iii. Let thisValue be globalEnv.[[GlobalThisValue]].
            this_value = &global_env.global_this_value();
        }
        // b. Else,
        else {
            // i. Let thisValue be ! ToObject(thisArgument).
            this_value = MUST(this_argument.to_object(vm));

            // ii. NOTE: ToObject produces wrapper objects using calleeRealm.
            VERIFY(vm.current_realm() == callee_realm);
        }
    }

    // 7. Assert: localEnv is a function Environment Record.
    // 8. Assert: The next step never returns an abrupt completion because localEnv.[[ThisBindingStatus]] is not initialized.
    // 9. Perform ! localEnv.BindThisValue(thisValue).
    MUST(verify_cast<FunctionEnvironment>(*local_env).bind_this_value(vm, this_value));

    // 10. Return unused.
}

// 27.7.5.1 AsyncFunctionStart ( promiseCapability, asyncFunctionBody ), https://tc39.es/ecma262/#sec-async-functions-abstract-operations-async-function-start
template<typename T>
void async_function_start(VM& vm, PromiseCapability const& promise_capability, T const& async_function_body)
{
    // 1. Let runningContext be the running execution context.
    auto& running_context = vm.running_execution_context();

    // 2. Let asyncContext be a copy of runningContext.
    auto async_context = running_context.copy();

    // 3. NOTE: Copying the execution state is required for AsyncBlockStart to resume its execution. It is ill-defined to resume a currently executing context.

    // 4. Perform AsyncBlockStart(promiseCapability, asyncFunctionBody, asyncContext).
    async_block_start(vm, async_function_body, promise_capability, async_context);

    // 5. Return unused.
}

// 27.7.5.2 AsyncBlockStart ( promiseCapability, asyncBody, asyncContext ), https://tc39.es/ecma262/#sec-asyncblockstart
// 12.7.1.1 AsyncBlockStart ( promiseCapability, asyncBody, asyncContext ), https://tc39.es/proposal-explicit-resource-management/#sec-asyncblockstart
// 1.2.1.1 AsyncBlockStart ( promiseCapability, asyncBody, asyncContext ), https://tc39.es/proposal-array-from-async/#sec-asyncblockstart
template<typename T>
void async_block_start(VM& vm, T const& async_body, PromiseCapability const& promise_capability, ExecutionContext& async_context)
{
    // NOTE: This function is a combination between two proposals, so does not exactly match spec steps of either.

    auto& realm = *vm.current_realm();

    // 1. Assert: promiseCapability is a PromiseCapability Record.

    // 2. Let runningContext be the running execution context.
    auto& running_context = vm.running_execution_context();

    // 3. Set the code evaluation state of asyncContext such that when evaluation is resumed for that execution context the following steps will be performed:
    auto execution_steps = NativeFunction::create(realm, "", [&async_body, &promise_capability, &async_context](auto& vm) -> ThrowCompletionOr<Value> {
        Completion result;

        // a. If asyncBody is a Parse Node, then
        if constexpr (!IsCallableWithArguments<T, Completion>) {
            // a. Let result be the result of evaluating asyncBody.
            // FIXME: Cache this executable somewhere.
            auto maybe_executable = Bytecode::compile(vm, async_body, FunctionKind::Async, "AsyncBlockStart"sv);
            if (maybe_executable.is_error())
                result = maybe_executable.release_error();
            else
                result = vm.bytecode_interpreter().run_and_return_frame(*maybe_executable.value(), nullptr).value;
        }
        // b. Else,
        else {
            // i. Assert: asyncBody is an Abstract Closure with no parameters.
            static_assert(IsCallableWithArguments<T, Completion>);

            // ii. Let result be asyncBody().
            result = async_body();
        }

        // c. Assert: If we return here, the async function either threw an exception or performed an implicit or explicit return; all awaiting is done.

        // d. Remove asyncContext from the execution context stack and restore the execution context that is at the top of the execution context stack as the running execution context.
        vm.pop_execution_context();

        // NOTE: This does not work for Array.fromAsync, likely due to conflicts between that proposal and Explicit Resource Management proposal.
        if constexpr (!IsCallableWithArguments<T, Completion>) {
            // e. Let env be asyncContext's LexicalEnvironment.
            auto env = async_context.lexical_environment;

            // f. Set result to DisposeResources(env, result).
            result = dispose_resources(vm, verify_cast<DeclarativeEnvironment>(env.ptr()), result);
        } else {
            (void)async_context;
        }

        // g. If result.[[Type]] is normal, then
        if (result.type() == Completion::Type::Normal) {
            // i. Perform ! Call(promiseCapability.[[Resolve]], undefined, « undefined »).
            MUST(call(vm, *promise_capability.resolve(), js_undefined(), js_undefined()));
        }
        // h. Else if result.[[Type]] is return, then
        else if (result.type() == Completion::Type::Return) {
            // i. Perform ! Call(promiseCapability.[[Resolve]], undefined, « result.[[Value]] »).
            MUST(call(vm, *promise_capability.resolve(), js_undefined(), *result.value()));
        }
        // i. Else,
        else {
            // i. Assert: result.[[Type]] is throw.
            VERIFY(result.type() == Completion::Type::Throw);

            // ii. Perform ! Call(promiseCapability.[[Reject]], undefined, « result.[[Value]] »).
            MUST(call(vm, *promise_capability.reject(), js_undefined(), *result.value()));
        }
        // j. Return unused.
        // NOTE: We don't support returning an empty/optional/unused value here.
        return js_undefined();
    });

    // 4. Push asyncContext onto the execution context stack; asyncContext is now the running execution context.
    auto push_result = vm.push_execution_context(async_context, {});
    if (push_result.is_error())
        return;

    // 5. Resume the suspended evaluation of asyncContext. Let result be the value returned by the resumed computation.
    auto result = call(vm, *execution_steps, async_context.this_value.is_empty() ? js_undefined() : async_context.this_value);

    // 6. Assert: When we return here, asyncContext has already been removed from the execution context stack and runningContext is the currently running execution context.
    VERIFY(&vm.running_execution_context() == &running_context);

    // 7. Assert: result is a normal completion with a value of unused. The possible sources of this value are Await or, if the async function doesn't await anything, step 3.g above.
    VERIFY(result.has_value() && result.value().is_undefined());

    // 8. Return unused.
}

template void async_block_start(VM&, NonnullRefPtr<Statement const> const& async_body, PromiseCapability const&, ExecutionContext&);
template void async_function_start(VM&, PromiseCapability const&, NonnullRefPtr<Statement const> const& async_function_body);

template void async_block_start(VM&, SafeFunction<Completion()> const& async_body, PromiseCapability const&, ExecutionContext&);
template void async_function_start(VM&, PromiseCapability const&, SafeFunction<Completion()> const& async_function_body);

// 10.2.1.4 OrdinaryCallEvaluateBody ( F, argumentsList ), https://tc39.es/ecma262/#sec-ordinarycallevaluatebody
// 15.8.4 Runtime Semantics: EvaluateAsyncFunctionBody, https://tc39.es/ecma262/#sec-runtime-semantics-evaluatefunctionbody
Completion ECMAScriptFunctionObject::ordinary_call_evaluate_body()
{
    auto& vm = this->vm();
    auto& realm = *vm.current_realm();

    // NOTE: There's a subtle ordering issue here:
    //       - We have to compile the default parameter values before instantiating the function.
    //       - We have to instantiate the function before compiling the function body.
    //       This is why FunctionDeclarationInstantiation is invoked in the middle.
    //       The issue is that FunctionDeclarationInstantiation may mark certain functions as hoisted
    //       per Annex B. This affects code generation for FunctionDeclaration nodes.

    if (!m_bytecode_executable) {
        size_t default_parameter_index = 0;
        for (auto& parameter : m_formal_parameters) {
            if (!parameter.default_value)
                continue;
            auto executable = TRY(Bytecode::compile(vm, *parameter.default_value, FunctionKind::Normal, DeprecatedString::formatted("default parameter #{} for {}", default_parameter_index, m_name)));
            m_default_parameter_bytecode_executables.append(move(executable));
        }
    }

    auto declaration_result = function_declaration_instantiation();

    if (m_kind == FunctionKind::Normal || m_kind == FunctionKind::Generator || m_kind == FunctionKind::AsyncGenerator) {
        if (declaration_result.is_error())
            return declaration_result.release_error();
    }

    if (!m_bytecode_executable)
        m_bytecode_executable = TRY(Bytecode::compile(vm, *m_ecmascript_code, m_kind, m_name));

    if (m_kind == FunctionKind::Async) {
        if (declaration_result.is_throw_completion()) {
            auto promise_capability = MUST(new_promise_capability(vm, realm.intrinsics().promise_constructor()));
            MUST(call(vm, *promise_capability->reject(), js_undefined(), *declaration_result.throw_completion().value()));
            return Completion { Completion::Type::Return, promise_capability->promise(), {} };
        }
    }

    auto result_and_frame = vm.bytecode_interpreter().run_and_return_frame(*m_bytecode_executable, nullptr);

    VERIFY(result_and_frame.frame != nullptr);
    if (result_and_frame.value.is_error())
        return result_and_frame.value.release_error();

    auto result = result_and_frame.value.release_value();

    // NOTE: Running the bytecode should eventually return a completion.
    // Until it does, we assume "return" and include the undefined fallback from the call site.
    if (m_kind == FunctionKind::Normal)
        return { Completion::Type::Return, result.value_or(js_undefined()), {} };

    if (m_kind == FunctionKind::AsyncGenerator) {
        auto async_generator_object = TRY(AsyncGenerator::create(realm, result, this, vm.running_execution_context().copy(), move(*result_and_frame.frame)));
        return { Completion::Type::Return, async_generator_object, {} };
    }

    auto generator_object = TRY(GeneratorObject::create(realm, result, this, vm.running_execution_context().copy(), move(*result_and_frame.frame)));

    // NOTE: Async functions are entirely transformed to generator functions, and wrapped in a custom driver that returns a promise
    //       See AwaitExpression::generate_bytecode() for the transformation.
    if (m_kind == FunctionKind::Async)
        return { Completion::Type::Return, AsyncFunctionDriverWrapper::create(realm, generator_object), {} };

    VERIFY(m_kind == FunctionKind::Generator);
    return { Completion::Type::Return, generator_object, {} };
}

void ECMAScriptFunctionObject::set_name(DeprecatedFlyString const& name)
{
    VERIFY(!name.is_null());
    auto& vm = this->vm();
    m_name = name;
    MUST(define_property_or_throw(vm.names.name, { .value = PrimitiveString::create(vm, m_name), .writable = false, .enumerable = false, .configurable = true }));
}
}
