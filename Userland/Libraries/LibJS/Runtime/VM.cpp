/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibFileSystem/FileSystem.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/SourceTextModule.h>
#include <LibJS/SyntheticModule.h>

namespace JS {

ErrorOr<NonnullRefPtr<VM>> VM::create(OwnPtr<CustomData> custom_data)
{
    ErrorMessages error_messages {};
    error_messages[to_underlying(ErrorMessage::OutOfMemory)] = TRY(String::from_utf8(ErrorType::OutOfMemory.message()));

    auto vm = adopt_ref(*new VM(move(custom_data), move(error_messages)));

    WellKnownSymbols well_known_symbols {
#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol::create(*vm, "Symbol." #SymbolName##_string, false),
        JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
    };

    vm->set_well_known_symbols(move(well_known_symbols));
    return vm;
}

template<u32... code_points>
static constexpr auto make_single_ascii_character_strings(IndexSequence<code_points...>)
{
    return AK::Array { (String::from_code_point(code_points))... };
}

static constexpr auto single_ascii_character_strings = make_single_ascii_character_strings(MakeIndexSequence<128>());

VM::VM(OwnPtr<CustomData> custom_data, ErrorMessages error_messages)
    : m_heap(*this)
    , m_error_messages(move(error_messages))
    , m_custom_data(move(custom_data))
{
    m_bytecode_interpreter = make<Bytecode::Interpreter>(*this);

    m_empty_string = m_heap.allocate_without_realm<PrimitiveString>(String {});

    for (size_t i = 0; i < single_ascii_character_strings.size(); ++i)
        m_single_ascii_character_strings[i] = m_heap.allocate_without_realm<PrimitiveString>(single_ascii_character_strings[i]);

    // Default hook implementations. These can be overridden by the host, for example, LibWeb overrides the default hooks to place promise jobs on the microtask queue.
    host_promise_rejection_tracker = [this](Promise& promise, Promise::RejectionOperation operation) {
        promise_rejection_tracker(promise, operation);
    };

    host_call_job_callback = [this](JobCallback& job_callback, Value this_value, MarkedVector<Value> arguments) {
        return call_job_callback(*this, job_callback, this_value, move(arguments));
    };

    host_enqueue_finalization_registry_cleanup_job = [this](FinalizationRegistry& finalization_registry) {
        enqueue_finalization_registry_cleanup_job(finalization_registry);
    };

    host_enqueue_promise_job = [this](Function<ThrowCompletionOr<Value>()> job, Realm* realm) {
        enqueue_promise_job(move(job), realm);
    };

    host_make_job_callback = [](FunctionObject& function_object) {
        return make_job_callback(function_object);
    };

    host_resolve_imported_module = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier) {
        return resolve_imported_module(move(referencing_script_or_module), specifier);
    };

    host_import_module_dynamically = [&](ScriptOrModule, ModuleRequest const&, PromiseCapability const& promise_capability) -> ThrowCompletionOr<void> {
        // By default, we throw on dynamic imports this is to prevent arbitrary file access by scripts.
        VERIFY(current_realm());
        auto& realm = *current_realm();
        auto promise = Promise::create(realm);

        // If you are here because you want to enable dynamic module importing make sure it won't be a security problem
        // by checking the default implementation of HostImportModuleDynamically and creating your own hook or calling
        // vm.enable_default_host_import_module_dynamically_hook().
        promise->reject(MUST_OR_THROW_OOM(Error::create(realm, ErrorType::DynamicImportNotAllowed.message())));

        promise->perform_then(
            NativeFunction::create(realm, "", [](auto&) -> ThrowCompletionOr<Value> {
                VERIFY_NOT_REACHED();
            }),
            NativeFunction::create(realm, "", [&promise_capability](auto& vm) -> ThrowCompletionOr<Value> {
                auto error = vm.argument(0);

                // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « error »).
                MUST(call(vm, *promise_capability.reject(), js_undefined(), error));

                // b. Return undefined.
                return js_undefined();
            }),
            {});

        return {};
    };

    host_finish_dynamic_import = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability const& promise_capability, Promise* promise) {
        return finish_dynamic_import(move(referencing_script_or_module), specifier, promise_capability, promise);
    };

    host_get_import_meta_properties = [&](SourceTextModule const&) -> HashMap<PropertyKey, Value> {
        return {};
    };

    host_finalize_import_meta = [&](Object*, SourceTextModule const&) {
    };

    host_get_supported_import_assertions = [&] {
        return Vector<DeprecatedString> { "type" };
    };

    // 19.2.1.2 HostEnsureCanCompileStrings ( callerRealm, calleeRealm ), https://tc39.es/ecma262/#sec-hostensurecancompilestrings
    host_ensure_can_compile_strings = [](Realm&) -> ThrowCompletionOr<void> {
        // The host-defined abstract operation HostEnsureCanCompileStrings takes argument calleeRealm (a Realm Record)
        // and returns either a normal completion containing unused or a throw completion.
        // It allows host environments to block certain ECMAScript functions which allow developers to compile strings into ECMAScript code.
        // An implementation of HostEnsureCanCompileStrings must conform to the following requirements:
        //   - If the returned Completion Record is a normal completion, it must be a normal completion containing unused.
        // The default implementation of HostEnsureCanCompileStrings is to return NormalCompletion(unused).
        return {};
    };

    host_ensure_can_add_private_element = [](Object&) -> ThrowCompletionOr<void> {
        // The host-defined abstract operation HostEnsureCanAddPrivateElement takes argument O (an Object)
        // and returns either a normal completion containing unused or a throw completion.
        // It allows host environments to prevent the addition of private elements to particular host-defined exotic objects.
        // An implementation of HostEnsureCanAddPrivateElement must conform to the following requirements:
        // - If O is not a host-defined exotic object, this abstract operation must return NormalCompletion(unused) and perform no other steps.
        // - Any two calls of this abstract operation with the same argument must return the same kind of Completion Record.
        // The default implementation of HostEnsureCanAddPrivateElement is to return NormalCompletion(unused).
        return {};

        // This abstract operation is only invoked by ECMAScript hosts that are web browsers.
        // NOTE: Since LibJS has no way of knowing whether the current environment is a browser we always
        //       call HostEnsureCanAddPrivateElement when needed.
    };
}

