/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Demangle.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Shape.h>
#include <typeinfo>

namespace JS {

class InterpreterNodeScope {
    AK_MAKE_NONCOPYABLE(InterpreterNodeScope);
    AK_MAKE_NONMOVABLE(InterpreterNodeScope);

public:
    InterpreterNodeScope(Interpreter& interpreter, ASTNode const& node)
        : m_interpreter(interpreter)
        , m_chain_node { nullptr, node }
    {
        m_interpreter.vm().running_execution_context().current_node = &node;
        m_interpreter.push_ast_node(m_chain_node);
    }

    ~InterpreterNodeScope()
    {
        m_interpreter.pop_ast_node();
    }

private:
    Interpreter& m_interpreter;
    ExecutingASTNodeChain m_chain_node;
};

String ASTNode::class_name() const
{
    // NOTE: We strip the "JS::" prefix.
    return demangle(typeid(*this).name()).substring(4);
}

static void update_function_name(Value value, FlyString const& name)
{
    if (!value.is_function())
        return;
    auto& function = value.as_function();
    if (is<ECMAScriptFunctionObject>(function) && function.name().is_empty())
        static_cast<ECMAScriptFunctionObject&>(function).set_name(name);
}

static ThrowCompletionOr<String> get_function_name(GlobalObject& global_object, Value value)
{
    if (value.is_symbol())
        return String::formatted("[{}]", value.as_symbol().description());
    if (value.is_string())
        return value.as_string().string();
    return value.to_string(global_object);
}

Value ScopeNode::evaluate_statements(Interpreter& interpreter, GlobalObject& global_object) const
{
    // FIXME: This should use completions but for now we just use the vm to communicate things.
    auto& vm = interpreter.vm();
    Value last_value;
    for (auto& node : children()) {
        auto value = node.execute(interpreter, global_object);
        if (!value.is_empty())
            last_value = value;
        if (vm.should_unwind()) {
            break;
        }
    }
    return last_value;
}

Value FunctionBody::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Note: Scoping should have already been setup by whoever is calling this FunctionBody.
    auto function_result = evaluate_statements(interpreter, global_object);

    if (interpreter.exception())
        return {};

    if (interpreter.vm().unwind_until() != ScopeType::Function)
        function_result = js_undefined();
    else
        interpreter.vm().stop_unwind();

    return function_result;
}

// 14.2.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
Value BlockStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    Environment* old_environment { nullptr };
    ArmedScopeGuard restore_environment = [&] {
        vm.running_execution_context().lexical_environment = old_environment;
    };

    // Optimization: We only need a new lexical environment if there are any lexical declarations. :^)
    if (has_lexical_declarations()) {
        old_environment = vm.running_execution_context().lexical_environment;
        auto* block_environment = new_declarative_environment(*old_environment);
        block_declaration_instantiation(global_object, block_environment);
        vm.running_execution_context().lexical_environment = block_environment;
    } else {
        restore_environment.disarm();
    }

    auto block_value = evaluate_statements(interpreter, global_object);
    if (!labels().is_empty() && vm.should_unwind_until(ScopeType::Breakable, labels()))
        vm.stop_unwind();

    if (vm.exception())
        return {};

    return block_value;
}

Value Program::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // FIXME: This tries to be "ScriptEvaluation" and "evaluating scriptBody" at once. It shouldn't.
    //        Clean this up and update perform_eval() / perform_shadow_realm_eval()

    InterpreterNodeScope node_scope { interpreter, *this };

    VERIFY(interpreter.lexical_environment() && interpreter.lexical_environment()->is_global_environment());
    auto& global_env = static_cast<GlobalEnvironment&>(*interpreter.lexical_environment());

    TRY_OR_DISCARD(global_declaration_instantiation(interpreter, global_object, global_env));

    return evaluate_statements(interpreter, global_object);
}

Value FunctionDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_is_hoisted) {
        // Perform special annexB steps see step 3 of: https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation
        auto* variable_environment = interpreter.vm().running_execution_context().variable_environment;
        auto* lexical_environment = interpreter.vm().running_execution_context().lexical_environment;
        auto function_object = MUST(lexical_environment->get_binding_value(global_object, name(), false));
        MUST(variable_environment->set_mutable_binding(global_object, name(), function_object, false));
    }

    return {};
}

Value FunctionExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return instantiate_ordinary_function_expression(interpreter, global_object, name());
}

// 15.2.5 Runtime Semantics: InstantiateOrdinaryFunctionExpression, https://tc39.es/ecma262/#sec-runtime-semantics-instantiateordinaryfunctionexpression
Value FunctionExpression::instantiate_ordinary_function_expression(Interpreter& interpreter, GlobalObject& global_object, FlyString given_name) const
{
    if (given_name.is_empty())
        given_name = "";
    auto has_own_name = !name().is_empty();

    auto const& used_name = has_own_name ? name() : given_name;
    auto* scope = interpreter.lexical_environment();
    if (has_own_name) {
        VERIFY(scope);
        scope = new_declarative_environment(*scope);
        MUST(scope->create_immutable_binding(global_object, name(), false));
    }

    auto* private_scope = interpreter.vm().running_execution_context().private_environment;

    auto closure = ECMAScriptFunctionObject::create(global_object, used_name, body(), parameters(), function_length(), scope, private_scope, kind(), is_strict_mode(), might_need_arguments_object(), contains_direct_call_to_eval(), is_arrow_function());

    // FIXME: 6. Perform SetFunctionName(closure, name).
    // FIXME: 7. Perform MakeConstructor(closure).

    if (has_own_name)
        MUST(scope->initialize_binding(global_object, name(), closure));

    return closure;
}

Value ExpressionStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_expression->execute(interpreter, global_object);
}

CallExpression::ThisAndCallee CallExpression::compute_this_and_callee(Interpreter& interpreter, GlobalObject& global_object, Reference const& callee_reference) const
{
    if (callee_reference.is_property_reference()) {
        auto this_value = callee_reference.get_this_value();
        auto callee = TRY_OR_DISCARD(callee_reference.get_value(global_object));

        return { this_value, callee };
    }

    // [[Call]] will handle that in non-strict mode the this value becomes the global object
    return {
        js_undefined(),
        callee_reference.is_unresolvable()
            ? m_callee->execute(interpreter, global_object)
            : TRY_OR_DISCARD(callee_reference.get_value(global_object))
    };
}

// 13.3.8.1 Runtime Semantics: ArgumentListEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
static void argument_list_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<CallExpression::Argument> const& arguments, MarkedValueList& list)
{
    auto& vm = global_object.vm();
    list.ensure_capacity(arguments.size());

    for (auto& argument : arguments) {
        auto value = argument.value->execute(interpreter, global_object);
        if (vm.exception())
            return;
        if (argument.is_spread) {
            auto result = get_iterator_values(global_object, value, [&](Value iterator_value) -> Optional<Completion> {
                list.append(iterator_value);
                return {};
            });
            if (result.is_error())
                return;
        } else {
            list.append(value);
        }
    }
}

Value NewExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    auto callee_value = m_callee->execute(interpreter, global_object);
    if (vm.exception())
        return {};

    if (!callee_value.is_function() || !callee_value.as_function().has_constructor()) {
        throw_type_error_for_callee(interpreter, global_object, callee_value, "constructor"sv);
        return {};
    }

    MarkedValueList arg_list(vm.heap());
    argument_list_evaluation(interpreter, global_object, m_arguments, arg_list);
    if (interpreter.exception())
        return {};

    auto& function = callee_value.as_function();
    return TRY_OR_DISCARD(construct(global_object, function, move(arg_list)));
}

void CallExpression::throw_type_error_for_callee(Interpreter& interpreter, GlobalObject& global_object, Value callee_value, StringView call_type) const
{
    auto& vm = interpreter.vm();
    if (is<Identifier>(*m_callee) || is<MemberExpression>(*m_callee)) {
        String expression_string;
        if (is<Identifier>(*m_callee)) {
            expression_string = static_cast<Identifier const&>(*m_callee).string();
        } else {
            expression_string = static_cast<MemberExpression const&>(*m_callee).to_string_approximation();
        }
        vm.throw_exception<TypeError>(global_object, ErrorType::IsNotAEvaluatedFrom, callee_value.to_string_without_side_effects(), call_type, expression_string);
    } else {
        vm.throw_exception<TypeError>(global_object, ErrorType::IsNotA, callee_value.to_string_without_side_effects(), call_type);
    }
}

Value CallExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto callee_reference = m_callee->to_reference(interpreter, global_object);
    if (vm.exception())
        return {};

    auto [this_value, callee] = compute_this_and_callee(interpreter, global_object, callee_reference);
    if (vm.exception())
        return {};

    VERIFY(!callee.is_empty());

    MarkedValueList arg_list(vm.heap());
    argument_list_evaluation(interpreter, global_object, m_arguments, arg_list);
    if (interpreter.exception())
        return {};

    if (!callee.is_function()) {
        throw_type_error_for_callee(interpreter, global_object, callee, "function"sv);
        return {};
    }

    auto& function = callee.as_function();

    if (&function == global_object.eval_function()
        && callee_reference.is_environment_reference()
        && callee_reference.name().is_string()
        && callee_reference.name().as_string() == vm.names.eval.as_string()) {

        auto script_value = arg_list.size() == 0 ? js_undefined() : arg_list[0];
        return TRY_OR_DISCARD(perform_eval(script_value, global_object, vm.in_strict_mode() ? CallerMode::Strict : CallerMode::NonStrict, EvalMode::Direct));
    }

    return TRY_OR_DISCARD(vm.call(function, this_value, move(arg_list)));
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
// SuperCall : super Arguments
Value SuperCall::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let newTarget be GetNewTarget().
    auto new_target = vm.get_new_target();
    if (vm.exception())
        return {};

    // 2. Assert: Type(newTarget) is Object.
    VERIFY(new_target.is_function());

    // 3. Let func be ! GetSuperConstructor().
    auto* func = get_super_constructor(interpreter.vm());
    VERIFY(!vm.exception());

    // 4. Let argList be ? ArgumentListEvaluation of Arguments.
    MarkedValueList arg_list(vm.heap());
    argument_list_evaluation(interpreter, global_object, m_arguments, arg_list);
    if (interpreter.exception())
        return {};

    // 5. If IsConstructor(func) is false, throw a TypeError exception.
    if (!func || !Value(func).is_constructor()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, "Super constructor");
        return {};
    }

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto* result = TRY_OR_DISCARD(construct(global_object, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_er = verify_cast<FunctionEnvironment>(get_this_environment(interpreter.vm()));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY_OR_DISCARD(this_er.bind_this_value(global_object, result));

    // 9. Let F be thisER.[[FunctionObject]].
    // 10. Assert: F is an ECMAScript function object. (NOTE: This is implied by the strong C++ type.)
    [[maybe_unused]] auto& f = this_er.function_object();

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY_OR_DISCARD(vm.initialize_instance_elements(*result, f));

    // 12. Return result.
    return result;
}

Value YieldExpression::execute(Interpreter&, GlobalObject&) const
{
    // This should be transformed to a return.
    VERIFY_NOT_REACHED();
}

// 15.8.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-async-function-definitions-runtime-semantics-evaluation
Value AwaitExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating UnaryExpression.
    // 2. Let value be ? GetValue(exprRef).
    auto value = m_argument->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    // 3. Return ? Await(value).
    return TRY_OR_DISCARD(await(global_object, value));
}

Value ReturnStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto value = argument() ? argument()->execute(interpreter, global_object) : js_undefined();
    if (interpreter.exception())
        return {};
    interpreter.vm().unwind(ScopeType::Function);
    return value;
}

Value IfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto predicate_result = m_predicate->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    if (predicate_result.to_boolean())
        return m_consequent->execute(interpreter, global_object);

    if (m_alternate)
        return m_alternate->execute(interpreter, global_object);

    return js_undefined();
}

// 14.11.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-with-statement-runtime-semantics-evaluation
// WithStatement : with ( Expression ) Statement
Value WithStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let value be the result of evaluating Expression.
    auto value = m_object->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    // 2. Let obj be ? ToObject(? GetValue(value)).
    auto* object = TRY_OR_DISCARD(value.to_object(global_object));

    // 3. Let oldEnv be the running execution context's LexicalEnvironment.
    auto* old_environment = interpreter.vm().running_execution_context().lexical_environment;

    // 4. Let newEnv be NewObjectEnvironment(obj, true, oldEnv).
    auto* new_environment = new_object_environment(*object, true, old_environment);
    if (interpreter.exception())
        return {};

    // 5. Set the running execution context's LexicalEnvironment to newEnv.
    interpreter.vm().running_execution_context().lexical_environment = new_environment;

    // 6. Let C be the result of evaluating Statement.
    auto result = m_body->execute(interpreter, global_object).value_or(js_undefined());

    // 7. Set the running execution context's LexicalEnvironment to oldEnv.
    interpreter.vm().running_execution_context().lexical_environment = old_environment;

    if (interpreter.exception())
        return {};

    // 8. Return Completion(UpdateEmpty(C, undefined)).
    return result;
}

