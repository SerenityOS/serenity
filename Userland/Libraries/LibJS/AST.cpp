/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
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
    if (is<OrdinaryFunctionObject>(function) && function.name().is_empty())
        static_cast<OrdinaryFunctionObject&>(function).set_name(name);
}

static String get_function_name(GlobalObject& global_object, Value value)
{
    if (value.is_symbol())
        return String::formatted("[{}]", value.as_symbol().description());
    if (value.is_string())
        return value.as_string().string();
    return value.to_string(global_object);
}

Value ScopeNode::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return interpreter.execute_statement(global_object, *this);
}

Value Program::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return interpreter.execute_statement(global_object, *this, ScopeType::Block);
}

Value FunctionDeclaration::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return {};
}

// 15.2.5 Runtime Semantics: InstantiateOrdinaryFunctionExpression, https://tc39.es/ecma262/#sec-runtime-semantics-instantiateordinaryfunctionexpression
Value FunctionExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto* func_env = interpreter.lexical_environment();
    bool has_identifier = !name().is_empty() && !is_auto_renamed();

    if (has_identifier) {
        func_env = interpreter.heap().allocate<DeclarativeEnvironment>(global_object, func_env);
        func_env->create_immutable_binding(global_object, name(), false);
    }

    auto closure = OrdinaryFunctionObject::create(global_object, name(), body(), parameters(), function_length(), func_env, kind(), is_strict_mode(), is_arrow_function());

    if (has_identifier)
        func_env->initialize_binding(global_object, name(), closure);

    return closure;
}

Value ExpressionStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_expression->execute(interpreter, global_object);
}

CallExpression::ThisAndCallee CallExpression::compute_this_and_callee(Interpreter& interpreter, GlobalObject& global_object, Reference const& callee_reference) const
{
    auto& vm = interpreter.vm();

    if (callee_reference.is_property_reference()) {
        auto this_value = callee_reference.get_this_value();
        auto callee = callee_reference.get_value(global_object);
        if (vm.exception())
            return {};

        return { this_value, callee };
    }

    // [[Call]] will handle that in non-strict mode the this value becomes the global object
    return {
        js_undefined(),
        callee_reference.is_unresolvable()
            ? m_callee->execute(interpreter, global_object)
            : callee_reference.get_value(global_object)
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
            get_iterator_values(global_object, value, [&](Value iterator_value) {
                if (vm.exception())
                    return IterationDecision::Break;
                list.append(iterator_value);
                return IterationDecision::Continue;
            });
            if (vm.exception())
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

    if (!callee_value.is_function() || (is<NativeFunction>(callee_value.as_object()) && !static_cast<NativeFunction&>(callee_value.as_object()).has_constructor())) {
        throw_type_error_for_callee(interpreter, global_object, callee_value, "constructor"sv);
        return {};
    }

    MarkedValueList arg_list(vm.heap());
    argument_list_evaluation(interpreter, global_object, m_arguments, arg_list);
    if (interpreter.exception())
        return {};

    auto& function = callee_value.as_function();
    return vm.construct(function, function, move(arg_list));
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
    // FIXME: This check is non-conforming.
    if (!func || !func->is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, "Super constructor");
        return {};
    }

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto& function = new_target.as_function();
    auto result = vm.construct(static_cast<FunctionObject&>(*func), function, move(arg_list));
    if (vm.exception())
        return {};

    // 7. Let thisER be GetThisEnvironment().
    auto& this_er = verify_cast<FunctionEnvironment>(get_this_environment(interpreter.vm()));

    // 8. Perform ? thisER.BindThisValue(result).
    this_er.bind_this_value(global_object, result);
    if (vm.exception())
        return {};

    // 9. Let F be thisER.[[FunctionObject]].
    // 10. Assert: F is an ECMAScript function object. (NOTE: This is implied by the strong C++ type.)
    [[maybe_unused]] auto& f = this_er.function_object();

    // 11. Perform ? InitializeInstanceElements(result, F).
    VERIFY(result.is_object());
    vm.initialize_instance_elements(result.as_object(), f);

    // 12. Return result.
    return result;
}