VM::~VM() = default;

String const& VM::error_message(ErrorMessage type) const
{
    VERIFY(type < ErrorMessage::__Count);

    auto const& message = m_error_messages[to_underlying(type)];
    VERIFY(!message.is_empty());

    return message;
}

void VM::enable_default_host_import_module_dynamically_hook()
{
    host_import_module_dynamically = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability const& promise_capability) {
        return import_module_dynamically(move(referencing_script_or_module), specifier, promise_capability);
    };
}

Interpreter& VM::interpreter()
{
    VERIFY(!m_interpreters.is_empty());
    return *m_interpreters.last();
}

Interpreter* VM::interpreter_if_exists()
{
    if (m_interpreters.is_empty())
        return nullptr;
    return m_interpreters.last();
}

Bytecode::Interpreter& VM::bytecode_interpreter()
{
    return *m_bytecode_interpreter;
}

Bytecode::Interpreter* VM::bytecode_interpreter_if_exists()
{
    return m_bytecode_interpreter;
}

void VM::push_interpreter(Interpreter& interpreter)
{
    m_interpreters.append(&interpreter);
}

void VM::pop_interpreter(Interpreter& interpreter)
{
    VERIFY(!m_interpreters.is_empty());
    auto* popped_interpreter = m_interpreters.take_last();
    VERIFY(popped_interpreter == &interpreter);
}

VM::InterpreterExecutionScope::InterpreterExecutionScope(Interpreter& interpreter)
    : m_interpreter(interpreter)
{
    m_interpreter.vm().push_interpreter(m_interpreter);
}

VM::InterpreterExecutionScope::~InterpreterExecutionScope()
{
    m_interpreter.vm().pop_interpreter(m_interpreter);
}

void VM::gather_roots(HashTable<Cell*>& roots)
{
    roots.set(m_empty_string);
    for (auto string : m_single_ascii_character_strings)
        roots.set(string);

    auto gather_roots_from_execution_context_stack = [&roots](Vector<ExecutionContext*> const& stack) {
        for (auto& execution_context : stack) {
            if (execution_context->this_value.is_cell())
                roots.set(&execution_context->this_value.as_cell());
            for (auto& argument : execution_context->arguments) {
                if (argument.is_cell())
                    roots.set(&argument.as_cell());
            }
            roots.set(execution_context->lexical_environment);
            roots.set(execution_context->variable_environment);
            roots.set(execution_context->private_environment);
            if (auto context_owner = execution_context->context_owner)
                roots.set(context_owner);
            execution_context->script_or_module.visit(
                [](Empty) {},
                [&](auto& script_or_module) {
                    roots.set(script_or_module.ptr());
                });
        }
    };

    gather_roots_from_execution_context_stack(m_execution_context_stack);
    for (auto& saved_stack : m_saved_execution_context_stacks)
        gather_roots_from_execution_context_stack(saved_stack);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    roots.set(m_well_known_symbols.snake_name);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    for (auto& symbol : m_global_symbol_registry)
        roots.set(symbol.value);

    for (auto finalization_registry : m_finalization_registry_cleanup_jobs)
        roots.set(finalization_registry);
}

ThrowCompletionOr<Value> VM::named_evaluation_if_anonymous_function(ASTNode const& expression, DeprecatedFlyString const& name)
{
    // 8.3.3 Static Semantics: IsAnonymousFunctionDefinition ( expr ), https://tc39.es/ecma262/#sec-isanonymousfunctiondefinition
    // And 8.3.5 Runtime Semantics: NamedEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-namedevaluation
    if (is<FunctionExpression>(expression)) {
        auto& function = static_cast<FunctionExpression const&>(expression);
        if (!function.has_name()) {
            return function.instantiate_ordinary_function_expression(*this, name);
        }
    } else if (is<ClassExpression>(expression)) {
        auto& class_expression = static_cast<ClassExpression const&>(expression);
        if (!class_expression.has_name()) {
            return TRY(class_expression.class_definition_evaluation(*this, {}, name));
        }
    }

    return execute_ast_node(expression);
}

// 13.15.5.2 Runtime Semantics: DestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-destructuringassignmentevaluation
ThrowCompletionOr<void> VM::destructuring_assignment_evaluation(NonnullRefPtr<BindingPattern const> const& target, Value value)
{
    // Note: DestructuringAssignmentEvaluation is just like BindingInitialization without an environment
    //       And it allows member expressions. We thus trust the parser to disallow member expressions
    //       in any non assignment binding and just call BindingInitialization with a nullptr environment
    return binding_initialization(target, value, nullptr);
}

// 8.5.2 Runtime Semantics: BindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-bindinginitialization
ThrowCompletionOr<void> VM::binding_initialization(DeprecatedFlyString const& target, Value value, Environment* environment)
{
    // 1. Let name be StringValue of Identifier.
    // 2. Return ? InitializeBoundName(name, value, environment).
    return initialize_bound_name(*this, target, value, environment);
}