Value WhileStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto last_value = js_undefined();
    for (;;) {
        auto test_result = m_test->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!test_result.to_boolean())
            break;
        last_value = m_body->execute(interpreter, global_object).value_or(last_value);
        if (interpreter.exception())
            return {};
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                interpreter.vm().stop_unwind();
                break;
            } else {
                return last_value;
            }
        }
    }

    return last_value;
}

Value DoWhileStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto last_value = js_undefined();
    for (;;) {
        if (interpreter.exception())
            return {};
        last_value = m_body->execute(interpreter, global_object).value_or(last_value);
        if (interpreter.exception())
            return {};
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                interpreter.vm().stop_unwind();
                break;
            } else {
                return last_value;
            }
        }
        auto test_result = m_test->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!test_result.to_boolean())
            break;
    }

    return last_value;
}

Value ForStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Note we don't always set a new environment but to use RAII we must do this here.
    auto* old_environment = interpreter.lexical_environment();
    ScopeGuard restore_old_environment = [&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    };

    Vector<FlyString> let_declarations;

    if (m_init) {
        if (is<VariableDeclaration>(*m_init) && static_cast<VariableDeclaration const&>(*m_init).declaration_kind() != DeclarationKind::Var) {
            auto* loop_environment = new_declarative_environment(*old_environment);
            auto& declaration = static_cast<VariableDeclaration const&>(*m_init);
            declaration.for_each_bound_name([&](auto const& name) {
                if (declaration.declaration_kind() == DeclarationKind::Const) {
                    MUST(loop_environment->create_immutable_binding(global_object, name, true));
                } else {
                    MUST(loop_environment->create_mutable_binding(global_object, name, false));
                    let_declarations.append(name);
                }
                return IterationDecision::Continue;
            });

            interpreter.vm().running_execution_context().lexical_environment = loop_environment;
        }

        m_init->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
    }

    auto last_value = js_undefined();

    // 14.7.4.4 CreatePerIterationEnvironment ( perIterationBindings ), https://tc39.es/ecma262/#sec-createperiterationenvironment
    auto create_per_iteration_environment = [&]() -> ThrowCompletionOr<void> {
        if (let_declarations.is_empty())
            return {};

        auto* last_iteration_env = interpreter.lexical_environment();
        auto* outer = last_iteration_env->outer_environment();
        VERIFY(outer);
        auto* this_iteration_env = new_declarative_environment(*outer);
        for (auto& name : let_declarations) {
            MUST(this_iteration_env->create_mutable_binding(global_object, name, false));
            auto last_value = TRY(last_iteration_env->get_binding_value(global_object, name, true));
            VERIFY(!last_value.is_empty());
            MUST(this_iteration_env->initialize_binding(global_object, name, last_value));
        }
        interpreter.vm().running_execution_context().lexical_environment = this_iteration_env;

        return {};
    };

    TRY_OR_DISCARD(create_per_iteration_environment());

    auto test_empty_or_true = [&] {
        if (!m_test)
            return true;

        auto test_result = m_test->execute(interpreter, global_object);
        if (interpreter.exception())
            return false;
        return test_result.to_boolean();
    };

    while (true) {
        if (!test_empty_or_true())
            break;

        last_value = m_body->execute(interpreter, global_object).value_or(last_value);
        if (interpreter.exception())
            return {};
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                interpreter.vm().stop_unwind();
                break;
            } else {
                return last_value;
            }
        }

        TRY_OR_DISCARD(create_per_iteration_environment());

        if (m_update) {
            m_update->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
        }
    }
    if (interpreter.exception())
        return {};
    return last_value;
}

struct ForInOfHeadState {
    explicit ForInOfHeadState(Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs)
    {
        lhs.visit(
            [&](NonnullRefPtr<ASTNode>& ast_node) {
                expression_lhs = ast_node.ptr();
            },
            [&](NonnullRefPtr<BindingPattern>& pattern) {
                pattern_lhs = pattern.ptr();
                destructuring = true;
                lhs_kind = Assignment;
            });
    }

    ASTNode* expression_lhs = nullptr;
    BindingPattern* pattern_lhs = nullptr;
    enum LhsKind {
        Assignment,
        VarBinding,
        LexicalBinding
    };
    LhsKind lhs_kind = Assignment;
    bool destructuring = false;

    Value rhs_value;

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // Note: This is only steps 6.g through 6.j of the method because we currently implement for-in without an iterator so to prevent duplicated code we do this part here.
    ThrowCompletionOr<void> execute_head(Interpreter& interpreter, GlobalObject& global_object, Value next_value) const
    {
        VERIFY(!next_value.is_empty());

        Optional<Reference> lhs_reference;
        Environment* iteration_environment = nullptr;

        // g. If lhsKind is either assignment or varBinding, then
        if (lhs_kind == Assignment || lhs_kind == VarBinding) {
            if (!destructuring) {
                VERIFY(expression_lhs);
                if (is<VariableDeclaration>(*expression_lhs)) {
                    auto& declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
                    VERIFY(declaration.declarations().first().target().has<NonnullRefPtr<Identifier>>());
                    lhs_reference = declaration.declarations().first().target().get<NonnullRefPtr<Identifier>>()->to_reference(interpreter, global_object);
                } else {
                    VERIFY(is<Identifier>(*expression_lhs) || is<MemberExpression>(*expression_lhs));
                    auto& expression = static_cast<Expression const&>(*expression_lhs);
                    lhs_reference = expression.to_reference(interpreter, global_object);
                }
            }
        }
        // h. Else,
        else {
            VERIFY(expression_lhs && is<VariableDeclaration>(*expression_lhs));
            iteration_environment = new_declarative_environment(*interpreter.lexical_environment());
            auto& for_declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
            for_declaration.for_each_bound_name([&](auto const& name) {
                if (for_declaration.declaration_kind() == DeclarationKind::Const)
                    MUST(iteration_environment->create_immutable_binding(global_object, name, false));
                else
                    MUST(iteration_environment->create_mutable_binding(global_object, name, true));
            });
            interpreter.vm().running_execution_context().lexical_environment = iteration_environment;

            if (!destructuring) {
                VERIFY(for_declaration.declarations().first().target().has<NonnullRefPtr<Identifier>>());
                lhs_reference = interpreter.vm().resolve_binding(for_declaration.declarations().first().target().get<NonnullRefPtr<Identifier>>()->string());
            }
        }

        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());

        // i. If destructuring is false, then
        if (!destructuring) {
            VERIFY(lhs_reference.has_value());
            if (lhs_kind == LexicalBinding)
                return lhs_reference->initialize_referenced_binding(global_object, next_value);
            else
                return lhs_reference->put_value(global_object, next_value);
        }

        // j. Else,
        if (lhs_kind == Assignment) {
            VERIFY(pattern_lhs);
            return interpreter.vm().destructuring_assignment_evaluation(*pattern_lhs, next_value, global_object);
        }
        VERIFY(expression_lhs && is<VariableDeclaration>(*expression_lhs));
        auto& for_declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
        auto& binding_pattern = for_declaration.declarations().first().target().get<NonnullRefPtr<BindingPattern>>();
        VERIFY(lhs_kind == VarBinding || iteration_environment);

        // At this point iteration_environment is undefined if lhs_kind == VarBinding which means this does both
        // branch j.ii and j.iii because ForBindingInitialization is just a forwarding call to BindingInitialization.
        return interpreter.vm().binding_initialization(binding_pattern, next_value, iteration_environment, global_object);
    }
};

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
// 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
// This method combines ForInOfLoopEvaluation and ForIn/OfHeadEvaluation for similar reason as ForIn/OfBodyEvaluation, to prevent code duplication.
// For the same reason we also skip step 6 and 7 of ForIn/OfHeadEvaluation as this is done by the appropriate for loop type.
static ThrowCompletionOr<ForInOfHeadState> for_in_of_head_execute(Interpreter& interpreter, GlobalObject& global_object, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs, Expression const& rhs)
{
    ForInOfHeadState state(lhs);
    if (auto* ast_ptr = lhs.get_pointer<NonnullRefPtr<ASTNode>>(); ast_ptr && is<VariableDeclaration>(*(*ast_ptr))) {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( var ForBinding in Expression ) Statement
        //  ForInOfStatement : for ( ForDeclaration in Expression ) Statement
        //  ForInOfStatement : for ( var ForBinding of AssignmentExpression ) Statement
        //  ForInOfStatement : for ( ForDeclaration of AssignmentExpression ) Statement

        // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
        Environment* new_environment = nullptr;

        auto& variable_declaration = static_cast<VariableDeclaration const&>(*(*ast_ptr));
        VERIFY(variable_declaration.declarations().size() == 1);
        state.destructuring = variable_declaration.declarations().first().target().has<NonnullRefPtr<BindingPattern>>();
        if (variable_declaration.declaration_kind() == DeclarationKind::Var) {
            state.lhs_kind = ForInOfHeadState::VarBinding;
            auto& variable = variable_declaration.declarations().first();
            // B.3.5 Initializers in ForIn Statement Heads, https://tc39.es/ecma262/#sec-initializers-in-forin-statement-heads
            if (variable.init()) {
                VERIFY(variable.target().has<NonnullRefPtr<Identifier>>());
                auto& binding_id = variable.target().get<NonnullRefPtr<Identifier>>()->string();
                auto reference = interpreter.vm().resolve_binding(binding_id);
                if (auto* exception = interpreter.exception())
                    return throw_completion(exception->value());

                auto result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, *variable.init(), binding_id));
                TRY(reference.put_value(global_object, result));
            }
        } else {
            state.lhs_kind = ForInOfHeadState::LexicalBinding;
            new_environment = new_declarative_environment(*interpreter.lexical_environment());
            variable_declaration.for_each_bound_name([&](auto const& name) {
                MUST(new_environment->create_mutable_binding(global_object, name, false));
            });
        }

        if (new_environment) {
            // 2.d Set the running execution context's LexicalEnvironment to newEnv.
            TemporaryChange<Environment*> scope_change(interpreter.vm().running_execution_context().lexical_environment, new_environment);

            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = rhs.execute(interpreter, global_object);

            // Note that since a reference stores it's environment it doesn't matter we only reset
            // this after step 5. (Also we have no way of separating these steps at this point)
            // 4. Set the running execution context's LexicalEnvironment to oldEnv.
        } else {
            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = rhs.execute(interpreter, global_object);
        }

        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());

        return state;
    }

    // Runtime Semantics: ForInOfLoopEvaluation, for any of:
    //  ForInOfStatement : for ( LeftHandSideExpression in Expression ) Statement
    //  ForInOfStatement : for ( LeftHandSideExpression of AssignmentExpression ) Statement

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation

    // We can skip step 1, 2 and 4 here (on top of already skipping step 6 and 7).
    // 3. Let exprRef be the result of evaluating expr.
    // 5. Let exprValue be ? GetValue(exprRef).
    state.rhs_value = rhs.execute(interpreter, global_object);
    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());
    return state;
}

Value ForInStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto for_in_head_state = TRY_OR_DISCARD(for_in_of_head_execute(interpreter, global_object, m_lhs, *m_rhs));

    auto rhs_result = for_in_head_state.rhs_value;

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
    if (rhs_result.is_nullish())
        return js_undefined();

    auto* object = MUST(rhs_result.to_object(global_object));

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });

    auto last_value = js_undefined();
    while (object) {
        auto property_names = TRY_OR_DISCARD(object->enumerable_own_property_names(Object::PropertyKind::Key));
        for (auto& value : property_names) {
            TRY_OR_DISCARD(for_in_head_state.execute_head(interpreter, global_object, value));
            last_value = m_body->execute(interpreter, global_object).value_or(last_value);
            interpreter.vm().running_execution_context().lexical_environment = old_environment;
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                    interpreter.vm().stop_unwind();
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                    interpreter.vm().stop_unwind();
                    break;
                } else {
                    return last_value;
                }
            }
        }
        object = TRY_OR_DISCARD(object->internal_get_prototype_of());
    }
    return last_value;
}

Value ForOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto for_of_head_state = TRY_OR_DISCARD(for_in_of_head_execute(interpreter, global_object, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;
    auto last_value = js_undefined();

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // We use get_iterator_values which behaves like ForIn/OfBodyEvaluation with iteratorKind iterate.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });

    TRY_OR_DISCARD(get_iterator_values(global_object, rhs_result, [&](Value value) -> Optional<Completion> {
        TRY(for_of_head_state.execute_head(interpreter, global_object, value));
        last_value = m_body->execute(interpreter, global_object).value_or(last_value);
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                interpreter.vm().stop_unwind();
                return normal_completion(last_value);
            } else {
                return normal_completion(last_value);
            }
        }
        return {};
    }));

    return last_value;
}