Value YieldExpression::execute(Interpreter&, GlobalObject&) const
{
    // This should be transformed to a return.
    VERIFY_NOT_REACHED();
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
        return interpreter.execute_statement(global_object, *m_consequent);

    if (m_alternate)
        return interpreter.execute_statement(global_object, *m_alternate);

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
    auto* object = value.to_object(global_object);
    if (interpreter.exception())
        return {};

    // 3. Let oldEnv be the running execution context's LexicalEnvironment.
    auto* old_environment = interpreter.vm().running_execution_context().lexical_environment;

    // 4. Let newEnv be NewObjectEnvironment(obj, true, oldEnv).
    auto* new_environment = new_object_environment(*object, true, old_environment);
    if (interpreter.exception())
        return {};

    // 5. Set the running execution context's LexicalEnvironment to newEnv.
    interpreter.vm().running_execution_context().lexical_environment = new_environment;

    // 6. Let C be the result of evaluating Statement.
    auto result = interpreter.execute_statement(global_object, m_body).value_or(js_undefined());

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
        last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
        if (interpreter.exception())
            return {};
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
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
        last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
        if (interpreter.exception())
            return {};
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
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

    RefPtr<BlockStatement> wrapper;

    if (m_init && is<VariableDeclaration>(*m_init) && static_cast<VariableDeclaration const&>(*m_init).declaration_kind() != DeclarationKind::Var) {
        wrapper = create_ast_node<BlockStatement>(source_range());
        NonnullRefPtrVector<VariableDeclaration> decls;
        decls.append(*static_cast<VariableDeclaration const*>(m_init.ptr()));
        wrapper->add_variables(decls);
        interpreter.enter_scope(*wrapper, ScopeType::Block, global_object);
    }

    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });

    auto last_value = js_undefined();
    if (m_init) {
        m_init->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
    }

    if (m_test) {
        while (true) {
            auto test_result = m_test->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            if (!test_result.to_boolean())
                break;
            last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                    interpreter.vm().stop_unwind();
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
                    interpreter.vm().stop_unwind();
                    break;
                } else {
                    return last_value;
                }
            }
            if (m_update) {
                m_update->execute(interpreter, global_object);
                if (interpreter.exception())
                    return {};
            }
        }
    } else {
        while (true) {
            last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                    interpreter.vm().stop_unwind();
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
                    interpreter.vm().stop_unwind();
                    break;
                } else {
                    return last_value;
                }
            }
            if (m_update) {
                m_update->execute(interpreter, global_object);
                if (interpreter.exception())
                    return {};
            }
        }
    }

    return last_value;
}

static Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> variable_from_for_declaration(Interpreter& interpreter, GlobalObject& global_object, ASTNode const& node, RefPtr<BlockStatement> wrapper)
{
    if (is<VariableDeclaration>(node)) {
        auto& variable_declaration = static_cast<VariableDeclaration const&>(node);
        VERIFY(!variable_declaration.declarations().is_empty());
        if (variable_declaration.declaration_kind() != DeclarationKind::Var) {
            wrapper = create_ast_node<BlockStatement>(node.source_range());
            interpreter.enter_scope(*wrapper, ScopeType::Block, global_object);
        }
        variable_declaration.execute(interpreter, global_object);
        return variable_declaration.declarations().first().target();
    }

    if (is<Identifier>(node)) {
        return NonnullRefPtr(static_cast<Identifier const&>(node));
    }

    VERIFY_NOT_REACHED();
}

Value ForInStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    bool has_declaration = is<VariableDeclaration>(*m_lhs);
    if (!has_declaration && !is<Identifier>(*m_lhs)) {
        // FIXME: Implement "for (foo.bar in baz)", "for (foo[0] in bar)"
        VERIFY_NOT_REACHED();
    }
    RefPtr<BlockStatement> wrapper;
    auto target = variable_from_for_declaration(interpreter, global_object, m_lhs, wrapper);
    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });
    auto last_value = js_undefined();
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    if (rhs_result.is_nullish())
        return {};
    auto* object = rhs_result.to_object(global_object);
    while (object) {
        auto property_names = object->enumerable_own_property_names(Object::PropertyKind::Key);
        for (auto& value : property_names) {
            interpreter.vm().assign(target, value, global_object, has_declaration);
            if (interpreter.exception())
                return {};
            last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                    interpreter.vm().stop_unwind();
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
                    interpreter.vm().stop_unwind();
                    break;
                } else {
                    return last_value;
                }
            }
        }
        object = object->internal_get_prototype_of();
        if (interpreter.exception())
            return {};
    }
    return last_value;
}