// 8.5.2 Runtime Semantics: BindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-bindinginitialization
ThrowCompletionOr<void> VM::binding_initialization(NonnullRefPtr<BindingPattern const> const& target, Value value, Environment* environment)
{
    auto& vm = *this;

    // BindingPattern : ObjectBindingPattern
    if (target->kind == BindingPattern::Kind::Object) {
        // 1. Perform ? RequireObjectCoercible(value).
        TRY(require_object_coercible(vm, value));

        // 2. Return ? BindingInitialization of ObjectBindingPattern with arguments value and environment.

        // BindingInitialization of ObjectBindingPattern
        // 1. Perform ? PropertyBindingInitialization of BindingPropertyList with arguments value and environment.
        TRY(property_binding_initialization(*target, value, environment));

        // 2. Return unused.
        return {};
    }
    // BindingPattern : ArrayBindingPattern
    else {
        // 1. Let iteratorRecord be ? GetIterator(value, sync).
        auto iterator_record = TRY(get_iterator(vm, value, IteratorHint::Sync));

        // 2. Let result be Completion(IteratorBindingInitialization of ArrayBindingPattern with arguments iteratorRecord and environment).
        auto result = iterator_binding_initialization(*target, iterator_record, environment);

        // 3. If iteratorRecord.[[Done]] is false, return ? IteratorClose(iteratorRecord, result).
        if (!iterator_record.done) {
            // iterator_close() always returns a Completion, which ThrowCompletionOr will interpret as a throw
            // completion. So only return the result of iterator_close() if it is indeed a throw completion.
            auto completion = result.is_throw_completion() ? result.release_error() : normal_completion({});
            if (completion = iterator_close(vm, iterator_record, move(completion)); completion.is_error())
                return completion.release_error();
        }

        // 4. Return ? result.
        return result;
    }
}

ThrowCompletionOr<Value> VM::execute_ast_node(ASTNode const& node)
{
    if (auto* bytecode_interpreter = bytecode_interpreter_if_exists()) {
        auto executable = TRY(Bytecode::compile(*this, node, FunctionKind::Normal, ""sv));
        auto result_or_error = bytecode_interpreter->run_and_return_frame(*current_realm(), *executable, nullptr);
        if (result_or_error.value.is_error())
            return result_or_error.value.release_error();
        return result_or_error.frame->registers[0];
    }

    return TRY(node.execute(interpreter())).value();
}

// 13.15.5.3 Runtime Semantics: PropertyDestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-propertydestructuringassignmentevaluation
// 14.3.3.1 Runtime Semantics: PropertyBindingInitialization, https://tc39.es/ecma262/#sec-destructuring-binding-patterns-runtime-semantics-propertybindinginitialization
ThrowCompletionOr<void> VM::property_binding_initialization(BindingPattern const& binding, Value value, Environment* environment)
{
    auto& vm = *this;
    auto& realm = *vm.current_realm();

    auto object = TRY(value.to_object(vm));

    HashTable<PropertyKey> seen_names;
    for (auto& property : binding.entries) {

        VERIFY(!property.is_elision());

        if (property.is_rest) {
            Reference assignment_target;
            if (auto identifier_ptr = property.name.get_pointer<NonnullRefPtr<Identifier const>>()) {
                assignment_target = TRY(resolve_binding((*identifier_ptr)->string(), environment));
            } else if (auto member_ptr = property.alias.get_pointer<NonnullRefPtr<MemberExpression const>>()) {
                assignment_target = TRY((*member_ptr)->to_reference(interpreter()));
            } else {
                VERIFY_NOT_REACHED();
            }

            auto rest_object = Object::create(realm, realm.intrinsics().object_prototype());
            VERIFY(rest_object);

            TRY(rest_object->copy_data_properties(vm, object, seen_names));
            if (!environment)
                return assignment_target.put_value(vm, rest_object);
            else
                return assignment_target.initialize_referenced_binding(vm, rest_object);
        }

        auto name = TRY(property.name.visit(
            [&](Empty) -> ThrowCompletionOr<PropertyKey> { VERIFY_NOT_REACHED(); },
            [&](NonnullRefPtr<Identifier const> const& identifier) -> ThrowCompletionOr<PropertyKey> {
                return identifier->string();
            },
            [&](NonnullRefPtr<Expression const> const& expression) -> ThrowCompletionOr<PropertyKey> {
                auto result = TRY(execute_ast_node(*expression));
                return result.to_property_key(vm);
            }));

        seen_names.set(name);

        if (property.name.has<NonnullRefPtr<Identifier const>>() && property.alias.has<Empty>()) {
            // FIXME: this branch and not taking this have a lot in common we might want to unify it more (like it was before).
            auto& identifier = *property.name.get<NonnullRefPtr<Identifier const>>();
            auto reference = TRY(resolve_binding(identifier.string(), environment));

            auto value_to_assign = TRY(object->get(name));
            if (property.initializer && value_to_assign.is_undefined()) {
                value_to_assign = TRY(named_evaluation_if_anonymous_function(*property.initializer, identifier.string()));
            }

            if (!environment)
                TRY(reference.put_value(vm, value_to_assign));
            else
                TRY(reference.initialize_referenced_binding(vm, value_to_assign));
            continue;
        }

        auto reference_to_assign_to = TRY(property.alias.visit(
            [&](Empty) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<Identifier const> const& identifier) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(resolve_binding(identifier->string(), environment));
            },
            [&](NonnullRefPtr<BindingPattern const> const&) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<MemberExpression const> const& member_expression) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(member_expression->to_reference(interpreter()));
            }));

        auto value_to_assign = TRY(object->get(name));
        if (property.initializer && value_to_assign.is_undefined()) {
            if (auto* identifier_ptr = property.alias.get_pointer<NonnullRefPtr<Identifier const>>())
                value_to_assign = TRY(named_evaluation_if_anonymous_function(*property.initializer, (*identifier_ptr)->string()));
            else
                value_to_assign = TRY(execute_ast_node(*property.initializer));
        }

        if (auto* binding_ptr = property.alias.get_pointer<NonnullRefPtr<BindingPattern const>>()) {
            TRY(binding_initialization(*binding_ptr, value_to_assign, environment));
        } else {
            VERIFY(reference_to_assign_to.has_value());
            if (!environment)
                TRY(reference_to_assign_to->put_value(vm, value_to_assign));
            else
                TRY(reference_to_assign_to->initialize_referenced_binding(vm, value_to_assign));
        }
    }

    return {};
}