Value ForAwaitOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
    // Note: Performs only steps 1 through 5.
    auto for_of_head_state = TRY_OR_DISCARD(for_in_of_head_execute(interpreter, global_object, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;

    // NOTE: Perform step 7 from ForIn/OfHeadEvaluation. And since this is always async we only have to do step 7.d.
    // d. Return ? GetIterator(exprValue, iteratorHint).
    auto* iterator = TRY_OR_DISCARD(get_iterator(global_object, rhs_result, IteratorHint::Async));
    VERIFY(iterator);

    auto& vm = interpreter.vm();

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // NOTE: Here iteratorKind is always async.
    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });
    // 3. Let V be undefined.
    auto last_value = js_undefined();

    // NOTE: Step 4 and 5 are just extracting properties from the head which is done already in for_in_of_head_execute.
    //       And these are only used in step 6.g through 6.k which is done with for_of_head_state.execute_head.

    // 6. Repeat,
    while (true) {
        // NOTE: Since we don't have iterator records yet we have to extract the function first.
        auto next_method = TRY_OR_DISCARD(iterator->get(vm.names.next));
        if (!next_method.is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::IterableNextNotAFunction);
            return {};
        }

        // a. Let nextResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
        auto next_result = TRY_OR_DISCARD(call(global_object, next_method, iterator));
        // b. If iteratorKind is async, set nextResult to ? Await(nextResult).
        next_result = TRY_OR_DISCARD(await(global_object, next_result));
        // c. If Type(nextResult) is not Object, throw a TypeError exception.
        if (!next_result.is_object()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::IterableNextBadReturn);
            return {};
        }

        // d. Let done be ? IteratorComplete(nextResult).
        auto done = TRY_OR_DISCARD(iterator_complete(global_object, next_result.as_object()));

        // e. If done is true, return NormalCompletion(V).
        if (done)
            return last_value;

        // f. Let nextValue be ? IteratorValue(nextResult).
        auto next_value = TRY_OR_DISCARD(iterator_value(global_object, next_result.as_object()));

        // NOTE: This performs steps g. through to k.
        TRY_OR_DISCARD(for_of_head_state.execute_head(interpreter, global_object, next_value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter, global_object);

        // m. Set the running execution context's LexicalEnvironment to oldEnv.
        interpreter.vm().running_execution_context().lexical_environment = old_environment;

        // NOTE: Since execute does not return a completion we have to have a number of checks here.
        // n. If LoopContinues(result, labelSet) is false, then
        if (auto* exception = vm.exception()) {
            // FIXME: We should return the result of AsyncIteratorClose but cannot return completions yet.
            // 3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
            TRY_OR_DISCARD(async_iterator_close(*iterator, throw_completion(exception->value())));
            return {};
        }

        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                // NOTE: In this case LoopContinues is not actually false so we don't perform step 6.n.ii.3.
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                interpreter.vm().stop_unwind();
                // 2. Set status to UpdateEmpty(result, V).
                if (!result.is_empty())
                    last_value = result;
                // 3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
                TRY_OR_DISCARD(async_iterator_close(*iterator, normal_completion(last_value)));
                return last_value;
            } else {
                // 2. Set status to UpdateEmpty(result, V).
                if (!result.is_empty())
                    last_value = result;
                // 3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
                TRY_OR_DISCARD(async_iterator_close(*iterator, normal_completion(last_value)));
                return last_value;
            }
        }
        // o. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (!result.is_empty())
            last_value = result;
    }

    VERIFY_NOT_REACHED();
}

Value BinaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Special case in which we cannot execute the lhs.  RelationalExpression : PrivateIdentifier in ShiftExpression
    //  RelationalExpression : PrivateIdentifier in ShiftExpression, https://tc39.es/ecma262/#sec-relational-operators-runtime-semantics-evaluation
    if (m_op == BinaryOp::In && is<PrivateIdentifier>(*m_lhs)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_lhs).string();

        auto rhs_result = m_rhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!rhs_result.is_object()) {
            interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::InOperatorWithObject);
            return {};
        }
        auto* private_environment = interpreter.vm().running_execution_context().private_environment;
        VERIFY(private_environment);
        auto private_name = private_environment->resolve_private_identifier(private_identifier);
        return Value(rhs_result.as_object().private_element_find(private_name) != nullptr);
    }

    auto lhs_result = m_lhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case BinaryOp::Addition:
        return TRY_OR_DISCARD(add(global_object, lhs_result, rhs_result));
    case BinaryOp::Subtraction:
        return TRY_OR_DISCARD(sub(global_object, lhs_result, rhs_result));
    case BinaryOp::Multiplication:
        return TRY_OR_DISCARD(mul(global_object, lhs_result, rhs_result));
    case BinaryOp::Division:
        return TRY_OR_DISCARD(div(global_object, lhs_result, rhs_result));
    case BinaryOp::Modulo:
        return TRY_OR_DISCARD(mod(global_object, lhs_result, rhs_result));
    case BinaryOp::Exponentiation:
        return TRY_OR_DISCARD(exp(global_object, lhs_result, rhs_result));
    case BinaryOp::StrictlyEquals:
        return Value(is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::StrictlyInequals:
        return Value(!is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::LooselyEquals:
        return Value(TRY_OR_DISCARD(is_loosely_equal(global_object, lhs_result, rhs_result)));
    case BinaryOp::LooselyInequals:
        return Value(!TRY_OR_DISCARD(is_loosely_equal(global_object, lhs_result, rhs_result)));
    case BinaryOp::GreaterThan:
        return TRY_OR_DISCARD(greater_than(global_object, lhs_result, rhs_result));
    case BinaryOp::GreaterThanEquals:
        return TRY_OR_DISCARD(greater_than_equals(global_object, lhs_result, rhs_result));
    case BinaryOp::LessThan:
        return TRY_OR_DISCARD(less_than(global_object, lhs_result, rhs_result));
    case BinaryOp::LessThanEquals:
        return TRY_OR_DISCARD(less_than_equals(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseAnd:
        return TRY_OR_DISCARD(bitwise_and(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseOr:
        return TRY_OR_DISCARD(bitwise_or(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseXor:
        return TRY_OR_DISCARD(bitwise_xor(global_object, lhs_result, rhs_result));
    case BinaryOp::LeftShift:
        return TRY_OR_DISCARD(left_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::RightShift:
        return TRY_OR_DISCARD(right_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::UnsignedRightShift:
        return TRY_OR_DISCARD(unsigned_right_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::In:
        return TRY_OR_DISCARD(in(global_object, lhs_result, rhs_result));
    case BinaryOp::InstanceOf:
        return TRY_OR_DISCARD(instance_of(global_object, lhs_result, rhs_result));
    }

    VERIFY_NOT_REACHED();
}

Value LogicalExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto lhs_result = m_lhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case LogicalOp::And:
        if (lhs_result.to_boolean()) {
            auto rhs_result = m_rhs->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            return rhs_result;
        }
        return lhs_result;
    case LogicalOp::Or: {
        if (lhs_result.to_boolean())
            return lhs_result;
        auto rhs_result = m_rhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        return rhs_result;
    }
    case LogicalOp::NullishCoalescing:
        if (lhs_result.is_nullish()) {
            auto rhs_result = m_rhs->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            return rhs_result;
        }
        return lhs_result;
    }

    VERIFY_NOT_REACHED();
}

Reference Expression::to_reference(Interpreter&, GlobalObject&) const
{
    return {};
}

Reference Identifier::to_reference(Interpreter& interpreter, GlobalObject&) const
{
    if (m_cached_environment_coordinate.has_value()) {
        auto* environment = interpreter.vm().running_execution_context().lexical_environment;
        for (size_t i = 0; i < m_cached_environment_coordinate->hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            return Reference { *environment, string(), interpreter.vm().in_strict_mode(), m_cached_environment_coordinate };
        }
        m_cached_environment_coordinate = {};
    }

    auto reference = interpreter.vm().resolve_binding(string());
    if (reference.environment_coordinate().has_value())
        m_cached_environment_coordinate = reference.environment_coordinate();
    return reference;
}

Reference MemberExpression::to_reference(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 13.3.7.1 Runtime Semantics: Evaluation
    // SuperProperty : super [ Expression ]
    // SuperProperty : super . IdentifierName
    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    if (is<SuperExpression>(object())) {
        // 1. Let env be GetThisEnvironment().
        auto& environment = get_this_environment(interpreter.vm());
        // 2. Let actualThis be ? env.GetThisBinding().
        auto actual_this = TRY_OR_DISCARD(environment.get_this_binding(global_object));

        PropertyKey property_key;

        if (is_computed()) {
            // SuperProperty : super [ Expression ]

            // 3. Let propertyNameReference be the result of evaluating Expression.
            // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
            auto property_name_value = m_property->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            property_key = TRY_OR_DISCARD(property_name_value.to_property_key(global_object));
        } else {
            // SuperProperty : super . IdentifierName

            // 3. Let propertyKey be StringValue of IdentifierName.
            VERIFY(is<Identifier>(property()));
            property_key = static_cast<Identifier const&>(property()).string();
        }

        // 6. If the code matched by this SuperProperty is strict mode code, let strict be true; else let strict be false.
        bool strict = interpreter.vm().in_strict_mode();

        // 7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).
        return TRY_OR_DISCARD(make_super_property_reference(global_object, actual_this, property_key, strict));
    }

    auto base_reference = m_object->to_reference(interpreter, global_object);
    if (interpreter.exception())
        return {};

    Value base_value;

    if (base_reference.is_valid_reference())
        base_value = TRY_OR_DISCARD(base_reference.get_value(global_object));
    else
        base_value = m_object->execute(interpreter, global_object);

    if (interpreter.exception())
        return {};
    VERIFY(!base_value.is_empty());

    // From here on equivalent to
    // 13.3.4 EvaluatePropertyAccessWithIdentifierKey ( baseValue, identifierName, strict ), https://tc39.es/ecma262/#sec-evaluate-property-access-with-identifier-key
    PropertyKey property_name;
    if (is_computed()) {
        // Weird order which I can't quite find from the specs.
        auto value = m_property->execute(interpreter, global_object);
        if (interpreter.exception())
            return Reference {};

        TRY_OR_DISCARD(require_object_coercible(global_object, base_value));

        VERIFY(!value.is_empty());
        property_name = PropertyKey::from_value(global_object, value);
        if (interpreter.exception())
            return Reference {};
    } else if (is<PrivateIdentifier>(*m_property)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_property);
        return make_private_reference(interpreter.vm(), base_value, private_identifier.string());
    } else {
        property_name = verify_cast<Identifier>(*m_property).string();
        TRY_OR_DISCARD(require_object_coercible(global_object, base_value));
    }
    if (!property_name.is_valid())
        return Reference {};

    auto strict = interpreter.vm().in_strict_mode();
    return Reference { base_value, move(property_name), {}, strict };
}

Value UnaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    if (m_op == UnaryOp::Delete) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception())
            return {};
        return Value(TRY_OR_DISCARD(reference.delete_(global_object)));
    }

    Value lhs_result;
    if (m_op == UnaryOp::Typeof && is<Identifier>(*m_lhs)) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception())
            return {};

        if (reference.is_unresolvable())
            lhs_result = js_undefined();
        else
            lhs_result = TRY_OR_DISCARD(reference.get_value(global_object));
        VERIFY(!lhs_result.is_empty());
    } else {
        lhs_result = m_lhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
    }

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        return TRY_OR_DISCARD(bitwise_not(global_object, lhs_result));
    case UnaryOp::Not:
        return Value(!lhs_result.to_boolean());
    case UnaryOp::Plus:
        return TRY_OR_DISCARD(unary_plus(global_object, lhs_result));
    case UnaryOp::Minus:
        return TRY_OR_DISCARD(unary_minus(global_object, lhs_result));
    case UnaryOp::Typeof:
        return js_string(vm, lhs_result.typeof());
    case UnaryOp::Void:
        return js_undefined();
    case UnaryOp::Delete:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

Value SuperExpression::execute(Interpreter&, GlobalObject&) const
{
    // The semantics for SuperExpression are handled in CallExpression and SuperCall.
    VERIFY_NOT_REACHED();
}

Value ClassElement::execute(Interpreter&, GlobalObject&) const
{
    // Note: The semantics of class element are handled in class_element_evaluation
    VERIFY_NOT_REACHED();
}

static ThrowCompletionOr<ClassElement::ClassElementName> class_key_to_property_name(Interpreter& interpreter, GlobalObject& global_object, Expression const& key)
{
    if (is<PrivateIdentifier>(key)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(key);
        auto* private_environment = interpreter.vm().running_execution_context().private_environment;
        VERIFY(private_environment);
        return ClassElement::ClassElementName { private_environment->resolve_private_identifier(private_identifier.string()) };
    }

    auto prop_key = key.execute(interpreter, global_object);
    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    if (prop_key.is_object())
        prop_key = TRY(prop_key.to_primitive(global_object, Value::PreferredType::String));

    auto property_key = PropertyKey::from_value(global_object, prop_key);
    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());
    return ClassElement::ClassElementName { property_key };
}

// 15.4.5 Runtime Semantics: MethodDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-methoddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassMethod::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& target) const
{
    auto property_key = TRY(class_key_to_property_name(interpreter, global_object, *m_key));

    auto method_value = m_function->execute(interpreter, global_object);
    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    auto& method_function = static_cast<ECMAScriptFunctionObject&>(method_value.as_function());
    method_function.set_home_object(&target);

    auto set_function_name = [&](String prefix = "") {
        auto property_name = property_key.visit(
            [&](PropertyKey const& property_name) -> String {
                if (property_name.is_symbol()) {
                    auto description = property_name.as_symbol()->description();
                    if (description.is_empty())
                        return "";
                    return String::formatted("[{}]", description);
                } else {
                    return property_name.to_string();
                }
            },
            [&](PrivateName const& private_name) -> String {
                return private_name.description;
            });

        update_function_name(method_value, String::formatted("{}{}{}", prefix, prefix.is_empty() ? "" : " ", property_name));
    };

    if (property_key.has<PropertyKey>()) {
        auto& property_name = property_key.get<PropertyKey>();
        switch (kind()) {
        case ClassMethod::Kind::Method:
            set_function_name();
            TRY(target.define_property_or_throw(property_name, { .value = method_value, .writable = true, .enumerable = false, .configurable = true }));
            break;
        case ClassMethod::Kind::Getter:
            set_function_name("get");
            TRY(target.define_property_or_throw(property_name, { .get = &method_function, .enumerable = true, .configurable = true }));
            break;
        case ClassMethod::Kind::Setter:
            set_function_name("set");
            TRY(target.define_property_or_throw(property_name, { .set = &method_function, .enumerable = true, .configurable = true }));
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        return ClassValue { normal_completion({}) };
    } else {
        auto& private_name = property_key.get<PrivateName>();
        switch (kind()) {
        case Kind::Method:
            set_function_name();
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Method, method_value } };
        case Kind::Getter:
            set_function_name("get");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, Accessor::create(interpreter.vm(), &method_function, nullptr) } };
        case Kind::Setter:
            set_function_name("set");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, Accessor::create(interpreter.vm(), nullptr, &method_function) } };
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