Value ForOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    bool has_declaration = is<VariableDeclaration>(*m_lhs);
    if (!has_declaration && !is<Identifier>(*m_lhs)) {
        // FIXME: Implement "for (foo.bar of baz)", "for (foo[0] of bar)"
        VERIFY_NOT_REACHED();
    }
    RefPtr<BlockStatement> wrapper;
    auto target = variable_from_for_declaration(interpreter, global_object, m_lhs, wrapper);
    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });
    auto last_value = js_undefined();
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    get_iterator_values(global_object, rhs_result, [&](Value value) {
        interpreter.vm().assign(target, value, global_object, has_declaration);
        last_value = interpreter.execute_statement(global_object, *m_body).value_or(last_value);
        if (interpreter.exception())
            return IterationDecision::Break;
        if (interpreter.vm().should_unwind()) {
            if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                interpreter.vm().stop_unwind();
            } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
                interpreter.vm().stop_unwind();
                return IterationDecision::Break;
            } else {
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    if (interpreter.exception())
        return {};

    return last_value;
}

Value BinaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto lhs_result = m_lhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case BinaryOp::Addition:
        return add(global_object, lhs_result, rhs_result);
    case BinaryOp::Subtraction:
        return sub(global_object, lhs_result, rhs_result);
    case BinaryOp::Multiplication:
        return mul(global_object, lhs_result, rhs_result);
    case BinaryOp::Division:
        return div(global_object, lhs_result, rhs_result);
    case BinaryOp::Modulo:
        return mod(global_object, lhs_result, rhs_result);
    case BinaryOp::Exponentiation:
        return exp(global_object, lhs_result, rhs_result);
    case BinaryOp::TypedEquals:
        return Value(is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::TypedInequals:
        return Value(!is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::AbstractEquals:
        return Value(abstract_eq(global_object, lhs_result, rhs_result));
    case BinaryOp::AbstractInequals:
        return Value(!abstract_eq(global_object, lhs_result, rhs_result));
    case BinaryOp::GreaterThan:
        return greater_than(global_object, lhs_result, rhs_result);
    case BinaryOp::GreaterThanEquals:
        return greater_than_equals(global_object, lhs_result, rhs_result);
    case BinaryOp::LessThan:
        return less_than(global_object, lhs_result, rhs_result);
    case BinaryOp::LessThanEquals:
        return less_than_equals(global_object, lhs_result, rhs_result);
    case BinaryOp::BitwiseAnd:
        return bitwise_and(global_object, lhs_result, rhs_result);
    case BinaryOp::BitwiseOr:
        return bitwise_or(global_object, lhs_result, rhs_result);
    case BinaryOp::BitwiseXor:
        return bitwise_xor(global_object, lhs_result, rhs_result);
    case BinaryOp::LeftShift:
        return left_shift(global_object, lhs_result, rhs_result);
    case BinaryOp::RightShift:
        return right_shift(global_object, lhs_result, rhs_result);
    case BinaryOp::UnsignedRightShift:
        return unsigned_right_shift(global_object, lhs_result, rhs_result);
    case BinaryOp::In:
        return in(global_object, lhs_result, rhs_result);
    case BinaryOp::InstanceOf:
        return instance_of(global_object, lhs_result, rhs_result);
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
    return interpreter.vm().resolve_binding(string());
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
        auto actual_this = environment.get_this_binding(global_object);

        StringOrSymbol property_key;

        if (is_computed()) {
            // SuperProperty : super [ Expression ]

            // 3. Let propertyNameReference be the result of evaluating Expression.
            // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
            auto property_name_value = m_property->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            property_key = property_name_value.to_property_key(global_object);
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

    auto object_value = m_object->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    // From here on equivalent to
    // 13.3.4 EvaluatePropertyAccessWithIdentifierKey ( baseValue, identifierName, strict ), https://tc39.es/ecma262/#sec-evaluate-property-access-with-identifier-key

    object_value = TRY_OR_DISCARD(require_object_coercible(global_object, object_value));

    auto property_name = computed_property_name(interpreter, global_object);
    if (!property_name.is_valid())
        return Reference {};

    auto strict = interpreter.vm().in_strict_mode();
    return Reference { object_value, move(property_name), {}, strict };
}

Value UnaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    if (m_op == UnaryOp::Delete) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception())
            return {};
        return Value(reference.delete_(global_object));
    }

    Value lhs_result;
    if (m_op == UnaryOp::Typeof && is<Identifier>(*m_lhs)) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception()) {
            return {};
        }
        if (reference.is_unresolvable()) {
            lhs_result = js_undefined();
        } else {
            lhs_result = reference.get_value(global_object, false);
        }
    } else {
        lhs_result = m_lhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
    }

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        return bitwise_not(global_object, lhs_result);
    case UnaryOp::Not:
        return Value(!lhs_result.to_boolean());
    case UnaryOp::Plus:
        return unary_plus(global_object, lhs_result);
    case UnaryOp::Minus:
        return unary_minus(global_object, lhs_result);
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