// 13.15.5.5 Runtime Semantics: IteratorDestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-iteratordestructuringassignmentevaluation
// 8.5.3 Runtime Semantics: IteratorBindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-iteratorbindinginitialization
ThrowCompletionOr<void> VM::iterator_binding_initialization(BindingPattern const& binding, IteratorRecord& iterator_record, Environment* environment)
{
    auto& vm = *this;
    auto& realm = *vm.current_realm();

    // FIXME: this method is nearly identical to destructuring assignment!
    for (size_t i = 0; i < binding.entries.size(); i++) {
        auto& entry = binding.entries[i];
        Value value;

        auto assignment_target = TRY(entry.alias.visit(
            [&](Empty) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<Identifier const> const& identifier) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(resolve_binding(identifier->string(), environment));
            },
            [&](NonnullRefPtr<BindingPattern const> const&) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<MemberExpression const> const& member_expression) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(member_expression->to_reference(interpreter()));
            }));

        // BindingRestElement : ... BindingIdentifier
        // BindingRestElement : ... BindingPattern
        if (entry.is_rest) {
            VERIFY(i == binding.entries.size() - 1);

            // 2. Let A be ! ArrayCreate(0).
            auto array = MUST(Array::create(realm, 0));

            // 3. Let n be 0.
            // 4. Repeat,
            while (true) {
                ThrowCompletionOr<GCPtr<Object>> next { nullptr };

                // a. If iteratorRecord.[[Done]] is false, then
                if (!iterator_record.done) {
                    // i. Let next be Completion(IteratorStep(iteratorRecord)).
                    next = iterator_step(vm, iterator_record);

                    // ii. If next is an abrupt completion, set iteratorRecord.[[Done]] to true.
                    // iii. ReturnIfAbrupt(next).
                    if (next.is_error()) {
                        iterator_record.done = true;
                        return next.release_error();
                    }

                    // iv. If next is false, set iteratorRecord.[[Done]] to true.
                    if (!next.value())
                        iterator_record.done = true;
                }

                // b. If iteratorRecord.[[Done]] is true, then
                if (iterator_record.done) {
                    // NOTE: Step i. and ii. are handled below.
                    break;
                }

                // c. Let nextValue be Completion(IteratorValue(next)).
                auto next_value = iterator_value(vm, *next.value());

                // d. If nextValue is an abrupt completion, set iteratorRecord.[[Done]] to true.
                // e. ReturnIfAbrupt(nextValue).
                if (next_value.is_error()) {
                    iterator_record.done = true;
                    return next_value.release_error();
                }

                // f. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(n)), nextValue).
                array->indexed_properties().append(next_value.value());

                // g. Set n to n + 1.
            }
            value = array;
        }
        // SingleNameBinding : BindingIdentifier Initializer[opt]
        // BindingElement : BindingPattern Initializer[opt]
        else {
            // 1. Let v be undefined.
            value = js_undefined();

            // 2. If iteratorRecord.[[Done]] is false, then
            if (!iterator_record.done) {
                // a. Let next be Completion(IteratorStep(iteratorRecord)).
                auto next = iterator_step(vm, iterator_record);

                // b. If next is an abrupt completion, set iteratorRecord.[[Done]] to true.
                // c. ReturnIfAbrupt(next).
                if (next.is_error()) {
                    iterator_record.done = true;
                    return next.release_error();
                }

                // d. If next is false, set iteratorRecord.[[Done]] to true.
                if (!next.value()) {
                    iterator_record.done = true;
                }
                // e. Else,
                else {
                    // i. Set v to Completion(IteratorValue(next)).
                    auto value_or_error = iterator_value(vm, *next.value());

                    // ii. If v is an abrupt completion, set iteratorRecord.[[Done]] to true.
                    // iii. ReturnIfAbrupt(v).
                    if (value_or_error.is_throw_completion()) {
                        iterator_record.done = true;
                        return value_or_error.release_error();
                    }
                    value = value_or_error.release_value();
                }
            }

            // NOTE: Step 3. and 4. are handled below.
        }

        if (value.is_undefined() && entry.initializer) {
            VERIFY(!entry.is_rest);
            if (auto* identifier_ptr = entry.alias.get_pointer<NonnullRefPtr<Identifier const>>())
                value = TRY(named_evaluation_if_anonymous_function(*entry.initializer, (*identifier_ptr)->string()));
            else
                value = TRY(execute_ast_node(*entry.initializer));
        }

        if (auto* binding_ptr = entry.alias.get_pointer<NonnullRefPtr<BindingPattern const>>()) {
            TRY(binding_initialization(*binding_ptr, value, environment));
        } else if (!entry.alias.has<Empty>()) {
            VERIFY(assignment_target.has_value());
            if (!environment)
                TRY(assignment_target->put_value(vm, value));
            else
                TRY(assignment_target->initialize_referenced_binding(vm, value));
        }
    }

    return {};
}

// 9.1.2.1 GetIdentifierReference ( env, name, strict ), https://tc39.es/ecma262/#sec-getidentifierreference
ThrowCompletionOr<Reference> VM::get_identifier_reference(Environment* environment, DeprecatedFlyString name, bool strict, size_t hops)
{
    // 1. If env is the value null, then
    if (!environment) {
        // a. Return the Reference Record { [[Base]]: unresolvable, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { Reference::BaseType::Unresolvable, move(name), strict };
    }

    // 2. Let exists be ? env.HasBinding(name).
    Optional<size_t> index;
    auto exists = TRY(environment->has_binding(name, &index));

    // Note: This is an optimization for looking up the same reference.
    Optional<EnvironmentCoordinate> environment_coordinate;
    if (index.has_value()) {
        VERIFY(hops <= NumericLimits<u32>::max());
        VERIFY(index.value() <= NumericLimits<u32>::max());
        environment_coordinate = EnvironmentCoordinate { .hops = static_cast<u32>(hops), .index = static_cast<u32>(index.value()) };
    }

    // 3. If exists is true, then
    if (exists) {
        // a. Return the Reference Record { [[Base]]: env, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { *environment, move(name), strict, environment_coordinate };
    }
    // 4. Else,
    else {
        // a. Let outer be env.[[OuterEnv]].
        // b. Return ? GetIdentifierReference(outer, name, strict).
        return get_identifier_reference(environment->outer_environment(), move(name), strict, hops + 1);
    }
}