// We use this class to mimic  Initializer : = AssignmentExpression of
// 10.2.1.3 Runtime Semantics: EvaluateBody, https://tc39.es/ecma262/#sec-runtime-semantics-evaluatebody
class ClassFieldInitializerStatement : public Statement {
public:
    ClassFieldInitializerStatement(SourceRange source_range, NonnullRefPtr<Expression> expression, FlyString field_name)
        : Statement(source_range)
        , m_expression(move(expression))
        , m_class_field_identifier_name(move(field_name))
    {
    }

    Value execute(Interpreter& interpreter, GlobalObject& global_object) const override
    {
        VERIFY(interpreter.vm().argument_count() == 0);
        VERIFY(!m_class_field_identifier_name.is_empty());
        return TRY_OR_DISCARD(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_expression, m_class_field_identifier_name));
    }

    void dump(int) const override
    {
        // This should not be dumped as it is never part of an actual AST.
        VERIFY_NOT_REACHED();
    }

private:
    NonnullRefPtr<Expression> m_expression;
    FlyString m_class_field_identifier_name; // [[ClassFieldIdentifierName]]
};

// 15.7.10 Runtime Semantics: ClassFieldDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classfielddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassField::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& target) const
{
    auto property_key = TRY(class_key_to_property_name(interpreter, global_object, *m_key));
    ECMAScriptFunctionObject* initializer = nullptr;
    if (m_initializer) {
        auto copy_initializer = m_initializer;
        auto name = property_key.visit(
            [&](PropertyKey const& property_name) -> String {
                return property_name.is_number() ? property_name.to_string() : property_name.to_string_or_symbol().to_display_string();
            },
            [&](PrivateName const& private_name) -> String {
                return private_name.description;
            });

        // FIXME: A potential optimization is not creating the functions here since these are never directly accessible.
        auto function_code = create_ast_node<ClassFieldInitializerStatement>(m_initializer->source_range(), copy_initializer.release_nonnull(), name);
        initializer = ECMAScriptFunctionObject::create(interpreter.global_object(), String::empty(), *function_code, {}, 0, interpreter.lexical_environment(), interpreter.vm().running_execution_context().private_environment, FunctionKind::Regular, true, false, m_contains_direct_call_to_eval, false);
        initializer->set_home_object(&target);
    }

    return ClassValue {
        ClassFieldDefinition {
            property_key,
            initializer,
        }
    };
}

static Optional<FlyString> nullopt_or_private_identifier_description(Expression const& expression)
{
    if (is<PrivateIdentifier>(expression))
        return static_cast<PrivateIdentifier const&>(expression).string();
    return {};
}

Optional<FlyString> ClassField::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

Optional<FlyString> ClassMethod::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

// 15.7.11 Runtime Semantics: ClassStaticBlockDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classstaticblockdefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> StaticInitializer::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& home_object) const
{
    auto* lexical_environment = interpreter.vm().running_execution_context().lexical_environment;
    auto* private_scope = interpreter.vm().running_execution_context().private_environment;

    // Note: The function bodyFunction is never directly accessible to ECMAScript code.
    auto* body_function = ECMAScriptFunctionObject::create(global_object, "", *m_function_body, {}, 0, lexical_environment, private_scope, FunctionKind::Regular, true, false, m_contains_direct_call_to_eval, false);
    body_function->set_home_object(&home_object);

    return ClassValue { normal_completion(body_function) };
}

Value ClassExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // FIXME: Set value.[[SourceText]] to the source text matched by ClassExpression.
    return TRY_OR_DISCARD(class_definition_evaluation(interpreter, global_object, m_name, m_name.is_null() ? "" : m_name));
}

Value ClassDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto name = m_class_expression->name();
    VERIFY(!name.is_empty());
    auto class_constructor = TRY_OR_DISCARD(m_class_expression->class_definition_evaluation(interpreter, global_object, name, name));

    if (interpreter.lexical_environment()) {
        MUST(interpreter.lexical_environment()->initialize_binding(global_object, name, class_constructor));
    } else {
        auto reference = interpreter.vm().resolve_binding(name);
        TRY_OR_DISCARD(reference.put_value(global_object, class_constructor));
    }

    return {};
}

// 15.7.14 Runtime Semantics: ClassDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classdefinitionevaluation
ThrowCompletionOr<Value> ClassExpression::class_definition_evaluation(Interpreter& interpreter, GlobalObject& global_object, FlyString const& binding_name, FlyString const& class_name) const
{
    auto& vm = interpreter.vm();
    auto* environment = vm.lexical_environment();
    VERIFY(environment);
    auto* class_scope = new_declarative_environment(*environment);

    // We might not set the lexical environment but we always want to restore it eventually.
    ArmedScopeGuard restore_environment = [&] {
        vm.running_execution_context().lexical_environment = environment;
    };

    if (!binding_name.is_null())
        MUST(class_scope->create_immutable_binding(global_object, binding_name, true));

    auto* outer_private_environment = vm.running_execution_context().private_environment;
    auto* class_private_environment = new_private_environment(vm, outer_private_environment);

    for (auto const& element : m_elements) {
        auto opt_private_name = element.private_bound_identifier();
        if (opt_private_name.has_value())
            class_private_environment->add_private_name({}, opt_private_name.release_value());
    }

    auto* proto_parent = vm.current_realm()->global_object().object_prototype();

    auto* constructor_parent = vm.current_realm()->global_object().function_prototype();

    if (!m_super_class.is_null()) {
        vm.running_execution_context().lexical_environment = class_scope;

        // Note: Since our execute does evaluation and GetValue in once we must check for a valid reference first

        Value super_class;

        auto reference = m_super_class->to_reference(interpreter, global_object);
        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());

        if (reference.is_valid_reference()) {
            super_class = TRY(reference.get_value(global_object));
        } else {
            super_class = m_super_class->execute(interpreter, global_object);
            if (auto* exception = interpreter.exception())
                return throw_completion(exception->value());
        }
        vm.running_execution_context().lexical_environment = environment;

        if (super_class.is_null()) {
            proto_parent = nullptr;
        } else if (!super_class.is_constructor()) {
            return vm.throw_completion<TypeError>(global_object, ErrorType::ClassExtendsValueNotAConstructorOrNull, super_class.to_string_without_side_effects());
        } else {
            auto super_class_prototype = TRY(super_class.get(global_object, vm.names.prototype));
            if (!super_class_prototype.is_null() && !super_class_prototype.is_object())
                return vm.throw_completion<TypeError>(global_object, ErrorType::ClassExtendsValueInvalidPrototype, super_class_prototype.to_string_without_side_effects());

            if (super_class_prototype.is_null())
                proto_parent = nullptr;
            else
                proto_parent = &super_class_prototype.as_object();

            constructor_parent = &super_class.as_object();
        }
    }

    auto* prototype = Object::create(global_object, proto_parent);
    VERIFY(prototype);

    vm.running_execution_context().lexical_environment = class_scope;
    vm.running_execution_context().private_environment = class_private_environment;
    ScopeGuard restore_private_environment = [&] {
        vm.running_execution_context().private_environment = outer_private_environment;
    };

    // FIXME: Step 14.a is done in the parser. But maybe it shouldn't?
    Value class_constructor_value = m_constructor->execute(interpreter, global_object);
    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    update_function_name(class_constructor_value, class_name);

    VERIFY(class_constructor_value.is_function() && is<ECMAScriptFunctionObject>(class_constructor_value.as_function()));
    auto* class_constructor = static_cast<ECMAScriptFunctionObject*>(&class_constructor_value.as_function());
    class_constructor->set_home_object(prototype);
    class_constructor->set_is_class_constructor();
    class_constructor->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    TRY(class_constructor->internal_set_prototype_of(constructor_parent));

    if (!m_super_class.is_null())
        class_constructor->set_constructor_kind(ECMAScriptFunctionObject::ConstructorKind::Derived);

    prototype->define_direct_property(vm.names.constructor, class_constructor, Attribute::Writable | Attribute::Configurable);

    using StaticElement = Variant<ClassElement::ClassFieldDefinition, ECMAScriptFunctionObject*>;

    Vector<PrivateElement> static_private_methods;
    Vector<PrivateElement> instance_private_methods;
    Vector<ClassElement::ClassFieldDefinition> instance_fields;
    Vector<StaticElement> static_elements;

    for (auto const& element : m_elements) {
        // Note: All ClassElementEvaluation start with evaluating the name (or we fake it).
        auto element_value = TRY(element.class_element_evaluation(interpreter, global_object, element.is_static() ? *class_constructor : *prototype));

        if (element_value.has<PrivateElement>()) {
            auto& container = element.is_static() ? static_private_methods : instance_private_methods;

            auto& private_element = element_value.get<PrivateElement>();

            auto added_to_existing = false;
            // FIXME: We can skip this loop in most cases.
            for (auto& existing : container) {
                if (existing.key == private_element.key) {
                    VERIFY(existing.kind == PrivateElement::Kind::Accessor);
                    VERIFY(private_element.kind == PrivateElement::Kind::Accessor);
                    auto& accessor = private_element.value.as_accessor();
                    if (!accessor.getter())
                        existing.value.as_accessor().set_setter(accessor.setter());
                    else
                        existing.value.as_accessor().set_getter(accessor.getter());
                    added_to_existing = true;
                }
            }

            if (!added_to_existing)
                container.append(move(element_value.get<PrivateElement>()));
        } else if (auto* class_field_definition_ptr = element_value.get_pointer<ClassElement::ClassFieldDefinition>()) {
            if (element.is_static())
                static_elements.append(move(*class_field_definition_ptr));
            else
                instance_fields.append(move(*class_field_definition_ptr));
        } else if (element.class_element_kind() == ClassElement::ElementKind::StaticInitializer) {
            // We use Completion to hold the ClassStaticBlockDefinition Record.
            VERIFY(element_value.has<Completion>() && element_value.get<Completion>().has_value());
            auto element_object = element_value.get<Completion>().value();
            VERIFY(is<ECMAScriptFunctionObject>(element_object.as_object()));
            static_elements.append(static_cast<ECMAScriptFunctionObject*>(&element_object.as_object()));
        }
    }

    vm.running_execution_context().lexical_environment = environment;
    restore_environment.disarm();

    if (!binding_name.is_null())
        MUST(class_scope->initialize_binding(global_object, binding_name, class_constructor));

    for (auto& field : instance_fields)
        class_constructor->add_field(field.name, field.initializer);

    for (auto& private_method : instance_private_methods)
        class_constructor->add_private_method(private_method);

    for (auto& method : static_private_methods)
        class_constructor->private_method_or_accessor_add(move(method));

    for (auto& element : static_elements) {
        TRY(element.visit(
            [&](ClassElement::ClassFieldDefinition const& field) -> ThrowCompletionOr<void> {
                return TRY(class_constructor->define_field(field.name, field.initializer));
            },
            [&](ECMAScriptFunctionObject* static_block_function) -> ThrowCompletionOr<void> {
                // We discard any value returned here.
                TRY(call(global_object, static_block_function, class_constructor_value));
                return {};
            }));
    }

    return Value(class_constructor);
}