Value ClassMethod::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_function->execute(interpreter, global_object);
}

Value ClassField::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return {};
}

Value ClassExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    Value class_constructor_value = m_constructor->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    update_function_name(class_constructor_value, m_name);

    VERIFY(class_constructor_value.is_function() && is<OrdinaryFunctionObject>(class_constructor_value.as_function()));
    auto* class_constructor = static_cast<OrdinaryFunctionObject*>(&class_constructor_value.as_function());
    class_constructor->set_is_class_constructor();
    Value super_constructor = js_undefined();
    if (!m_super_class.is_null()) {
        super_constructor = m_super_class->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!super_constructor.is_function() && !super_constructor.is_null()) {
            interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::ClassExtendsValueNotAConstructorOrNull, super_constructor.to_string_without_side_effects());
            return {};
        }
        class_constructor->set_constructor_kind(FunctionObject::ConstructorKind::Derived);

        Object* super_constructor_prototype = nullptr;
        if (!super_constructor.is_null()) {
            auto super_constructor_prototype_value = super_constructor.as_object().get(vm.names.prototype);
            if (interpreter.exception())
                return {};
            if (!super_constructor_prototype_value.is_object() && !super_constructor_prototype_value.is_null()) {
                interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::ClassExtendsValueInvalidPrototype, super_constructor_prototype_value.to_string_without_side_effects());
                return {};
            }
            if (super_constructor_prototype_value.is_object())
                super_constructor_prototype = &super_constructor_prototype_value.as_object();
        }
        auto* prototype = Object::create(global_object, super_constructor_prototype);

        prototype->define_direct_property(vm.names.constructor, class_constructor, 0);
        if (interpreter.exception())
            return {};
        class_constructor->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
        if (interpreter.exception())
            return {};
        class_constructor->internal_set_prototype_of(super_constructor.is_null() ? global_object.function_prototype() : &super_constructor.as_object());
    }

    auto class_prototype = class_constructor->get(vm.names.prototype);
    if (interpreter.exception())
        return {};

    if (!class_prototype.is_object()) {
        interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Class prototype");
        return {};
    }
    for (auto const& method : m_methods) {
        auto method_value = method.execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        auto& method_function = method_value.as_function();

        auto key = method.key().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        auto property_key = key.to_property_key(global_object);
        if (interpreter.exception())
            return {};

        auto& target = method.is_static() ? *class_constructor : class_prototype.as_object();
        method_function.set_home_object(&target);

        switch (method.kind()) {
        case ClassMethod::Kind::Method:
            target.define_property_or_throw(property_key, { .value = method_value, .writable = true, .enumerable = false, .configurable = true });
            break;
        case ClassMethod::Kind::Getter:
            update_function_name(method_value, String::formatted("get {}", get_function_name(global_object, key)));
            target.define_property_or_throw(property_key, { .get = &method_function, .enumerable = true, .configurable = true });
            break;
        case ClassMethod::Kind::Setter:
            update_function_name(method_value, String::formatted("set {}", get_function_name(global_object, key)));
            target.define_property_or_throw(property_key, { .set = &method_function, .enumerable = true, .configurable = true });
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        if (interpreter.exception())
            return {};
    }

    for (auto& field : m_fields) {
        auto key = field.key().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        auto property_key = key.to_property_key(global_object);
        if (interpreter.exception())
            return {};

        FunctionObject* initializer = nullptr;
        if (field.initializer()) {
            auto copy_initializer = field.initializer();
            auto body = create_ast_node<ExpressionStatement>(field.initializer()->source_range(), copy_initializer.release_nonnull());
            // FIXME: A potential optimization is not creating the functions here since these are never directly accessible.
            initializer = OrdinaryFunctionObject::create(interpreter.global_object(), property_key.to_display_string(), *body, {}, 0, interpreter.lexical_environment(), FunctionKind::Regular, false);
            initializer->set_home_object(field.is_static() ? class_constructor : &class_prototype.as_object());
        }

        if (field.is_static()) {
            Value field_value = js_undefined();
            if (initializer)
                field_value = TRY_OR_DISCARD(interpreter.vm().call(*initializer, class_constructor_value));

            class_constructor->create_data_property_or_throw(property_key, field_value);
            if (interpreter.exception())
                return {};
        } else {
            class_constructor->add_field(property_key, initializer);
        }
    }

    return class_constructor;
}