// 9.4.2 ResolveBinding ( name [ , env ] ), https://tc39.es/ecma262/#sec-resolvebinding
ThrowCompletionOr<Reference> VM::resolve_binding(DeprecatedFlyString const& name, Environment* environment)
{
    // 1. If env is not present or if env is undefined, then
    if (!environment) {
        // a. Set env to the running execution context's LexicalEnvironment.
        environment = running_execution_context().lexical_environment;
    }

    // 2. Assert: env is an Environment Record.
    VERIFY(environment);

    // 3. If the source text matched by the syntactic production that is being evaluated is contained in strict mode code, let strict be true; else let strict be false.
    bool strict = in_strict_mode();

    // 4. Return ? GetIdentifierReference(env, name, strict).
    return get_identifier_reference(environment, name, strict);

    // NOTE: The spec says:
    //       Note: The result of ResolveBinding is always a Reference Record whose [[ReferencedName]] field is name.
    //       But this is not actually correct as GetIdentifierReference (or really the methods it calls) can throw.
}

// 9.4.4 ResolveThisBinding ( ), https://tc39.es/ecma262/#sec-resolvethisbinding
ThrowCompletionOr<Value> VM::resolve_this_binding()
{
    auto& vm = *this;

    // 1. Let envRec be GetThisEnvironment().
    auto environment = get_this_environment(vm);

    // 2. Return ? envRec.GetThisBinding().
    return TRY(environment->get_this_binding(vm));
}

// 9.4.5 GetNewTarget ( ), https://tc39.es/ecma262/#sec-getnewtarget
Value VM::get_new_target()
{
    // 1. Let envRec be GetThisEnvironment().
    auto env = get_this_environment(*this);

    // 2. Assert: envRec has a [[NewTarget]] field.
    // 3. Return envRec.[[NewTarget]].
    return verify_cast<FunctionEnvironment>(*env).new_target();
}

// 13.3.12.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-meta-properties-runtime-semantics-evaluation
// ImportMeta branch only
Object* VM::get_import_meta()
{
    // 1. Let module be GetActiveScriptOrModule().
    auto script_or_module = get_active_script_or_module();

    // 2. Assert: module is a Source Text Module Record.
    auto& module = verify_cast<SourceTextModule>(*script_or_module.get<NonnullGCPtr<Module>>());

    // 3. Let importMeta be module.[[ImportMeta]].
    auto* import_meta = module.import_meta();

    // 4. If importMeta is empty, then
    if (import_meta == nullptr) {
        // a. Set importMeta to OrdinaryObjectCreate(null).
        import_meta = Object::create(*current_realm(), nullptr);

        // b. Let importMetaValues be HostGetImportMetaProperties(module).
        auto import_meta_values = host_get_import_meta_properties(module);

        // c. For each Record { [[Key]], [[Value]] } p of importMetaValues, do
        for (auto& entry : import_meta_values) {
            // i. Perform ! CreateDataPropertyOrThrow(importMeta, p.[[Key]], p.[[Value]]).
            MUST(import_meta->create_data_property_or_throw(entry.key, entry.value));
        }

        // d. Perform HostFinalizeImportMeta(importMeta, module).
        host_finalize_import_meta(import_meta, module);

        // e. Set module.[[ImportMeta]] to importMeta.
        module.set_import_meta({}, import_meta);

        // f. Return importMeta.
        return import_meta;
    }
    // 5. Else,
    else {
        // a. Assert: Type(importMeta) is Object.
        // Note: This is always true by the type.

        // b. Return importMeta.
        return import_meta;
    }
}

// 9.4.5 GetGlobalObject ( ), https://tc39.es/ecma262/#sec-getglobalobject
Object& VM::get_global_object()
{
    // 1. Let currentRealm be the current Realm Record.
    auto& current_realm = *this->current_realm();

    // 2. Return currentRealm.[[GlobalObject]].
    return current_realm.global_object();
}

bool VM::in_strict_mode() const
{
    if (execution_context_stack().is_empty())
        return false;
    return running_execution_context().is_strict_mode;
}

void VM::run_queued_promise_jobs()
{
    dbgln_if(PROMISE_DEBUG, "Running queued promise jobs");

    while (!m_promise_jobs.is_empty()) {
        auto job = m_promise_jobs.take_first();
        dbgln_if(PROMISE_DEBUG, "Calling promise job function");

        [[maybe_unused]] auto result = job();
    }
}

// 9.5.4 HostEnqueuePromiseJob ( job, realm ), https://tc39.es/ecma262/#sec-hostenqueuepromisejob
void VM::enqueue_promise_job(Function<ThrowCompletionOr<Value>()> job, Realm*)
{
    // An implementation of HostEnqueuePromiseJob must conform to the requirements in 9.5 as well as the following:
    // - FIXME: If realm is not null, each time job is invoked the implementation must perform implementation-defined steps such that execution is prepared to evaluate ECMAScript code at the time of job's invocation.
    // - FIXME: Let scriptOrModule be GetActiveScriptOrModule() at the time HostEnqueuePromiseJob is invoked. If realm is not null, each time job is invoked the implementation must perform implementation-defined steps
    //          such that scriptOrModule is the active script or module at the time of job's invocation.
    // - Jobs must run in the same order as the HostEnqueuePromiseJob invocations that scheduled them.
    m_promise_jobs.append(move(job));
}

void VM::run_queued_finalization_registry_cleanup_jobs()
{
    while (!m_finalization_registry_cleanup_jobs.is_empty()) {
        auto registry = m_finalization_registry_cleanup_jobs.take_first();
        // FIXME: Handle any uncatched exceptions here.
        (void)registry->cleanup();
    }
}

// 9.10.4.1 HostEnqueueFinalizationRegistryCleanupJob ( finalizationRegistry ), https://tc39.es/ecma262/#sec-host-cleanup-finalization-registry
void VM::enqueue_finalization_registry_cleanup_job(FinalizationRegistry& registry)
{
    m_finalization_registry_cleanup_jobs.append(&registry);
}