static void print_indent(int indent)
{
    out("{}", String::repeated(' ', indent * 2));
}

void ASTNode::dump(int indent) const
{
    print_indent(indent);
    outln("{}", class_name());
}

void ScopeNode::dump(int indent) const
{
    ASTNode::dump(indent);
    if (!m_lexical_declarations.is_empty()) {
        print_indent(indent + 1);
        outln("(Lexical declarations)");
        for (auto& declaration : m_lexical_declarations)
            declaration.dump(indent + 2);
    }

    if (!m_var_declarations.is_empty()) {
        print_indent(indent + 1);
        outln("(Variable declarations)");
        for (auto& declaration : m_var_declarations)
            declaration.dump(indent + 2);
    }

    if (!m_functions_hoistable_with_annexB_extension.is_empty()) {
        print_indent(indent + 1);
        outln("(Hoisted functions via annexB extension)");
        for (auto& declaration : m_functions_hoistable_with_annexB_extension)
            declaration.dump(indent + 2);
    }

    if (!m_children.is_empty()) {
        print_indent(indent + 1);
        outln("(Children)");
        for (auto& child : children())
            child.dump(indent + 2);
    }
}

void BinaryExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case BinaryOp::Addition:
        op_string = "+";
        break;
    case BinaryOp::Subtraction:
        op_string = "-";
        break;
    case BinaryOp::Multiplication:
        op_string = "*";
        break;
    case BinaryOp::Division:
        op_string = "/";
        break;
    case BinaryOp::Modulo:
        op_string = "%";
        break;
    case BinaryOp::Exponentiation:
        op_string = "**";
        break;
    case BinaryOp::StrictlyEquals:
        op_string = "===";
        break;
    case BinaryOp::StrictlyInequals:
        op_string = "!==";
        break;
    case BinaryOp::LooselyEquals:
        op_string = "==";
        break;
    case BinaryOp::LooselyInequals:
        op_string = "!=";
        break;
    case BinaryOp::GreaterThan:
        op_string = ">";
        break;
    case BinaryOp::GreaterThanEquals:
        op_string = ">=";
        break;
    case BinaryOp::LessThan:
        op_string = "<";
        break;
    case BinaryOp::LessThanEquals:
        op_string = "<=";
        break;
    case BinaryOp::BitwiseAnd:
        op_string = "&";
        break;
    case BinaryOp::BitwiseOr:
        op_string = "|";
        break;
    case BinaryOp::BitwiseXor:
        op_string = "^";
        break;
    case BinaryOp::LeftShift:
        op_string = "<<";
        break;
    case BinaryOp::RightShift:
        op_string = ">>";
        break;
    case BinaryOp::UnsignedRightShift:
        op_string = ">>>";
        break;
    case BinaryOp::In:
        op_string = "in";
        break;
    case BinaryOp::InstanceOf:
        op_string = "instanceof";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_rhs->dump(indent + 1);
}

void LogicalExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case LogicalOp::And:
        op_string = "&&";
        break;
    case LogicalOp::Or:
        op_string = "||";
        break;
    case LogicalOp::NullishCoalescing:
        op_string = "??";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_rhs->dump(indent + 1);
}

void UnaryExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case UnaryOp::BitwiseNot:
        op_string = "~";
        break;
    case UnaryOp::Not:
        op_string = "!";
        break;
    case UnaryOp::Plus:
        op_string = "+";
        break;
    case UnaryOp::Minus:
        op_string = "-";
        break;
    case UnaryOp::Typeof:
        op_string = "typeof ";
        break;
    case UnaryOp::Void:
        op_string = "void ";
        break;
    case UnaryOp::Delete:
        op_string = "delete ";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    print_indent(indent + 1);
    outln("{}", op_string);
    m_lhs->dump(indent + 1);
}

void CallExpression::dump(int indent) const
{
    print_indent(indent);
    if (is<NewExpression>(*this))
        outln("CallExpression [new]");
    else
        outln("CallExpression");
    m_callee->dump(indent + 1);
    for (auto& argument : m_arguments)
        argument.value->dump(indent + 1);
}

void SuperCall::dump(int indent) const
{
    print_indent(indent);
    outln("SuperCall");
    for (auto& argument : m_arguments)
        argument.value->dump(indent + 1);
}

void ClassDeclaration::dump(int indent) const
{
    ASTNode::dump(indent);
    m_class_expression->dump(indent + 1);
}

void ClassDeclaration::for_each_bound_name(IteratorOrVoidFunction<FlyString const&> callback) const
{
    if (!m_class_expression->name().is_empty())
        callback(m_class_expression->name());
}

void ClassExpression::dump(int indent) const
{
    print_indent(indent);
    outln("ClassExpression: \"{}\"", m_name);

    print_indent(indent);
    outln("(Constructor)");
    m_constructor->dump(indent + 1);

    if (!m_super_class.is_null()) {
        print_indent(indent);
        outln("(Super Class)");
        m_super_class->dump(indent + 1);
    }

    print_indent(indent);
    outln("(Elements)");
    for (auto& method : m_elements)
        method.dump(indent + 1);
}

void ClassMethod::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("(Key)");
    m_key->dump(indent + 1);

    const char* kind_string = nullptr;
    switch (m_kind) {
    case Kind::Method:
        kind_string = "Method";
        break;
    case Kind::Getter:
        kind_string = "Getter";
        break;
    case Kind::Setter:
        kind_string = "Setter";
        break;
    }
    print_indent(indent);
    outln("Kind: {}", kind_string);

    print_indent(indent);
    outln("Static: {}", is_static());

    print_indent(indent);
    outln("(Function)");
    m_function->dump(indent + 1);
}

void ClassField::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Key)");
    m_key->dump(indent + 1);

    print_indent(indent);
    outln("Static: {}", is_static());

    if (m_initializer) {
        print_indent(indent);
        outln("(Initializer)");
        m_initializer->dump(indent + 1);
    }
}

void StaticInitializer::dump(int indent) const
{
    ASTNode::dump(indent);
    m_function_body->dump(indent + 1);
}

void StringLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("StringLiteral \"{}\"", m_value);
}

void SuperExpression::dump(int indent) const
{
    print_indent(indent);
    outln("super");
}

void NumericLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("NumericLiteral {}", m_value);
}

void BigIntLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("BigIntLiteral {}", m_value);
}

void BooleanLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("BooleanLiteral {}", m_value);
}

void NullLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("null");
}

bool BindingPattern::contains_expression() const
{
    for (auto& entry : entries) {
        if (entry.initializer)
            return true;
        if (auto binding_ptr = entry.alias.get_pointer<NonnullRefPtr<BindingPattern>>(); binding_ptr && (*binding_ptr)->contains_expression())
            return true;
    }
    return false;
}

void BindingPattern::dump(int indent) const
{
    print_indent(indent);
    outln("BindingPattern {}", kind == Kind::Array ? "Array" : "Object");

    for (auto& entry : entries) {
        print_indent(indent + 1);
        outln("(Property)");

        if (kind == Kind::Object) {
            print_indent(indent + 2);
            outln("(Identifier)");
            if (entry.name.has<NonnullRefPtr<Identifier>>()) {
                entry.name.get<NonnullRefPtr<Identifier>>()->dump(indent + 3);
            } else {
                entry.name.get<NonnullRefPtr<Expression>>()->dump(indent + 3);
            }
        } else if (entry.is_elision()) {
            print_indent(indent + 2);
            outln("(Elision)");
            continue;
        }

        print_indent(indent + 2);
        outln("(Pattern{})", entry.is_rest ? " rest=true" : "");
        if (entry.alias.has<NonnullRefPtr<Identifier>>()) {
            entry.alias.get<NonnullRefPtr<Identifier>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<BindingPattern>>()) {
            entry.alias.get<NonnullRefPtr<BindingPattern>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<MemberExpression>>()) {
            entry.alias.get<NonnullRefPtr<MemberExpression>>()->dump(indent + 3);
        } else {
            print_indent(indent + 3);
            outln("<empty>");
        }

        if (entry.initializer) {
            print_indent(indent + 2);
            outln("(Initializer)");
            entry.initializer->dump(indent + 3);
        }
    }
}

void FunctionNode::dump(int indent, String const& class_name) const
{
    print_indent(indent);
    auto is_async = m_kind == FunctionKind::Async || m_kind == FunctionKind::AsyncGenerator;
    auto is_generator = m_kind == FunctionKind::Generator || m_kind == FunctionKind::AsyncGenerator;
    outln("{}{}{} '{}'", class_name, is_async ? " async" : "", is_generator ? "*" : "", name());
    if (m_contains_direct_call_to_eval) {
        print_indent(indent + 1);
        outln("\033[31;1m(direct eval)\033[0m");
    }
    if (!m_parameters.is_empty()) {
        print_indent(indent + 1);
        outln("(Parameters)");

        for (auto& parameter : m_parameters) {
            print_indent(indent + 2);
            if (parameter.is_rest)
                out("...");
            parameter.binding.visit(
                [&](FlyString const& name) {
                    outln("{}", name);
                },
                [&](BindingPattern const& pattern) {
                    pattern.dump(indent + 2);
                });
            if (parameter.default_value)
                parameter.default_value->dump(indent + 3);
        }
    }
    print_indent(indent + 1);
    outln("(Body)");
    body().dump(indent + 2);
}

void FunctionDeclaration::dump(int indent) const
{
    FunctionNode::dump(indent, class_name());
}

void FunctionDeclaration::for_each_bound_name(IteratorOrVoidFunction<FlyString const&> callback) const
{
    if (!name().is_empty())
        callback(name());
}

void FunctionExpression::dump(int indent) const
{
    FunctionNode::dump(indent, class_name());
}

void YieldExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    if (argument())
        argument()->dump(indent + 1);
}

void AwaitExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    m_argument->dump(indent + 1);
}

void ReturnStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    if (argument())
        argument()->dump(indent + 1);
}

void IfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("If");
    predicate().dump(indent + 1);
    consequent().dump(indent + 1);
    if (alternate()) {
        print_indent(indent);
        outln("Else");
        alternate()->dump(indent + 1);
    }
}

void WhileStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("While");
    test().dump(indent + 1);
    body().dump(indent + 1);
}

void WithStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent + 1);
    outln("Object");
    object().dump(indent + 2);
    print_indent(indent + 1);
    outln("Body");
    body().dump(indent + 2);
}

void DoWhileStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("DoWhile");
    test().dump(indent + 1);
    body().dump(indent + 1);
}

void ForStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("For");
    if (init())
        init()->dump(indent + 1);
    if (test())
        test()->dump(indent + 1);
    if (update())
        update()->dump(indent + 1);
    body().dump(indent + 1);
}

void ForInStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForIn");
    lhs().visit([&](auto& lhs) { lhs->dump(indent + 1); });
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

void ForOfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForOf");
    lhs().visit([&](auto& lhs) { lhs->dump(indent + 1); });
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

void ForAwaitOfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForAwaitOf");
    m_lhs.visit([&](auto& lhs) { lhs->dump(indent + 1); });
    m_rhs->dump(indent + 1);
    m_body->dump(indent + 1);
}

Value Identifier::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto reference = to_reference(interpreter, global_object);
    if (interpreter.exception())
        return {};

    return TRY_OR_DISCARD(reference.get_value(global_object));
}

void Identifier::dump(int indent) const
{
    print_indent(indent);
    outln("Identifier \"{}\"", m_string);
}

Value PrivateIdentifier::execute(Interpreter&, GlobalObject&) const
{
    // Note: This should be handled by either the member expression this is part of
    //       or the binary expression in the case of `#foo in bar`.
    VERIFY_NOT_REACHED();
}

void PrivateIdentifier::dump(int indent) const
{
    print_indent(indent);
    outln("PrivateIdentifier \"{}\"", m_string);
}

void SpreadExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    m_target->dump(indent + 1);
}

Value SpreadExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_target->execute(interpreter, global_object);
}

Value ThisExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return interpreter.vm().resolve_this_binding(global_object);
}

void ThisExpression::dump(int indent) const
{
    ASTNode::dump(indent);
}