Value ClassDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    Value class_constructor = m_class_expression->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    interpreter.lexical_environment()->put_into_environment(m_class_expression->name(), { class_constructor, DeclarationKind::Let });

    return {};
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
    if (!m_variables.is_empty()) {
        print_indent(indent + 1);
        outln("(Variables)");
        for (auto& variable : m_variables)
            variable.dump(indent + 2);
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
    case BinaryOp::TypedEquals:
        op_string = "===";
        break;
    case BinaryOp::TypedInequals:
        op_string = "!==";
        break;
    case BinaryOp::AbstractEquals:
        op_string = "==";
        break;
    case BinaryOp::AbstractInequals:
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
    outln("(Methods)");
    for (auto& method : m_methods)
        method.dump(indent + 1);

    print_indent(indent);
    outln("(Fields)");
    for (auto& field : m_fields)
        field.dump(indent + 1);
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
    outln("Static: {}", m_is_static);

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
    outln("Static: {}", m_is_static);

    if (m_initializer) {
        print_indent(indent);
        outln("(Initializer)");
        m_initializer->dump(indent + 1);
    }
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
    outln("{}{} '{}'", class_name, m_kind == FunctionKind::Generator ? "*" : "", name());
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
    lhs().dump(indent + 1);
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

void ForOfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForOf");
    lhs().dump(indent + 1);
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

Value Identifier::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto value = interpreter.vm().get_variable(string(), global_object);
    if (interpreter.exception())
        return {};
    if (value.is_empty()) {
        interpreter.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, string());
        return {};
    }
    return value;
}