// 27.2.1.9 HostPromiseRejectionTracker ( promise, operation ), https://tc39.es/ecma262/#sec-host-promise-rejection-tracker
void VM::promise_rejection_tracker(Promise& promise, Promise::RejectionOperation operation) const
{
    switch (operation) {
    case Promise::RejectionOperation::Reject:
        // A promise was rejected without any handlers
        if (on_promise_unhandled_rejection)
            on_promise_unhandled_rejection(promise);
        break;
    case Promise::RejectionOperation::Handle:
        // A handler was added to an already rejected promise
        if (on_promise_rejection_handled)
            on_promise_rejection_handled(promise);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void VM::dump_backtrace() const
{
    for (ssize_t i = m_execution_context_stack.size() - 1; i >= 0; --i) {
        auto& frame = m_execution_context_stack[i];
        if (frame->source_range.source_code) {
            auto source_range = frame->source_range.realize();
            dbgln("-> {} @ {}:{},{}", frame->function_name, source_range.filename(), source_range.start.line, source_range.start.column);
        } else {
            dbgln("-> {}", frame->function_name);
        }
    }
}

void VM::save_execution_context_stack()
{
    m_saved_execution_context_stacks.append(move(m_execution_context_stack));
}

void VM::restore_execution_context_stack()
{
    m_execution_context_stack = m_saved_execution_context_stacks.take_last();
}

// 9.4.1 GetActiveScriptOrModule ( ), https://tc39.es/ecma262/#sec-getactivescriptormodule
ScriptOrModule VM::get_active_script_or_module() const
{
    // 1. If the execution context stack is empty, return null.
    if (m_execution_context_stack.is_empty())
        return Empty {};

    // 2. Let ec be the topmost execution context on the execution context stack whose ScriptOrModule component is not null.
    for (auto i = m_execution_context_stack.size() - 1; i > 0; i--) {
        if (!m_execution_context_stack[i]->script_or_module.has<Empty>())
            return m_execution_context_stack[i]->script_or_module;
    }

    // 3. If no such execution context exists, return null. Otherwise, return ec's ScriptOrModule.
    // Note: Since it is not empty we have 0 and since we got here all the
    //       above contexts don't have a non-null ScriptOrModule
    return m_execution_context_stack[0]->script_or_module;
}

VM::StoredModule* VM::get_stored_module(ScriptOrModule const&, DeprecatedString const& filename, DeprecatedString const&)
{
    // Note the spec says:
    // Each time this operation is called with a specific referencingScriptOrModule, specifier pair as arguments
    // it must return the same Module Record instance if it completes normally.
    // Currently, we ignore the referencing script or module but this might not be correct in all cases.

    // Editor's Note from https://tc39.es/proposal-json-modules/#sec-hostresolveimportedmodule
    // The above text implies that is recommended but not required that hosts do not use moduleRequest.[[Assertions]]
    // as part of the module cache key. In either case, an exception thrown from an import with a given assertion list
    // does not rule out success of another import with the same specifier but a different assertion list.

    auto end_or_module = m_loaded_modules.find_if([&](StoredModule const& stored_module) {
        return stored_module.filename == filename;
    });
    if (end_or_module.is_end())
        return nullptr;
    return &(*end_or_module);
}

ThrowCompletionOr<void> VM::link_and_eval_module(Badge<Interpreter>, SourceTextModule& module)
{
    return link_and_eval_module(module);
}

ThrowCompletionOr<void> VM::link_and_eval_module(Badge<Bytecode::Interpreter>, SourceTextModule& module)
{
    return link_and_eval_module(module);
}

ThrowCompletionOr<void> VM::link_and_eval_module(Module& module)
{
    auto filename = module.filename();

    auto module_or_end = m_loaded_modules.find_if([&](StoredModule const& stored_module) {
        return stored_module.module.ptr() == &module;
    });

    StoredModule* stored_module;

    if (module_or_end.is_end()) {
        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Warning introducing module via link_and_eval_module {}", module.filename());
        if (m_loaded_modules.size() > 0)
            dbgln("Warning: Using multiple modules as entry point can lead to unexpected results");

        m_loaded_modules.empend(
            NonnullGCPtr(module),
            module.filename(),
            DeprecatedString {}, // Null type
            module,
            true);
        stored_module = &m_loaded_modules.last();
    } else {
        stored_module = module_or_end.operator->();
        if (stored_module->has_once_started_linking) {
            dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Module already has started linking once {}", module.filename());
            return {};
        }
        stored_module->has_once_started_linking = true;
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Linking module {}", filename);
    auto linked_or_error = module.link(*this);
    if (linked_or_error.is_error())
        return linked_or_error.throw_completion();

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Linking passed, now evaluating module {}", filename);
    auto evaluated_or_error = module.evaluate(*this);

    if (evaluated_or_error.is_error())
        return evaluated_or_error.throw_completion();

    auto* evaluated_value = evaluated_or_error.value();

    run_queued_promise_jobs();
    VERIFY(m_promise_jobs.is_empty());

    // FIXME: This will break if we start doing promises actually asynchronously.
    VERIFY(evaluated_value->state() != Promise::State::Pending);

    if (evaluated_value->state() == Promise::State::Rejected)
        return JS::throw_completion(evaluated_value->result());

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Evaluating passed for module {}", module.filename());
    return {};
}

static DeprecatedString resolve_module_filename(StringView filename, StringView module_type)
{
    auto extensions = Vector<StringView, 2> { "js"sv, "mjs"sv };
    if (module_type == "json"sv)
        extensions = { "json"sv };
    if (!FileSystem::exists(filename)) {
        for (auto extension : extensions) {
            // import "./foo" -> import "./foo.ext"
            auto resolved_filepath = DeprecatedString::formatted("{}.{}", filename, extension);
            if (FileSystem::exists(resolved_filepath))
                return resolved_filepath;
        }
    } else if (FileSystem::is_directory(filename)) {
        for (auto extension : extensions) {
            // import "./foo" -> import "./foo/index.ext"
            auto resolved_filepath = LexicalPath::join(filename, DeprecatedString::formatted("index.{}", extension)).string();
            if (FileSystem::exists(resolved_filepath))
                return resolved_filepath;
        }
    }
    return filename;
}

// 16.2.1.7 HostResolveImportedModule ( referencingScriptOrModule, specifier ), https://tc39.es/ecma262/#sec-hostresolveimportedmodule
ThrowCompletionOr<NonnullGCPtr<Module>> VM::resolve_imported_module(ScriptOrModule referencing_script_or_module, ModuleRequest const& module_request)
{
    // An implementation of HostResolveImportedModule must conform to the following requirements:
    //  - If it completes normally, the [[Value]] slot of the completion must contain an instance of a concrete subclass of Module Record.
    //  - If a Module Record corresponding to the pair referencingScriptOrModule, moduleRequest does not exist or cannot be created, an exception must be thrown.
    //  - Each time this operation is called with a specific referencingScriptOrModule, moduleRequest.[[Specifier]], moduleRequest.[[Assertions]] triple
    //    as arguments it must return the same Module Record instance if it completes normally.
    //    * It is recommended but not required that implementations additionally conform to the following stronger constraint:
    //      each time this operation is called with a specific referencingScriptOrModule, moduleRequest.[[Specifier]] pair as arguments it must return the same Module Record instance if it completes normally.
    //   - moduleRequest.[[Assertions]] must not influence the interpretation of the module or the module specifier;
    //     instead, it may be used to determine whether the algorithm completes normally or with an abrupt completion.

    // Multiple different referencingScriptOrModule, moduleRequest.[[Specifier]] pairs may map to the same Module Record instance.
    // The actual mapping semantic is host-defined but typically a normalization process is applied to specifier as part of the mapping process.
    // A typical normalization process would include actions such as alphabetic case folding and expansion of relative and abbreviated path specifiers.

    // We only allow "type" as a supported assertion so it is the only valid key that should ever arrive here.
    VERIFY(module_request.assertions.is_empty() || (module_request.assertions.size() == 1 && module_request.assertions.first().key == "type"));
    auto module_type = module_request.assertions.is_empty() ? DeprecatedString {} : module_request.assertions.first().value;

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] module at {} has type {} [is_null={}]", module_request.module_specifier, module_type, module_type.is_null());

    StringView base_filename = referencing_script_or_module.visit(
        [&](Empty) {
            return "."sv;
        },
        [&](auto& script_or_module) {
            return script_or_module->filename();
        });

    LexicalPath base_path { base_filename };
    auto filename = LexicalPath::absolute_path(base_path.dirname(), module_request.module_specifier);

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] base path: '{}'", base_path);
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] initial filename: '{}'", filename);

    filename = resolve_module_filename(filename, module_type);

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolved filename: '{}'", filename);

#if JS_MODULE_DEBUG
    DeprecatedString referencing_module_string = referencing_script_or_module.visit(
        [&](Empty) -> DeprecatedString {
            return ".";
        },
        [&](auto& script_or_module) {
            if constexpr (IsSame<Script*, decltype(script_or_module)>) {
                return DeprecatedString::formatted("Script @ {}", script_or_module.ptr());
            }
            return DeprecatedString::formatted("Module @ {}", script_or_module.ptr());
        });

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module({}, {})", referencing_module_string, filename);
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE]     resolved {} + {} -> {}", base_path, module_request.module_specifier, filename);
#endif

    auto* loaded_module_or_end = get_stored_module(referencing_script_or_module, filename, module_type);
    if (loaded_module_or_end != nullptr) {
        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module({}) already loaded at {}", filename, loaded_module_or_end->module.ptr());
        return NonnullGCPtr(*loaded_module_or_end->module);
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] reading and parsing module {}", filename);

    auto file_or_error = Core::File::open(filename, Core::File::OpenMode::Read);

    if (file_or_error.is_error()) {
        return throw_completion<SyntaxError>(ErrorType::ModuleNotFound, module_request.module_specifier);
    }

    // FIXME: Don't read the file in one go.
    auto file_content_or_error = file_or_error.value()->read_until_eof();

    if (file_content_or_error.is_error()) {
        if (file_content_or_error.error().code() == ENOMEM)
            return throw_completion<JS::InternalError>(error_message(::JS::VM::ErrorMessage::OutOfMemory));
        return throw_completion<SyntaxError>(ErrorType::ModuleNotFound, module_request.module_specifier);
    }

    StringView const content_view { file_content_or_error.value().bytes() };

    auto module = TRY([&]() -> ThrowCompletionOr<NonnullGCPtr<Module>> {
        // If assertions has an entry entry such that entry.[[Key]] is "type", let type be entry.[[Value]]. The following requirements apply:
        // If type is "json", then this algorithm must either invoke ParseJSONModule and return the resulting Completion Record, or throw an exception.
        if (module_type == "json"sv) {
            dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] reading and parsing JSON module {}", filename);
            return parse_json_module(content_view, *current_realm(), filename);
        }

        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] reading and parsing as SourceTextModule module {}", filename);
        // Note: We treat all files as module, so if a script does not have exports it just runs it.
        auto module_or_errors = SourceTextModule::parse(content_view, *current_realm(), filename);

        if (module_or_errors.is_error()) {
            VERIFY(module_or_errors.error().size() > 0);
            return throw_completion<SyntaxError>(module_or_errors.error().first().to_deprecated_string());
        }
        return module_or_errors.release_value();
    }());

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module(...) parsed {} to {}", filename, module.ptr());

    // We have to set it here already in case it references itself.
    m_loaded_modules.empend(
        referencing_script_or_module,
        filename,
        module_type,
        *module,
        false);

    return module;
}