// 13.15.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-assignment-operators-runtime-semantics-evaluation
Value AssignmentExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_op == AssignmentOp::Assignment) {
        // AssignmentExpression : LeftHandSideExpression = AssignmentExpression
        return m_lhs.visit(
            [&](NonnullRefPtr<Expression>& lhs) -> JS::Value {
                auto reference = lhs->to_reference(interpreter, global_object);
                if (interpreter.exception())
                    return {};

                Value rhs_result;
                if (lhs->is_identifier()) {
                    auto& identifier_name = static_cast<Identifier const&>(*lhs).string();
                    rhs_result = TRY_OR_DISCARD(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_rhs, identifier_name));
                } else {
                    rhs_result = m_rhs->execute(interpreter, global_object);
                }
                if (interpreter.exception())
                    return {};

                TRY_OR_DISCARD(reference.put_value(global_object, rhs_result));

                return rhs_result;
            },
            [&](NonnullRefPtr<BindingPattern>& pattern) -> JS::Value {
                Value rhs_result = m_rhs->execute(interpreter, global_object);
                if (interpreter.exception())
                    return {};

                TRY_OR_DISCARD(interpreter.vm().destructuring_assignment_evaluation(pattern, rhs_result, global_object));

                return rhs_result;
            });
    }
    VERIFY(m_lhs.has<NonnullRefPtr<Expression>>());

    auto& lhs_expression = *m_lhs.get<NonnullRefPtr<Expression>>();
    auto reference = lhs_expression.to_reference(interpreter, global_object);
    if (interpreter.exception())
        return {};

    auto lhs_result = TRY_OR_DISCARD(reference.get_value(global_object));

    //  AssignmentExpression : LeftHandSideExpression {&&=, ||=, ??=} AssignmentExpression
    if (m_op == AssignmentOp::AndAssignment || m_op == AssignmentOp::OrAssignment || m_op == AssignmentOp::NullishAssignment) {
        switch (m_op) {
        case AssignmentOp::AndAssignment:
            if (!lhs_result.to_boolean())
                return lhs_result;
            break;
        case AssignmentOp::OrAssignment:
            if (lhs_result.to_boolean())
                return lhs_result;
            break;
        case AssignmentOp::NullishAssignment:
            if (!lhs_result.is_nullish())
                return lhs_result;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        Value rhs_result;
        if (lhs_expression.is_identifier()) {
            auto& identifier_name = static_cast<Identifier const&>(lhs_expression).string();
            rhs_result = TRY_OR_DISCARD(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_rhs, identifier_name));
        } else {
            rhs_result = m_rhs->execute(interpreter, global_object);

            if (interpreter.exception())
                return {};
        }

        TRY_OR_DISCARD(reference.put_value(global_object, rhs_result));

        return rhs_result;
    }

    // AssignmentExpression : LeftHandSideExpression AssignmentOperator AssignmentExpression
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case AssignmentOp::AdditionAssignment:
        rhs_result = TRY_OR_DISCARD(add(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::SubtractionAssignment:
        rhs_result = TRY_OR_DISCARD(sub(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::MultiplicationAssignment:
        rhs_result = TRY_OR_DISCARD(mul(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::DivisionAssignment:
        rhs_result = TRY_OR_DISCARD(div(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::ModuloAssignment:
        rhs_result = TRY_OR_DISCARD(mod(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::ExponentiationAssignment:
        rhs_result = TRY_OR_DISCARD(exp(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseAndAssignment:
        rhs_result = TRY_OR_DISCARD(bitwise_and(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseOrAssignment:
        rhs_result = TRY_OR_DISCARD(bitwise_or(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseXorAssignment:
        rhs_result = TRY_OR_DISCARD(bitwise_xor(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::LeftShiftAssignment:
        rhs_result = TRY_OR_DISCARD(left_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::RightShiftAssignment:
        rhs_result = TRY_OR_DISCARD(right_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        rhs_result = TRY_OR_DISCARD(unsigned_right_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::Assignment:
    case AssignmentOp::AndAssignment:
    case AssignmentOp::OrAssignment:
    case AssignmentOp::NullishAssignment:
        VERIFY_NOT_REACHED();
    }

    TRY_OR_DISCARD(reference.put_value(global_object, rhs_result));

    return rhs_result;
}

Value UpdateExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto reference = m_argument->to_reference(interpreter, global_object);

    if (interpreter.exception())
        return {};
    auto old_value = TRY_OR_DISCARD(reference.get_value(global_object));
    old_value = TRY_OR_DISCARD(old_value.to_numeric(global_object));

    Value new_value;
    switch (m_op) {
    case UpdateOp::Increment:
        if (old_value.is_number())
            new_value = Value(old_value.as_double() + 1);
        else
            new_value = js_bigint(interpreter.heap(), old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
        break;
    case UpdateOp::Decrement:
        if (old_value.is_number())
            new_value = Value(old_value.as_double() - 1);
        else
            new_value = js_bigint(interpreter.heap(), old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    TRY_OR_DISCARD(reference.put_value(global_object, new_value));
    return m_prefixed ? new_value : old_value;
}

void AssignmentExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case AssignmentOp::Assignment:
        op_string = "=";
        break;
    case AssignmentOp::AdditionAssignment:
        op_string = "+=";
        break;
    case AssignmentOp::SubtractionAssignment:
        op_string = "-=";
        break;
    case AssignmentOp::MultiplicationAssignment:
        op_string = "*=";
        break;
    case AssignmentOp::DivisionAssignment:
        op_string = "/=";
        break;
    case AssignmentOp::ModuloAssignment:
        op_string = "%=";
        break;
    case AssignmentOp::ExponentiationAssignment:
        op_string = "**=";
        break;
    case AssignmentOp::BitwiseAndAssignment:
        op_string = "&=";
        break;
    case AssignmentOp::BitwiseOrAssignment:
        op_string = "|=";
        break;
    case AssignmentOp::BitwiseXorAssignment:
        op_string = "^=";
        break;
    case AssignmentOp::LeftShiftAssignment:
        op_string = "<<=";
        break;
    case AssignmentOp::RightShiftAssignment:
        op_string = ">>=";
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        op_string = ">>>=";
        break;
    case AssignmentOp::AndAssignment:
        op_string = "&&=";
        break;
    case AssignmentOp::OrAssignment:
        op_string = "||=";
        break;
    case AssignmentOp::NullishAssignment:
        op_string = "\?\?=";
        break;
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_lhs.visit([&](auto& lhs) { lhs->dump(indent + 1); });
    m_rhs->dump(indent + 1);
}

void UpdateExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case UpdateOp::Increment:
        op_string = "++";
        break;
    case UpdateOp::Decrement:
        op_string = "--";
        break;
    }

    ASTNode::dump(indent);
    if (m_prefixed) {
        print_indent(indent + 1);
        outln("{}", op_string);
    }
    m_argument->dump(indent + 1);
    if (!m_prefixed) {
        print_indent(indent + 1);
        outln("{}", op_string);
    }
}

Value VariableDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    for (auto& declarator : m_declarations) {
        if (auto* init = declarator.init()) {
            TRY_OR_DISCARD(declarator.target().visit(
                [&](NonnullRefPtr<Identifier> const& id) -> ThrowCompletionOr<void> {
                    auto reference = id->to_reference(interpreter, global_object);
                    if (auto* exception = interpreter.exception())
                        return throw_completion(exception->value());
                    auto initializer_result = TRY_OR_DISCARD(interpreter.vm().named_evaluation_if_anonymous_function(global_object, *init, id->string()));
                    VERIFY(!initializer_result.is_empty());

                    if (m_declaration_kind == DeclarationKind::Var)
                        return reference.put_value(global_object, initializer_result);
                    else
                        return reference.initialize_referenced_binding(global_object, initializer_result);
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) -> ThrowCompletionOr<void> {
                    auto initializer_result = init->execute(interpreter, global_object);
                    if (auto* exception = interpreter.exception())
                        return throw_completion(exception->value());

                    Environment* environment = m_declaration_kind == DeclarationKind::Var ? nullptr : interpreter.lexical_environment();

                    return interpreter.vm().binding_initialization(pattern, initializer_result, environment, global_object);
                }));
        } else if (m_declaration_kind != DeclarationKind::Var) {
            VERIFY(declarator.target().has<NonnullRefPtr<Identifier>>());
            auto& identifier = declarator.target().get<NonnullRefPtr<Identifier>>();
            auto reference = identifier->to_reference(interpreter, global_object);
            TRY_OR_DISCARD(reference.initialize_referenced_binding(global_object, js_undefined()));
        }
    }
    return {};
}

Value VariableDeclarator::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: VariableDeclarator execution is handled by VariableDeclaration.
    VERIFY_NOT_REACHED();
}

void VariableDeclaration::for_each_bound_name(IteratorOrVoidFunction<FlyString const&> callback) const
{
    for (auto& entry : declarations()) {
        entry.target().template visit(
            [&](const NonnullRefPtr<Identifier>& id) {
                callback(id->string());
            },
            [&](const NonnullRefPtr<BindingPattern>& binding) {
                binding->for_each_bound_name([&](const auto& name) {
                    callback(name);
                });
            });
    }
}

void VariableDeclaration::dump(int indent) const
{
    const char* declaration_kind_string = nullptr;
    switch (m_declaration_kind) {
    case DeclarationKind::Let:
        declaration_kind_string = "Let";
        break;
    case DeclarationKind::Var:
        declaration_kind_string = "Var";
        break;
    case DeclarationKind::Const:
        declaration_kind_string = "Const";
        break;
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", declaration_kind_string);

    for (auto& declarator : m_declarations)
        declarator.dump(indent + 1);
}

void VariableDeclarator::dump(int indent) const
{
    ASTNode::dump(indent);
    m_target.visit([indent](const auto& value) { value->dump(indent + 1); });
    if (m_init)
        m_init->dump(indent + 1);
}

void ObjectProperty::dump(int indent) const
{
    ASTNode::dump(indent);

    if (m_property_type == Type::Spread) {
        print_indent(indent + 1);
        outln("...Spreading");
        m_key->dump(indent + 1);
    } else {
        m_key->dump(indent + 1);
        m_value->dump(indent + 1);
    }
}

void ObjectExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& property : m_properties) {
        property.dump(indent + 1);
    }
}

void ExpressionStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_expression->dump(indent + 1);
}

Value ObjectProperty::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: ObjectProperty execution is handled by ObjectExpression.
    VERIFY_NOT_REACHED();
}

Value ObjectExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto* object = Object::create(global_object, global_object.object_prototype());
    for (auto& property : m_properties) {
        auto key = property.key().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        if (property.type() == ObjectProperty::Type::Spread) {
            if (key.is_object() && is<Array>(key.as_object())) {
                auto& array_to_spread = static_cast<Array&>(key.as_object());
                for (auto& entry : array_to_spread.indexed_properties()) {
                    auto value = TRY_OR_DISCARD(array_to_spread.get(entry.index()));
                    object->indexed_properties().put(entry.index(), value);
                    if (interpreter.exception())
                        return {};
                }
            } else if (key.is_object()) {
                auto& obj_to_spread = key.as_object();

                for (auto& it : obj_to_spread.shape().property_table_ordered()) {
                    if (it.value.attributes.is_enumerable()) {
                        object->define_direct_property(it.key, TRY_OR_DISCARD(obj_to_spread.get(it.key)), JS::default_attributes);
                        if (interpreter.exception())
                            return {};
                    }
                }
            } else if (key.is_string()) {
                auto& str_to_spread = key.as_string().string();

                for (size_t i = 0; i < str_to_spread.length(); i++) {
                    object->define_direct_property(i, js_string(interpreter.heap(), str_to_spread.substring(i, 1)), JS::default_attributes);
                    if (interpreter.exception())
                        return {};
                }
            }
            continue;
        }

        auto value = property.value().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        if (value.is_function() && property.is_method())
            static_cast<ECMAScriptFunctionObject&>(value.as_function()).set_home_object(object);

        auto name = TRY_OR_DISCARD(get_function_name(global_object, key));
        if (property.type() == ObjectProperty::Type::Getter) {
            name = String::formatted("get {}", name);
        } else if (property.type() == ObjectProperty::Type::Setter) {
            name = String::formatted("set {}", name);
        }

        update_function_name(value, name);

        switch (property.type()) {
        case ObjectProperty::Type::Getter:
            VERIFY(value.is_function());
            object->define_direct_accessor(PropertyKey::from_value(global_object, key), &value.as_function(), nullptr, Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::Setter:
            VERIFY(value.is_function());
            object->define_direct_accessor(PropertyKey::from_value(global_object, key), nullptr, &value.as_function(), Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::KeyValue:
            object->define_direct_property(PropertyKey::from_value(global_object, key), value, JS::default_attributes);
            break;
        case ObjectProperty::Type::Spread:
        default:
            VERIFY_NOT_REACHED();
        }
        if (interpreter.exception())
            return {};
    }
    return object;
}

void MemberExpression::dump(int indent) const
{
    print_indent(indent);
    outln("{}(computed={})", class_name(), is_computed());
    m_object->dump(indent + 1);
    m_property->dump(indent + 1);
}

PropertyKey MemberExpression::computed_property_name(Interpreter& interpreter, GlobalObject& global_object) const
{
    if (!is_computed())
        return verify_cast<Identifier>(*m_property).string();

    auto value = m_property->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    VERIFY(!value.is_empty());
    return PropertyKey::from_value(global_object, value);
}

String MemberExpression::to_string_approximation() const
{
    String object_string = "<object>";
    if (is<Identifier>(*m_object))
        object_string = static_cast<Identifier const&>(*m_object).string();
    if (is_computed())
        return String::formatted("{}[<computed>]", object_string);
    return String::formatted("{}.{}", object_string, verify_cast<Identifier>(*m_property).string());
}

Value MemberExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto reference = to_reference(interpreter, global_object);
    if (interpreter.exception())
        return {};
    return TRY_OR_DISCARD(reference.get_value(global_object));
}

bool MemberExpression::ends_in_private_name() const
{
    if (is_computed())
        return false;
    if (is<PrivateIdentifier>(*m_property))
        return true;
    if (is<MemberExpression>(*m_property))
        return static_cast<MemberExpression const&>(*m_property).ends_in_private_name();
    return false;
}

void OptionalChain::dump(int indent) const
{
    print_indent(indent);
    outln("{}", class_name());
    m_base->dump(indent + 1);
    for (auto& reference : m_references) {
        reference.visit(
            [&](Call const& call) {
                print_indent(indent + 1);
                outln("Call({})", call.mode == Mode::Optional ? "Optional" : "Not Optional");
                for (auto& argument : call.arguments)
                    argument.value->dump(indent + 2);
            },
            [&](ComputedReference const& ref) {
                print_indent(indent + 1);
                outln("ComputedReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.expression->dump(indent + 2);
            },
            [&](MemberReference const& ref) {
                print_indent(indent + 1);
                outln("MemberReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.identifier->dump(indent + 2);
            },
            [&](PrivateMemberReference const& ref) {
                print_indent(indent + 1);
                outln("PrivateMemberReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.private_identifier->dump(indent + 2);
            });
    }
}

Optional<OptionalChain::ReferenceAndValue> OptionalChain::to_reference_and_value(JS::Interpreter& interpreter, JS::GlobalObject& global_object) const
{
    // Note: This is wrapped in an optional to allow base_reference = ...
    Optional<JS::Reference> base_reference = m_base->to_reference(interpreter, global_object);
    auto base = base_reference->is_unresolvable() ? m_base->execute(interpreter, global_object) : TRY_OR_DISCARD(base_reference->get_value(global_object));
    if (interpreter.exception())
        return {};

    for (auto& reference : m_references) {
        auto is_optional = reference.visit([](auto& ref) { return ref.mode; }) == Mode::Optional;
        if (is_optional && base.is_nullish())
            return ReferenceAndValue { {}, js_undefined() };

        auto expression = reference.visit(
            [&](Call const& call) -> NonnullRefPtr<Expression> {
                return create_ast_node<CallExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), *base_reference, base),
                    call.arguments);
            },
            [&](ComputedReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), *base_reference, base),
                    ref.expression,
                    true);
            },
            [&](MemberReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), *base_reference, base),
                    ref.identifier,
                    false);
            },
            [&](PrivateMemberReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), *base_reference, base),
                    ref.private_identifier,
                    false);
            });
        if (is<CallExpression>(*expression)) {
            base_reference = JS::Reference {};
            base = expression->execute(interpreter, global_object);
        } else {
            base_reference = expression->to_reference(interpreter, global_object);
            base = TRY_OR_DISCARD(base_reference->get_value(global_object));
        }
        if (interpreter.exception())
            return {};
    }

    return ReferenceAndValue { base_reference.release_value(), base };
}

Value OptionalChain::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    if (auto result = to_reference_and_value(interpreter, global_object); result.has_value())
        return result.release_value().value;
    return {};
}

JS::Reference OptionalChain::to_reference(Interpreter& interpreter, GlobalObject& global_object) const
{
    if (auto result = to_reference_and_value(interpreter, global_object); result.has_value())
        return result.release_value().reference;
    return {};
}

void MetaProperty::dump(int indent) const
{
    String name;
    if (m_type == MetaProperty::Type::NewTarget)
        name = "new.target";
    else if (m_type == MetaProperty::Type::ImportMeta)
        name = "import.meta";
    else
        VERIFY_NOT_REACHED();
    print_indent(indent);
    outln("{} {}", class_name(), name);
}

Value MetaProperty::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_type == MetaProperty::Type::NewTarget)
        return interpreter.vm().get_new_target().value_or(js_undefined());
    if (m_type == MetaProperty::Type::ImportMeta) {
        interpreter.vm().throw_exception<InternalError>(global_object, ErrorType::NotImplemented, "'import.meta' in modules");
        return {};
    }

    VERIFY_NOT_REACHED();
}

void ImportCall::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Specifier)");
    m_specifier->dump(indent + 1);
    if (m_options) {
        outln("(Options)");
        m_options->dump(indent + 1);
    }
}