void Identifier::dump(int indent) const
{
    print_indent(indent);
    outln("Identifier \"{}\"", m_string);
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

Value AssignmentExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

#define EXECUTE_LHS()                                                     \
    do {                                                                  \
        if (auto* ptr = m_lhs.get_pointer<NonnullRefPtr<Expression>>()) { \
            lhs_result = (*ptr)->execute(interpreter, global_object);     \
            if (interpreter.exception())                                  \
                return {};                                                \
        }                                                                 \
    } while (0)

#define EXECUTE_LHS_AND_RHS()                                    \
    do {                                                         \
        EXECUTE_LHS();                                           \
        rhs_result = m_rhs->execute(interpreter, global_object); \
        if (interpreter.exception())                             \
            return {};                                           \
    } while (0)

    Value lhs_result;
    Value rhs_result;
    switch (m_op) {
    case AssignmentOp::Assignment:
        break;
    case AssignmentOp::AdditionAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = add(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::SubtractionAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = sub(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::MultiplicationAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = mul(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::DivisionAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = div(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::ModuloAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = mod(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::ExponentiationAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = exp(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::BitwiseAndAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = bitwise_and(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::BitwiseOrAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = bitwise_or(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::BitwiseXorAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = bitwise_xor(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::LeftShiftAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = left_shift(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::RightShiftAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = right_shift(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        EXECUTE_LHS_AND_RHS();
        rhs_result = unsigned_right_shift(global_object, lhs_result, rhs_result);
        break;
    case AssignmentOp::AndAssignment:
        EXECUTE_LHS();
        if (!lhs_result.to_boolean())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    case AssignmentOp::OrAssignment:
        EXECUTE_LHS();
        if (lhs_result.to_boolean())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    case AssignmentOp::NullishAssignment:
        EXECUTE_LHS();
        if (!lhs_result.is_nullish())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    }
    if (interpreter.exception())
        return {};

    return m_lhs.visit(
        [&](NonnullRefPtr<Expression>& lhs) -> JS::Value {
            auto reference = lhs->to_reference(interpreter, global_object);
            if (interpreter.exception())
                return {};

            if (m_op == AssignmentOp::Assignment) {
                rhs_result = m_rhs->execute(interpreter, global_object);
                if (interpreter.exception())
                    return {};
            }

            if (reference.is_unresolvable()) {
                interpreter.vm().throw_exception<ReferenceError>(global_object, ErrorType::InvalidLeftHandAssignment);
                return {};
            }

            reference.put_value(global_object, rhs_result);
            if (interpreter.exception())
                return {};

            return rhs_result;
        },
        [&](NonnullRefPtr<BindingPattern>& pattern) -> JS::Value {
            VERIFY(m_op == AssignmentOp::Assignment);

            rhs_result = m_rhs->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};

            interpreter.vm().assign(pattern, rhs_result, global_object);
            if (interpreter.exception())
                return {};

            return rhs_result;
        });
}

Value UpdateExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto reference = m_argument->to_reference(interpreter, global_object);

    if (interpreter.exception())
        return {};
    auto old_value = reference.get_value(global_object);
    if (interpreter.exception())
        return {};
    old_value = old_value.to_numeric(global_object);
    if (interpreter.exception())
        return {};

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

    reference.put_value(global_object, new_value);
    if (interpreter.exception())
        return {};
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
            auto initializer_result = init->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            declarator.target().visit(
                [&](NonnullRefPtr<Identifier> const& id) {
                    auto variable_name = id->string();
                    if (is<ClassExpression>(*init))
                        update_function_name(initializer_result, variable_name);
                    interpreter.vm().set_variable(variable_name, initializer_result, global_object, true);
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    interpreter.vm().assign(pattern, initializer_result, global_object, true);
                });
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
    m_key->dump(indent + 1);
    m_value->dump(indent + 1);
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
                    auto value = array_to_spread.get(entry.index());
                    if (interpreter.exception())
                        return {};
                    object->indexed_properties().put(entry.index(), value);
                    if (interpreter.exception())
                        return {};
                }
            } else if (key.is_object()) {
                auto& obj_to_spread = key.as_object();

                for (auto& it : obj_to_spread.shape().property_table_ordered()) {
                    if (it.value.attributes.is_enumerable()) {
                        object->define_direct_property(it.key, obj_to_spread.get(it.key), JS::default_attributes);
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
            value.as_function().set_home_object(object);

        String name = get_function_name(global_object, key);
        if (property.type() == ObjectProperty::Type::Getter) {
            name = String::formatted("get {}", name);
        } else if (property.type() == ObjectProperty::Type::Setter) {
            name = String::formatted("set {}", name);
        }

        update_function_name(value, name);

        switch (property.type()) {
        case ObjectProperty::Type::Getter:
            VERIFY(value.is_function());
            object->define_direct_accessor(PropertyName::from_value(global_object, key), &value.as_function(), nullptr, Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::Setter:
            VERIFY(value.is_function());
            object->define_direct_accessor(PropertyName::from_value(global_object, key), nullptr, &value.as_function(), Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::KeyValue:
            object->define_direct_property(PropertyName::from_value(global_object, key), value, JS::default_attributes);
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

PropertyName MemberExpression::computed_property_name(Interpreter& interpreter, GlobalObject& global_object) const
{
    if (!is_computed())
        return verify_cast<Identifier>(*m_property).string();

    auto value = m_property->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    VERIFY(!value.is_empty());
    return PropertyName::from_value(global_object, value);
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
    return reference.get_value(global_object);
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
            });
    }
}

Optional<OptionalChain::ReferenceAndValue> OptionalChain::to_reference_and_value(JS::Interpreter& interpreter, JS::GlobalObject& global_object) const
{
    // Note: This is wrapped in an optional to allow base_reference = ...
    Optional<JS::Reference> base_reference = m_base->to_reference(interpreter, global_object);
    auto base = base_reference->is_unresolvable() ? m_base->execute(interpreter, global_object) : base_reference->get_value(global_object);
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
            });
        if (is<CallExpression>(*expression)) {
            base_reference = JS::Reference {};
            base = expression->execute(interpreter, global_object);
        } else {
            base_reference = expression->to_reference(interpreter, global_object);
            base = base_reference->get_value(global_object);
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

Value MetaProperty::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_type == MetaProperty::Type::NewTarget)
        return interpreter.vm().get_new_target().value_or(js_undefined());
    if (m_type == MetaProperty::Type::ImportMeta)
        TODO();
    VERIFY_NOT_REACHED();
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

    auto* array = Array::create(global_object, 0);
    array->indexed_properties();
    size_t index = 0;
    for (auto& element : m_elements) {
        auto value = Value();
        if (element) {
            value = element->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};

            if (is<SpreadExpression>(*element)) {
                get_iterator_values(global_object, value, [&](Value iterator_value) {
                    array->indexed_properties().put(index++, iterator_value, default_attributes);
                    return IterationDecision::Continue;
                });
                if (interpreter.exception())
                    return {};
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
        auto string = expr.to_string(global_object);
        if (interpreter.exception())
            return {};
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
    auto* strings = Array::create(global_object, 0);
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

    auto* raw_strings = Array::create(global_object, 0);
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

Value TryStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto result = interpreter.execute_statement(global_object, m_block, ScopeType::Try);
    if (auto* exception = interpreter.exception()) {
        if (m_handler) {
            interpreter.vm().clear_exception();

            HashMap<FlyString, Variable> parameters;
            m_handler->parameter().visit(
                [&](FlyString const& parameter) {
                    parameters.set(parameter, Variable { exception->value(), DeclarationKind::Var });
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    pattern->for_each_bound_name([&](auto& name) {
                        parameters.set(name, Variable { Value {}, DeclarationKind::Var });
                    });
                });
            auto* catch_scope = interpreter.heap().allocate<DeclarativeEnvironment>(global_object, move(parameters), interpreter.vm().running_execution_context().lexical_environment);
            TemporaryChange<Environment*> scope_change(interpreter.vm().running_execution_context().lexical_environment, catch_scope);

            if (auto* pattern = m_handler->parameter().get_pointer<NonnullRefPtr<BindingPattern>>())
                interpreter.vm().assign(*pattern, exception->value(), global_object, true);
            if (interpreter.exception())
                result = js_undefined();
            else
                result = interpreter.execute_statement(global_object, m_handler->body());
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

Value SwitchStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto discriminant_result = m_discriminant->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    bool falling_through = false;
    auto last_value = js_undefined();

    for (auto& switch_case : m_cases) {
        if (!falling_through && switch_case.test()) {
            auto test_result = switch_case.test()->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            if (!is_strictly_equal(discriminant_result, test_result))
                continue;
        }
        falling_through = true;

        for (auto& statement : switch_case.consequent()) {
            auto value = statement.execute(interpreter, global_object);
            if (!value.is_empty())
                last_value = value;
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                    // No stop_unwind(), the outer loop will handle that - we just need to break out of the switch/case.
                    return last_value;
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
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
    ASTNode::dump(indent);
    print_indent(indent + 1);
    if (m_test) {
        outln("(Test)");
        m_test->dump(indent + 2);
    } else {
        outln("(Default)");
    }
    print_indent(indent + 1);
    outln("(Consequent)");
    for (auto& statement : m_consequent)
        statement.dump(indent + 2);
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

void ScopeNode::add_variables(NonnullRefPtrVector<VariableDeclaration> variables)
{
    m_variables.extend(move(variables));
}

void ScopeNode::add_functions(NonnullRefPtrVector<FunctionDeclaration> functions)
{
    m_functions.extend(move(functions));
}

void ScopeNode::add_hoisted_function(NonnullRefPtr<FunctionDeclaration> hoisted_function)
{
    m_hoisted_functions.append(hoisted_function);
}

Value ImportStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    dbgln("Modules are not fully supported yet!");
    TODO();
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

}