// 16.2.1.8 HostImportModuleDynamically ( referencingScriptOrModule, specifier, promiseCapability ), https://tc39.es/ecma262/#sec-hostimportmoduledynamically
ThrowCompletionOr<void> VM::import_module_dynamically(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability const& promise_capability)
{
    auto& realm = *current_realm();

    // Success path:
    //  - At some future time, the host environment must perform FinishDynamicImport(referencingScriptOrModule, moduleRequest, promiseCapability, promise),
    //    where promise is a Promise resolved with undefined.
    //  - Any subsequent call to HostResolveImportedModule after FinishDynamicImport has completed,
    //    given the arguments referencingScriptOrModule and specifier, must return a normal completion
    //    containing a module which has already been evaluated, i.e. whose Evaluate concrete method has
    //    already been called and returned a normal completion.
    // Failure path:
    //  - At some future time, the host environment must perform
    //    FinishDynamicImport(referencingScriptOrModule, moduleRequest, promiseCapability, promise),
    //    where promise is a Promise rejected with an error representing the cause of failure.

    auto promise = Promise::create(realm);

    ScopeGuard finish_dynamic_import = [&] {
        host_finish_dynamic_import(referencing_script_or_module, module_request, promise_capability, promise);
    };

    // Generally within ECMA262 we always get a referencing_script_or_moulde. However, ShadowRealm gives an explicit null.
    // To get around this is we attempt to get the active script_or_module otherwise we might start loading "random" files from the working directory.
    if (referencing_script_or_module.has<Empty>()) {
        referencing_script_or_module = get_active_script_or_module();

        // If there is no ScriptOrModule in any of the execution contexts
        if (referencing_script_or_module.has<Empty>()) {
            // Throw an error for now
            promise->reject(InternalError::create(realm, TRY_OR_THROW_OOM(*this, String::formatted(ErrorType::ModuleNotFoundNoReferencingScript.message(), module_request.module_specifier))));
            return {};
        }
    }

    // Note: If host_resolve_imported_module returns a module it has been loaded successfully and the next call in finish_dynamic_import will retrieve it again.
    auto module_or_error = host_resolve_imported_module(referencing_script_or_module, module_request);
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] HostImportModuleDynamically(..., {}) -> {}", module_request.module_specifier, module_or_error.is_error() ? "failed" : "passed");
    if (module_or_error.is_throw_completion()) {
        promise->reject(*module_or_error.throw_completion().value());
    } else {
        auto module = module_or_error.release_value();
        auto& source_text_module = static_cast<Module&>(*module);

        auto evaluated_or_error = link_and_eval_module(source_text_module);

        if (evaluated_or_error.is_throw_completion()) {
            promise->reject(*evaluated_or_error.throw_completion().value());
        } else {
            promise->fulfill(js_undefined());
        }
    }

    // It must return unused.
    // Note: Just return void always since the resulting value cannot be accessed by user code.
    return {};
}