Value ImportCall::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    interpreter.vm().throw_exception<InternalError>(global_object, ErrorType::NotImplemented, "'import(...)' in modules");
    return {};
}

Value StringLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return js_string(interpreter.heap(), m_value);
}

Value NumericLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return Value(m_value);
}

Value BigIntLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    Crypto::SignedBigInteger integer;
    if (m_value[0] == '0' && m_value.length() >= 3) {
        if (m_value[1] == 'x' || m_value[1] == 'X') {
            return js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(16, m_value.substring(2, m_value.length() - 3)));
        } else if (m_value[1] == 'o' || m_value[1] == 'O') {
            return js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(8, m_value.substring(2, m_value.length() - 3)));
        } else if (m_value[1] == 'b' || m_value[1] == 'B') {
            return js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(2, m_value.substring(2, m_value.length() - 3)));
        }
    }
    return js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1)));
}

Value BooleanLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return Value(m_value);
}

Value NullLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return js_null();
}

void RegExpLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("{} (/{}/{})", class_name(), pattern(), flags());
}

Value RegExpLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    Regex<ECMA262> regex(parsed_regex(), parsed_pattern(), parsed_flags());
    return RegExpObject::create(global_object, move(regex), pattern(), flags());
}

void ArrayExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& element : m_elements) {
        if (element) {
            element->dump(indent + 1);
        } else {
            print_indent(indent + 1);
            outln("<empty>");
        }
    }
}

Value ArrayExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto* array = MUST(Array::create(global_object, 0));
    array->indexed_properties();
    size_t index = 0;
    for (auto& element : m_elements) {
        auto value = Value();
        if (element) {
            value = element->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};

            if (is<SpreadExpression>(*element)) {
                TRY_OR_DISCARD(get_iterator_values(global_object, value, [&](Value iterator_value) -> Optional<Completion> {
                    array->indexed_properties().put(index++, iterator_value, default_attributes);
                    return {};
                }));
                continue;
            }
        }
        array->indexed_properties().put(index++, value, default_attributes);
    }
    return array;
}

void TemplateLiteral::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& expression : m_expressions)
        expression.dump(indent + 1);
}

Value TemplateLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    StringBuilder string_builder;

    for (auto& expression : m_expressions) {
        auto expr = expression.execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        auto string = TRY_OR_DISCARD(expr.to_string(global_object));
        string_builder.append(string);
    }

    return js_string(interpreter.heap(), string_builder.build());
}

void TaggedTemplateLiteral::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(Tag)");
    m_tag->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Template Literal)");
    m_template_literal->dump(indent + 2);
}

Value TaggedTemplateLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    auto tag = m_tag->execute(interpreter, global_object);
    if (vm.exception())
        return {};
    if (!tag.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, tag.to_string_without_side_effects());
        return {};
    }
    auto& tag_function = tag.as_function();
    auto& expressions = m_template_literal->expressions();
    auto* strings = MUST(Array::create(global_object, 0));
    MarkedValueList arguments(vm.heap());
    arguments.append(strings);
    for (size_t i = 0; i < expressions.size(); ++i) {
        auto value = expressions[i].execute(interpreter, global_object);
        if (vm.exception())
            return {};
        // tag`${foo}`             -> "", foo, ""                -> tag(["", ""], foo)
        // tag`foo${bar}baz${qux}` -> "foo", bar, "baz", qux, "" -> tag(["foo", "baz", ""], bar, qux)
        if (i % 2 == 0) {
            strings->indexed_properties().append(value);
        } else {
            arguments.append(value);
        }
    }

    auto* raw_strings = MUST(Array::create(global_object, 0));
    for (auto& raw_string : m_template_literal->raw_strings()) {
        auto value = raw_string.execute(interpreter, global_object);
        if (vm.exception())
            return {};
        raw_strings->indexed_properties().append(value);
    }
    strings->define_direct_property(vm.names.raw, raw_strings, 0);
    return TRY_OR_DISCARD(vm.call(tag_function, js_undefined(), move(arguments)));
}

void TryStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Block)");
    block().dump(indent + 1);

    if (handler()) {
        print_indent(indent);
        outln("(Handler)");
        handler()->dump(indent + 1);
    }

    if (finalizer()) {
        print_indent(indent);
        outln("(Finalizer)");
        finalizer()->dump(indent + 1);
    }
}

void CatchClause::dump(int indent) const
{
    print_indent(indent);
    m_parameter.visit(
        [&](FlyString const& parameter) {
            if (parameter.is_null())
                outln("CatchClause");
            else
                outln("CatchClause ({})", parameter);
        },
        [&](NonnullRefPtr<BindingPattern> const& pattern) {
            outln("CatchClause");
            print_indent(indent);
            outln("(Parameter)");
            pattern->dump(indent + 2);
        });

    body().dump(indent + 1);
}

void ThrowStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    argument().dump(indent + 1);
}

void TryStatement::add_label(FlyString string)
{
    m_block->add_label(move(string));
}

Value TryStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // FIXME: Use Completions here to be closer to the spec.
    auto result = m_block->execute(interpreter, global_object);
    if (interpreter.vm().unwind_until() == ScopeType::Try)
        interpreter.vm().stop_unwind();
    if (auto* exception = interpreter.exception()) {
        // 14.15.2 Runtime Semantics: CatchClauseEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-catchclauseevaluation
        if (m_handler) {
            interpreter.vm().clear_exception();

            auto* catch_scope = new_declarative_environment(*interpreter.lexical_environment());

            m_handler->parameter().visit(
                [&](FlyString const& parameter) {
                    MUST(catch_scope->create_mutable_binding(global_object, parameter, false));
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    pattern->for_each_bound_name([&](auto& name) {
                        MUST(catch_scope->create_mutable_binding(global_object, name, false));
                    });
                });

            TemporaryChange<Environment*> scope_change(interpreter.vm().running_execution_context().lexical_environment, catch_scope);

            m_handler->parameter().visit(
                [&](FlyString const& parameter) {
                    (void)catch_scope->initialize_binding(global_object, parameter, exception->value());
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    (void)interpreter.vm().binding_initialization(pattern, exception->value(), catch_scope, global_object);
                });

            if (!interpreter.exception())
                result = m_handler->body().execute(interpreter, global_object);
        }
    }

    if (m_finalizer) {
        // Keep, if any, and then clear the current exception so we can
        // execute() the finalizer without an exception in our way.
        auto* previous_exception = interpreter.exception();
        interpreter.vm().clear_exception();

        // Remember what scope type we were unwinding to, and temporarily
        // clear it as well (e.g. return from handler).
        auto unwind_until = interpreter.vm().unwind_until();
        interpreter.vm().stop_unwind();

        auto finalizer_result = m_finalizer->execute(interpreter, global_object);
        if (interpreter.vm().should_unwind()) {
            // This was NOT a 'normal' completion (e.g. return from finalizer).
            result = finalizer_result;
        } else {
            // Continue unwinding to whatever we found ourselves unwinding
            // to when the finalizer was entered (e.g. return from handler,
            // which is unaffected by normal completion from finalizer).
            interpreter.vm().unwind(unwind_until);

            // If we previously had an exception and the finalizer didn't
            // throw a new one, restore the old one.
            if (previous_exception && !interpreter.exception())
                interpreter.vm().set_exception(*previous_exception);
        }
    }

    return result.value_or(js_undefined());
}

Value CatchClause::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: CatchClause execution is handled by TryStatement.
    VERIFY_NOT_REACHED();
    return {};
}

Value ThrowStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto value = m_argument->execute(interpreter, global_object);
    if (interpreter.vm().exception())
        return {};
    interpreter.vm().throw_exception(global_object, value);
    return {};
}