// 16.2.1.9 FinishDynamicImport ( referencingScriptOrModule, specifier, promiseCapability, innerPromise ), https://tc39.es/ecma262/#sec-finishdynamicimport
void VM::finish_dynamic_import(ScriptOrModule referencing_script_or_module, ModuleRequest module_request, PromiseCapability const& promise_capability, Promise* inner_promise)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] finish_dynamic_import on {}", module_request.module_specifier);

    auto& realm = *current_realm();

    // 1. Let fulfilledClosure be a new Abstract Closure with parameters (result) that captures referencingScriptOrModule, specifier, and promiseCapability and performs the following steps when called:
    auto fulfilled_closure = [referencing_script_or_module = move(referencing_script_or_module), module_request = move(module_request), &promise_capability](VM& vm) -> ThrowCompletionOr<Value> {
        auto result = vm.argument(0);
        // a. Assert: result is undefined.
        VERIFY(result.is_undefined());
        // b. Let moduleRecord be ! HostResolveImportedModule(referencingScriptOrModule, specifier).
        auto module_record = MUST(vm.host_resolve_imported_module(referencing_script_or_module, module_request));

        // c. Assert: Evaluate has already been invoked on moduleRecord and successfully completed.
        // Note: If HostResolveImportedModule returns a module evaluate will have been called on it.

        // d. Let namespace be Completion(GetModuleNamespace(moduleRecord)).
        auto namespace_ = module_record->get_module_namespace(vm);

        // e. If namespace is an abrupt completion, then
        if (namespace_.is_throw_completion()) {
            // i. Perform ! Call(promiseCapability.[[Reject]], undefined, « namespace.[[Value]] »).
            MUST(call(vm, *promise_capability.reject(), js_undefined(), *namespace_.throw_completion().value()));
        }
        // f. Else,
        else {
            // i. Perform ! Call(promiseCapability.[[Resolve]], undefined, « namespace.[[Value]] »).
            MUST(call(vm, *promise_capability.resolve(), js_undefined(), namespace_.release_value()));
        }
        // g. Return unused.
        // NOTE: We don't support returning an empty/optional/unused value here.
        return js_undefined();
    };

    // 2. Let onFulfilled be CreateBuiltinFunction(fulfilledClosure, 0, "", « »).
    auto on_fulfilled = NativeFunction::create(realm, move(fulfilled_closure), 0, "");

    // 3. Let rejectedClosure be a new Abstract Closure with parameters (error) that captures promiseCapability and performs the following steps when called:
    auto rejected_closure = [&promise_capability](VM& vm) -> ThrowCompletionOr<Value> {
        auto error = vm.argument(0);
        // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « error »).
        MUST(call(vm, *promise_capability.reject(), js_undefined(), error));

        // b. Return unused.
        // NOTE: We don't support returning an empty/optional/unused value here.
        return js_undefined();
    };

    // 4. Let onRejected be CreateBuiltinFunction(rejectedClosure, 0, "", « »).
    auto on_rejected = NativeFunction::create(realm, move(rejected_closure), 0, "");

    // 5. Perform PerformPromiseThen(innerPromise, onFulfilled, onRejected).
    inner_promise->perform_then(on_fulfilled, on_rejected, {});

    // 6. Return unused.
}

}