// 14.12.2 Runtime Semantics: CaseBlockEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-caseblockevaluation
Value SwitchStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // FIXME: This needs a massive refactoring, ideally once we start using continue, break, and return completions.
    // Instead of having an optional test expression, SwitchCase should be split into CaseClause and DefaultClause.
    // https://tc39.es/ecma262/#sec-switch-statement

    InterpreterNodeScope node_scope { interpreter, *this };

    auto discriminant_result = m_discriminant->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    // Optimization: Avoid creating a lexical environment if there are no lexical declarations.
    Optional<TemporaryChange<Environment*>> lexical_environment_changer;
    if (has_lexical_declarations()) {
        auto* old_environment = interpreter.lexical_environment();
        auto* block_environment = new_declarative_environment(*old_environment);
        block_declaration_instantiation(global_object, block_environment);
        lexical_environment_changer.emplace(interpreter.vm().running_execution_context().lexical_environment, block_environment);
    }

    Optional<size_t> first_passing_case;
    for (size_t i = 0; i < m_cases.size(); ++i) {
        auto& switch_case = m_cases[i];
        if (switch_case.test()) {
            auto test_result = switch_case.test()->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            if (is_strictly_equal(discriminant_result, test_result)) {
                first_passing_case = i;
                break;
            }
        }
    }

    // FIXME: we could optimize and store the location of the default case in a member variable.
    if (!first_passing_case.has_value()) {
        for (size_t i = 0; i < m_cases.size(); ++i) {
            auto& switch_case = m_cases[i];
            if (!switch_case.test()) {
                first_passing_case = i;
                break;
            }
        }
    }

    auto last_value = js_undefined();

    if (!first_passing_case.has_value()) {
        return last_value;
    }
    VERIFY(first_passing_case.value() < m_cases.size());

    for (size_t i = first_passing_case.value(); i < m_cases.size(); ++i) {
        auto& switch_case = m_cases[i];
        for (auto& statement : switch_case.children()) {
            auto value = statement.execute(interpreter, global_object);
            if (!value.is_empty())
                last_value = value;
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_labels)) {
                    // No stop_unwind(), the outer loop will handle that - we just need to break out of the switch/case.
                    return last_value;
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_labels)) {
                    interpreter.vm().stop_unwind();
                    return last_value;
                } else {
                    return last_value;
                }
            }
        }
    }
    return last_value;
}

Value SwitchCase::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: SwitchCase execution is handled by SwitchStatement.
    VERIFY_NOT_REACHED();
    return {};
}

Value BreakStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    interpreter.vm().unwind(ScopeType::Breakable, m_target_label);
    return {};
}

Value ContinueStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    interpreter.vm().unwind(ScopeType::Continuable, m_target_label);
    return {};
}

void SwitchStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_discriminant->dump(indent + 1);
    for (auto& switch_case : m_cases) {
        switch_case.dump(indent + 1);
    }
}

void SwitchCase::dump(int indent) const
{
    print_indent(indent + 1);
    if (m_test) {
        outln("(Test)");
        m_test->dump(indent + 2);
    } else {
        outln("(Default)");
    }
    print_indent(indent + 1);
    outln("(Consequent)");
    ScopeNode::dump(indent + 2);
}

Value ConditionalExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto test_result = m_test->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    Value result;
    if (test_result.to_boolean()) {
        result = m_consequent->execute(interpreter, global_object);
    } else {
        result = m_alternate->execute(interpreter, global_object);
    }
    if (interpreter.exception())
        return {};
    return result;
}

void ConditionalExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(Test)");
    m_test->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Consequent)");
    m_consequent->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Alternate)");
    m_alternate->dump(indent + 2);
}

void SequenceExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& expression : m_expressions)
        expression.dump(indent + 1);
}

Value SequenceExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    Value last_value;
    for (auto& expression : m_expressions) {
        last_value = expression.execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
    }
    return last_value;
}

Value DebuggerStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    // Sorry, no JavaScript debugger available (yet)!
    return {};
}

void ScopeNode::for_each_lexically_scoped_declaration(IteratorOrVoidFunction<Declaration const&>&& callback) const
{
    for (auto& declaration : m_lexical_declarations) {
        if (callback(declaration) == IterationDecision::Break)
            break;
    }
}

void ScopeNode::for_each_lexically_declared_name(IteratorOrVoidFunction<FlyString const&>&& callback) const
{
    auto running = true;
    for (auto& declaration : m_lexical_declarations) {
        declaration.for_each_bound_name([&](auto const& name) {
            if (callback(name) == IterationDecision::Break) {
                running = false;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (!running)
            break;
    }
}

void ScopeNode::for_each_var_declared_name(IteratorOrVoidFunction<FlyString const&>&& callback) const
{
    auto running = true;
    for (auto& declaration : m_var_declarations) {
        declaration.for_each_bound_name([&](auto const& name) {
            if (callback(name) == IterationDecision::Break) {
                running = false;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        if (!running)
            break;
    }
}

void ScopeNode::for_each_var_function_declaration_in_reverse_order(IteratorOrVoidFunction<FunctionDeclaration const&>&& callback) const
{
    for (ssize_t i = m_var_declarations.size() - 1; i >= 0; i--) {
        auto& declaration = m_var_declarations[i];
        if (is<FunctionDeclaration>(declaration)) {
            if (callback(static_cast<FunctionDeclaration const&>(declaration)) == IterationDecision::Break)
                break;
        }
    }
}

void ScopeNode::for_each_var_scoped_variable_declaration(IteratorOrVoidFunction<VariableDeclaration const&>&& callback) const
{
    for (auto& declaration : m_var_declarations) {
        if (!is<FunctionDeclaration>(declaration)) {
            VERIFY(is<VariableDeclaration>(declaration));
            if (callback(static_cast<VariableDeclaration const&>(declaration)) == IterationDecision::Break)
                break;
        }
    }
}

void ScopeNode::for_each_function_hoistable_with_annexB_extension(IteratorOrVoidFunction<FunctionDeclaration&>&& callback) const
{
    for (auto& function : m_functions_hoistable_with_annexB_extension) {
        // We need const_cast here since it might have to set a property on function declaration.
        if (callback(const_cast<FunctionDeclaration&>(function)) == IterationDecision::Break)
            break;
    }
}

void ScopeNode::add_lexical_declaration(NonnullRefPtr<Declaration> declaration)
{
    m_lexical_declarations.append(move(declaration));
}

void ScopeNode::add_var_scoped_declaration(NonnullRefPtr<Declaration> declaration)
{
    m_var_declarations.append(move(declaration));
}

void ScopeNode::add_hoisted_function(NonnullRefPtr<FunctionDeclaration> declaration)
{
    m_functions_hoistable_with_annexB_extension.append(move(declaration));
}

Value ImportStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    dbgln("Modules are not fully supported yet!");
    interpreter.vm().throw_exception<InternalError>(global_object, ErrorType::NotImplemented, "'import' in modules");
    return {};
}

Value ExportStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    if (m_statement)
        return m_statement->execute(interpreter, global_object);

    return {};
}

void ExportStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(ExportEntries)");

    auto string_or_null = [](String const& string) -> String {
        if (string.is_empty()) {
            return "null";
        }
        return String::formatted("\"{}\"", string);
    };

    for (auto& entry : m_entries) {
        print_indent(indent + 2);
        outln("ModuleRequest: {}, ImportName: {}, LocalName: {}, ExportName: {}", string_or_null(entry.module_request), entry.kind == ExportEntry::ModuleRequest ? string_or_null(entry.local_or_import_name) : "null", entry.kind != ExportEntry::ModuleRequest ? string_or_null(entry.local_or_import_name) : "null", string_or_null(entry.export_name));
    }
}

void ImportStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    if (m_entries.is_empty()) {
        // direct from "module" import
        outln("Entire module '{}'", m_module_request);
    } else {
        outln("(ExportEntries) from {}", m_module_request);

        for (auto& entry : m_entries) {
            print_indent(indent + 2);
            outln("ImportName: {}, LocalName: {}", entry.import_name, entry.local_name);
        }
    }
}

bool ExportStatement::has_export(StringView export_name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        return entry.export_name == export_name;
    });
}

bool ImportStatement::has_bound_name(StringView name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        return entry.local_name == name;
    });
}

// 14.2.3 BlockDeclarationInstantiation ( code, env ), https://tc39.es/ecma262/#sec-blockdeclarationinstantiation
void ScopeNode::block_declaration_instantiation(GlobalObject& global_object, Environment* environment) const
{
    // See also B.3.2.6 Changes to BlockDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-blockdeclarationinstantiation
    VERIFY(environment);
    auto* private_environment = global_object.vm().running_execution_context().private_environment;
    for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        auto is_constant_declaration = declaration.is_constant_declaration();
        declaration.for_each_bound_name([&](auto const& name) {
            if (is_constant_declaration) {
                MUST(environment->create_immutable_binding(global_object, name, true));
            } else {
                if (!MUST(environment->has_binding(name)))
                    MUST(environment->create_mutable_binding(global_object, name, false));
            }
        });

        if (is<FunctionDeclaration>(declaration)) {
            auto& function_declaration = static_cast<FunctionDeclaration const&>(declaration);
            auto* function = ECMAScriptFunctionObject::create(global_object, function_declaration.name(), function_declaration.body(), function_declaration.parameters(), function_declaration.function_length(), environment, private_environment, function_declaration.kind(), function_declaration.is_strict_mode(), function_declaration.might_need_arguments_object(), function_declaration.contains_direct_call_to_eval());
            VERIFY(is<DeclarativeEnvironment>(*environment));
            static_cast<DeclarativeEnvironment&>(*environment).initialize_or_set_mutable_binding({}, global_object, function_declaration.name(), function);
        }
    });
}

// 16.1.7 GlobalDeclarationInstantiation ( script, env ), https://tc39.es/ecma262/#sec-globaldeclarationinstantiation
ThrowCompletionOr<void> Program::global_declaration_instantiation(Interpreter& interpreter, GlobalObject& global_object, GlobalEnvironment& global_environment) const
{
    for_each_lexically_declared_name([&](FlyString const& name) {
        if (global_environment.has_var_declaration(name) || global_environment.has_lexical_declaration(name)) {
            interpreter.vm().throw_exception<SyntaxError>(global_object, ErrorType::TopLevelVariableAlreadyDeclared, name);
            return IterationDecision::Break;
        }

        auto restricted_global = global_environment.has_restricted_global_property(name);
        if (interpreter.exception())
            return IterationDecision::Break;

        if (restricted_global)
            interpreter.vm().throw_exception<SyntaxError>(global_object, ErrorType::RestrictedGlobalProperty, name);

        return IterationDecision::Continue;
    });

    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    for_each_var_declared_name([&](auto const& name) {
        if (global_environment.has_lexical_declaration(name)) {
            interpreter.vm().throw_exception<SyntaxError>(global_object, ErrorType::TopLevelVariableAlreadyDeclared, name);
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    HashTable<FlyString> declared_function_names;
    Vector<FunctionDeclaration const&> functions_to_initialize;

    for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) {
        if (declared_function_names.set(function.name()) != AK::HashSetResult::InsertedNewEntry)
            return IterationDecision::Continue;

        auto function_definable = global_environment.can_declare_global_function(function.name());

        if (interpreter.exception())
            return IterationDecision::Break;

        if (!function_definable) {
            interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::CannotDeclareGlobalFunction, function.name());
            return IterationDecision::Break;
        }

        functions_to_initialize.append(function);
        return IterationDecision::Continue;
    });

    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    HashTable<FlyString> declared_var_names;

    for_each_var_scoped_variable_declaration([&](Declaration const& declaration) {
        declaration.for_each_bound_name([&](auto const& name) {
            if (declared_function_names.contains(name))
                return IterationDecision::Continue;

            auto var_definable = global_environment.can_declare_global_var(name);
            if (interpreter.exception())
                return IterationDecision::Break;

            if (!var_definable) {
                interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::CannotDeclareGlobalVariable, name);
                return IterationDecision::Break;
            }

            declared_var_names.set(name);
            return IterationDecision::Continue;
        });
        if (interpreter.exception())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });

    if (auto* exception = interpreter.exception())
        return throw_completion(exception->value());

    if (!m_is_strict_mode) {
        for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) {
            auto& function_name = function_declaration.name();
            if (global_environment.has_lexical_declaration(function_name))
                return IterationDecision::Continue;

            auto function_definable = global_environment.can_declare_global_function(function_name);

            if (interpreter.exception())
                return IterationDecision::Break;

            if (!function_definable) {
                interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::CannotDeclareGlobalFunction, function_name);
                return IterationDecision::Break;
            }

            if (!declared_function_names.contains(function_name) && !declared_var_names.contains(function_name)) {
                global_environment.create_global_var_binding(function_name, false);
                if (interpreter.exception())
                    return IterationDecision::Break;
                declared_function_names.set(function_name);
            }

            function_declaration.set_should_do_additional_annexB_steps();

            return IterationDecision::Continue;
        });

        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());

        // We should not use declared function names below here anymore since these functions are not in there in the spec.
        declared_function_names.clear();
    }

    PrivateEnvironment* private_environment = nullptr;

    for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        declaration.for_each_bound_name([&](auto const& name) {
            if (declaration.is_constant_declaration())
                (void)global_environment.create_immutable_binding(global_object, name, true);
            else
                (void)global_environment.create_mutable_binding(global_object, name, false);
            if (interpreter.exception())
                return IterationDecision::Break;
            return IterationDecision::Continue;
        });
        if (interpreter.exception())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });

    for (auto& declaration : functions_to_initialize) {
        auto* function = ECMAScriptFunctionObject::create(global_object, declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), &global_environment, private_environment, declaration.kind(), declaration.is_strict_mode(), declaration.might_need_arguments_object(), declaration.contains_direct_call_to_eval());
        global_environment.create_global_function_binding(declaration.name(), function, false);
        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());
    }

    for (auto& var_name : declared_var_names) {
        global_environment.create_global_var_binding(var_name, false);
        if (auto* exception = interpreter.exception())
            return throw_completion(exception->value());
    }

    return {};
}

}
