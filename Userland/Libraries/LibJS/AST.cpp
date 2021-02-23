/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/WithScope.h>
#include <typeinfo>

namespace JS {

String ASTNode::class_name() const
{
    // NOTE: We strip the "JS::" prefix.
    return demangle(typeid(*this).name()).substring(4);
}

static void update_function_name(Value value, const FlyString& name, HashTable<JS::Cell*>& visited)
{
    if (!value.is_object())
        return;
    if (visited.contains(value.as_cell()))
        return;
    visited.set(value.as_cell());
    auto& object = value.as_object();
    if (object.is_function()) {
        auto& function = static_cast<Function&>(object);
        if (is<ScriptFunction>(function) && function.name().is_empty())
            static_cast<ScriptFunction&>(function).set_name(name);
    } else if (object.is_array()) {
        auto& array = static_cast<Array&>(object);
        array.indexed_properties().for_each_value([&](auto& array_element_value) {
            update_function_name(array_element_value, name, visited);
        });
    }
}

static void update_function_name(Value value, const FlyString& name)
{
    HashTable<JS::Cell*> visited;
    update_function_name(value, name, visited);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return interpreter.execute_statement(global_object, *this);
}

Value Program::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return interpreter.execute_statement(global_object, *this, ScopeType::Block);
}

Value FunctionDeclaration::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return js_undefined();
}

Value FunctionExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return ScriptFunction::create(global_object, name(), body(), parameters(), function_length(), interpreter.current_scope(), is_strict_mode() || interpreter.vm().in_strict_mode(), m_is_arrow_function);
}

Value ExpressionStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return m_expression->execute(interpreter, global_object);
}

CallExpression::ThisAndCallee CallExpression::compute_this_and_callee(Interpreter& interpreter, GlobalObject& global_object) const
{
    auto& vm = interpreter.vm();

    if (is<NewExpression>(*this)) {
        // Computing |this| is irrelevant for "new" expression.
        return { js_undefined(), m_callee->execute(interpreter, global_object) };
    }

    if (is<SuperExpression>(*m_callee)) {
        // If we are calling super, |this| has not been initialized yet, and would not be meaningful to provide.
        auto new_target = vm.get_new_target();
        VERIFY(new_target.is_function());
        return { js_undefined(), new_target };
    }

    if (is<MemberExpression>(*m_callee)) {
        auto& member_expression = static_cast<const MemberExpression&>(*m_callee);
        bool is_super_property_lookup = is<SuperExpression>(member_expression.object());
        auto lookup_target = is_super_property_lookup ? interpreter.current_environment()->get_super_base() : member_expression.object().execute(interpreter, global_object);
        if (vm.exception())
            return {};
        if (is_super_property_lookup && lookup_target.is_nullish()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::ObjectPrototypeNullOrUndefinedOnSuperPropertyAccess, lookup_target.to_string_without_side_effects());
            return {};
        }

        auto* this_value = is_super_property_lookup ? &vm.this_value(global_object).as_object() : lookup_target.to_object(global_object);
        if (vm.exception())
            return {};
        auto property_name = member_expression.computed_property_name(interpreter, global_object);
        if (!property_name.is_valid())
            return {};
        auto callee = lookup_target.to_object(global_object)->get(property_name).value_or(js_undefined());
        return { this_value, callee };
    }
    return { &global_object, m_callee->execute(interpreter, global_object) };
}

Value CallExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto& vm = interpreter.vm();
    auto [this_value, callee] = compute_this_and_callee(interpreter, global_object);
    if (vm.exception())
        return {};

    VERIFY(!callee.is_empty());

    if (!callee.is_function()
        || (is<NewExpression>(*this) && (is<NativeFunction>(callee.as_object()) && !static_cast<NativeFunction&>(callee.as_object()).has_constructor()))) {
        String error_message;
        auto call_type = is<NewExpression>(*this) ? "constructor" : "function";
        if (is<Identifier>(*m_callee) || is<MemberExpression>(*m_callee)) {
            String expression_string;
            if (is<Identifier>(*m_callee)) {
                expression_string = static_cast<const Identifier&>(*m_callee).string();
            } else {
                expression_string = static_cast<const MemberExpression&>(*m_callee).to_string_approximation();
            }
            vm.throw_exception<TypeError>(global_object, ErrorType::IsNotAEvaluatedFrom, callee.to_string_without_side_effects(), call_type, expression_string);
        } else {
            vm.throw_exception<TypeError>(global_object, ErrorType::IsNotA, callee.to_string_without_side_effects(), call_type);
        }
        return {};
    }

    auto& function = callee.as_function();

    MarkedValueList arguments(vm.heap());
    arguments.ensure_capacity(m_arguments.size());

    for (size_t i = 0; i < m_arguments.size(); ++i) {
        auto value = m_arguments[i].value->execute(interpreter, global_object);
        if (vm.exception())
            return {};
        if (m_arguments[i].is_spread) {
            get_iterator_values(global_object, value, [&](Value iterator_value) {
                if (vm.exception())
                    return IterationDecision::Break;
                arguments.append(iterator_value);
                return IterationDecision::Continue;
            });
            if (vm.exception())
                return {};
        } else {
            arguments.append(value);
        }
    }

    Object* new_object = nullptr;
    Value result;
    if (is<NewExpression>(*this)) {
        result = vm.construct(function, function, move(arguments), global_object);
        if (result.is_object())
            new_object = &result.as_object();
    } else if (is<SuperExpression>(*m_callee)) {
        auto* super_constructor = interpreter.current_environment()->current_function()->prototype();
        // FIXME: Functions should track their constructor kind.
        if (!super_constructor || !super_constructor->is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, "Super constructor");
            return {};
        }
        result = vm.construct(static_cast<Function&>(*super_constructor), function, move(arguments), global_object);
        if (vm.exception())
            return {};

        interpreter.current_environment()->bind_this_value(global_object, result);
    } else {
        result = vm.call(function, this_value, move(arguments));
    }

    if (vm.exception())
        return {};

    if (is<NewExpression>(*this)) {
        if (result.is_object())
            return result;
        return new_object;
    }
    return result;
}

Value ReturnStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto value = argument() ? argument()->execute(interpreter, global_object) : js_undefined();
    if (interpreter.exception())
        return {};
    interpreter.vm().unwind(ScopeType::Function);
    return value;
}

Value IfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto predicate_result = m_predicate->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    if (predicate_result.to_boolean())
        return interpreter.execute_statement(global_object, *m_consequent);

    if (m_alternate)
        return interpreter.execute_statement(global_object, *m_alternate);

    return js_undefined();
}

Value WithStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto object_value = m_object->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    auto* object = object_value.to_object(global_object);
    if (interpreter.exception())
        return {};

    VERIFY(object);

    auto* with_scope = interpreter.heap().allocate<WithScope>(global_object, *object, interpreter.vm().call_frame().scope);
    TemporaryChange<ScopeObject*> scope_change(interpreter.vm().call_frame().scope, with_scope);
    interpreter.execute_statement(global_object, m_body);
    return {};
}

Value WhileStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    Value last_value = js_undefined();
    for (;;) {
        auto test_result = m_test->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!test_result.to_boolean())
            break;
        last_value = interpreter.execute_statement(global_object, *m_body);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    Value last_value = js_undefined();
    for (;;) {
        if (interpreter.exception())
            return {};
        last_value = interpreter.execute_statement(global_object, *m_body);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    RefPtr<BlockStatement> wrapper;

    if (m_init && is<VariableDeclaration>(*m_init) && static_cast<const VariableDeclaration&>(*m_init).declaration_kind() != DeclarationKind::Var) {
        wrapper = create_ast_node<BlockStatement>(source_range());
        NonnullRefPtrVector<VariableDeclaration> decls;
        decls.append(*static_cast<const VariableDeclaration*>(m_init.ptr()));
        wrapper->add_variables(decls);
        interpreter.enter_scope(*wrapper, ScopeType::Block, global_object);
    }

    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });

    Value last_value = js_undefined();

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
            last_value = interpreter.execute_statement(global_object, *m_body);
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
            last_value = interpreter.execute_statement(global_object, *m_body);
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

static FlyString variable_from_for_declaration(Interpreter& interpreter, GlobalObject& global_object, const ASTNode& node, RefPtr<BlockStatement> wrapper)
{
    FlyString variable_name;
    if (is<VariableDeclaration>(node)) {
        auto& variable_declaration = static_cast<const VariableDeclaration&>(node);
        VERIFY(!variable_declaration.declarations().is_empty());
        if (variable_declaration.declaration_kind() != DeclarationKind::Var) {
            wrapper = create_ast_node<BlockStatement>(node.source_range());
            interpreter.enter_scope(*wrapper, ScopeType::Block, global_object);
        }
        variable_declaration.execute(interpreter, global_object);
        variable_name = variable_declaration.declarations().first().id().string();
    } else if (is<Identifier>(node)) {
        variable_name = static_cast<const Identifier&>(node).string();
    } else {
        VERIFY_NOT_REACHED();
    }
    return variable_name;
}

Value ForInStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    if (!is<VariableDeclaration>(*m_lhs) && !is<Identifier>(*m_lhs)) {
        // FIXME: Implement "for (foo.bar in baz)", "for (foo[0] in bar)"
        VERIFY_NOT_REACHED();
    }
    RefPtr<BlockStatement> wrapper;
    auto variable_name = variable_from_for_declaration(interpreter, global_object, m_lhs, wrapper);
    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });
    auto last_value = js_undefined();
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto* object = rhs_result.to_object(global_object);
    while (object) {
        auto property_names = object->get_own_properties(*object, Object::PropertyKind::Key, true);
        for (auto& property_name : property_names.as_object().indexed_properties()) {
            interpreter.vm().set_variable(variable_name, property_name.value_and_attributes(object).value, global_object);
            if (interpreter.exception())
                return {};
            last_value = interpreter.execute_statement(global_object, *m_body);
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
        object = object->prototype();
        if (interpreter.exception())
            return {};
    }
    return last_value;
}

Value ForOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    if (!is<VariableDeclaration>(*m_lhs) && !is<Identifier>(*m_lhs)) {
        // FIXME: Implement "for (foo.bar of baz)", "for (foo[0] of bar)"
        VERIFY_NOT_REACHED();
    }
    RefPtr<BlockStatement> wrapper;
    auto variable_name = variable_from_for_declaration(interpreter, global_object, m_lhs, wrapper);
    auto wrapper_cleanup = ScopeGuard([&] {
        if (wrapper)
            interpreter.exit_scope(*wrapper);
    });
    auto last_value = js_undefined();
    auto rhs_result = m_rhs->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    get_iterator_values(global_object, rhs_result, [&](Value value) {
        interpreter.vm().set_variable(variable_name, value, global_object);
        last_value = interpreter.execute_statement(global_object, *m_body);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
        return Value(strict_eq(lhs_result, rhs_result));
    case BinaryOp::TypedInequals:
        return Value(!strict_eq(lhs_result, rhs_result));
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    return interpreter.vm().get_reference(string());
}

Reference MemberExpression::to_reference(Interpreter& interpreter, GlobalObject& global_object) const
{
    auto object_value = m_object->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto property_name = computed_property_name(interpreter, global_object);
    if (!property_name.is_valid())
        return {};
    return { object_value, property_name };
}

Value UnaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto& vm = interpreter.vm();
    if (m_op == UnaryOp::Delete) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (reference.is_unresolvable())
            return Value(true);
        // FIXME: Support deleting locals
        VERIFY(!reference.is_local_variable());
        if (reference.is_global_variable())
            return global_object.delete_property(reference.name());
        auto* base_object = reference.base().to_object(global_object);
        if (!base_object)
            return {};
        return base_object->delete_property(reference.name());
    }

    Value lhs_result;
    if (m_op == UnaryOp::Typeof && is<Identifier>(*m_lhs)) {
        auto reference = m_lhs->to_reference(interpreter, global_object);
        if (interpreter.exception()) {
            return {};
        }
        // FIXME: standard recommends checking with is_unresolvable but it ALWAYS return false here
        if (reference.is_local_variable() || reference.is_global_variable()) {
            auto name = reference.name();
            lhs_result = interpreter.vm().get_variable(name.to_string(), global_object).value_or(js_undefined());
            if (interpreter.exception())
                return {};
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
        switch (lhs_result.type()) {
        case Value::Type::Empty:
            VERIFY_NOT_REACHED();
            return {};
        case Value::Type::Undefined:
            return js_string(vm, "undefined");
        case Value::Type::Null:
            // yes, this is on purpose. yes, this is how javascript works.
            // yes, it's silly.
            return js_string(vm, "object");
        case Value::Type::Number:
            return js_string(vm, "number");
        case Value::Type::String:
            return js_string(vm, "string");
        case Value::Type::Object:
            if (lhs_result.is_function())
                return js_string(vm, "function");
            return js_string(vm, "object");
        case Value::Type::Boolean:
            return js_string(vm, "boolean");
        case Value::Type::Symbol:
            return js_string(vm, "symbol");
        case Value::Type::BigInt:
            return js_string(vm, "bigint");
        default:
            VERIFY_NOT_REACHED();
        }
    case UnaryOp::Void:
        return js_undefined();
    case UnaryOp::Delete:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

Value SuperExpression::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    // The semantics for SuperExpressions are handled in CallExpression::compute_this_and_callee()
    VERIFY_NOT_REACHED();
}

Value ClassMethod::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return m_function->execute(interpreter, global_object);
}

Value ClassExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto& vm = interpreter.vm();
    Value class_constructor_value = m_constructor->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    update_function_name(class_constructor_value, m_name);

    VERIFY(class_constructor_value.is_function() && is<ScriptFunction>(class_constructor_value.as_function()));
    ScriptFunction* class_constructor = static_cast<ScriptFunction*>(&class_constructor_value.as_function());
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
        class_constructor->set_constructor_kind(Function::ConstructorKind::Derived);
        Object* prototype = Object::create_empty(global_object);

        Object* super_constructor_prototype = nullptr;
        if (!super_constructor.is_null()) {
            auto super_constructor_prototype_value = super_constructor.as_object().get(vm.names.prototype).value_or(js_undefined());
            if (interpreter.exception())
                return {};
            if (!super_constructor_prototype_value.is_object() && !super_constructor_prototype_value.is_null()) {
                interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::ClassExtendsValueInvalidPrototype, super_constructor_prototype_value.to_string_without_side_effects());
                return {};
            }
            if (super_constructor_prototype_value.is_object())
                super_constructor_prototype = &super_constructor_prototype_value.as_object();
        }
        prototype->set_prototype(super_constructor_prototype);

        prototype->define_property(vm.names.constructor, class_constructor, 0);
        if (interpreter.exception())
            return {};
        class_constructor->define_property(vm.names.prototype, prototype, Attribute::Writable);
        if (interpreter.exception())
            return {};
        class_constructor->set_prototype(super_constructor.is_null() ? global_object.function_prototype() : &super_constructor.as_object());
    }

    auto class_prototype = class_constructor->get(vm.names.prototype);
    if (interpreter.exception())
        return {};

    if (!class_prototype.is_object()) {
        interpreter.vm().throw_exception<TypeError>(global_object, ErrorType::NotAnObject, "Class prototype");
        return {};
    }
    for (const auto& method : m_methods) {
        auto method_value = method.execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        auto& method_function = method_value.as_function();

        auto key = method.key().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        auto& target = method.is_static() ? *class_constructor : class_prototype.as_object();
        method_function.set_home_object(&target);

        if (method.kind() == ClassMethod::Kind::Method) {
            target.define_property(StringOrSymbol::from_value(global_object, key), method_value);
        } else {
            String accessor_name = [&] {
                switch (method.kind()) {
                case ClassMethod::Kind::Getter:
                    return String::formatted("get {}", get_function_name(global_object, key));
                case ClassMethod::Kind::Setter:
                    return String::formatted("set {}", get_function_name(global_object, key));
                default:
                    VERIFY_NOT_REACHED();
                }
            }();
            update_function_name(method_value, accessor_name);
            target.define_accessor(StringOrSymbol::from_value(global_object, key), method_function, method.kind() == ClassMethod::Kind::Getter, Attribute::Configurable | Attribute::Enumerable);
        }
        if (interpreter.exception())
            return {};
    }

    return class_constructor;
}

Value ClassDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    Value class_constructor = m_class_expression->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    interpreter.current_scope()->put_to_scope(m_class_expression->name(), { class_constructor, DeclarationKind::Let });

    return js_undefined();
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

void FunctionNode::dump(int indent, const String& class_name) const
{
    print_indent(indent);
    outln("{} '{}'", class_name, name());
    if (!m_parameters.is_empty()) {
        print_indent(indent + 1);
        outln("(Parameters)\n");

        for (auto& parameter : m_parameters) {
            print_indent(indent + 2);
            if (parameter.is_rest)
                out("...");
            outln("{}", parameter.name);
            if (parameter.default_value)
                parameter.default_value->dump(indent + 3);
        }
    }
    if (!m_variables.is_empty()) {
        print_indent(indent + 1);
        outln("(Variables)");

        for (auto& variable : m_variables)
            variable.dump(indent + 2);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto value = interpreter.vm().get_variable(string(), global_object);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return m_target->execute(interpreter, global_object);
}

Value ThisExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return interpreter.vm().resolve_this_binding(global_object);
}

void ThisExpression::dump(int indent) const
{
    ASTNode::dump(indent);
}

Value AssignmentExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

#define EXECUTE_LHS_AND_RHS()                                    \
    do {                                                         \
        lhs_result = m_lhs->execute(interpreter, global_object); \
        if (interpreter.exception())                             \
            return {};                                           \
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
        lhs_result = m_lhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!lhs_result.to_boolean())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    case AssignmentOp::OrAssignment:
        lhs_result = m_lhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (lhs_result.to_boolean())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    case AssignmentOp::NullishAssignment:
        lhs_result = m_lhs->execute(interpreter, global_object);
        if (interpreter.exception())
            return {};
        if (!lhs_result.is_nullish())
            return lhs_result;
        rhs_result = m_rhs->execute(interpreter, global_object);
        break;
    }
    if (interpreter.exception())
        return {};

    auto reference = m_lhs->to_reference(interpreter, global_object);
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
    update_function_name(rhs_result, get_function_name(global_object, reference.name().to_value(interpreter.vm())));
    reference.put(global_object, rhs_result);

    if (interpreter.exception())
        return {};
    return rhs_result;
}

Value UpdateExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto reference = m_argument->to_reference(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto old_value = reference.get(global_object);
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

    reference.put(global_object, new_value);
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
    m_lhs->dump(indent + 1);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    for (auto& declarator : m_declarations) {
        if (auto* init = declarator.init()) {
            auto initalizer_result = init->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            auto variable_name = declarator.id().string();
            update_function_name(initalizer_result, variable_name);
            interpreter.vm().set_variable(variable_name, initalizer_result, global_object, true);
        }
    }
    return js_undefined();
}

Value VariableDeclarator::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    m_id->dump(indent + 1);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    // NOTE: ObjectProperty execution is handled by ObjectExpression.
    VERIFY_NOT_REACHED();
}

Value ObjectExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto* object = Object::create_empty(global_object);
    for (auto& property : m_properties) {
        auto key = property.key().execute(interpreter, global_object);
        if (interpreter.exception())
            return {};

        if (property.type() == ObjectProperty::Type::Spread) {
            if (key.is_array()) {
                auto& array_to_spread = static_cast<Array&>(key.as_object());
                for (auto& entry : array_to_spread.indexed_properties()) {
                    object->indexed_properties().put(object, entry.index(), entry.value_and_attributes(&array_to_spread).value);
                    if (interpreter.exception())
                        return {};
                }
            } else if (key.is_object()) {
                auto& obj_to_spread = key.as_object();

                for (auto& it : obj_to_spread.shape().property_table_ordered()) {
                    if (it.value.attributes.is_enumerable()) {
                        object->define_property(it.key, obj_to_spread.get(it.key));
                        if (interpreter.exception())
                            return {};
                    }
                }
            } else if (key.is_string()) {
                auto& str_to_spread = key.as_string().string();

                for (size_t i = 0; i < str_to_spread.length(); i++) {
                    object->define_property(i, js_string(interpreter.heap(), str_to_spread.substring(i, 1)));
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

        if (property.type() == ObjectProperty::Type::Getter || property.type() == ObjectProperty::Type::Setter) {
            VERIFY(value.is_function());
            object->define_accessor(PropertyName::from_value(global_object, key), value.as_function(), property.type() == ObjectProperty::Type::Getter, Attribute::Configurable | Attribute::Enumerable);
            if (interpreter.exception())
                return {};
        } else {
            object->define_property(PropertyName::from_value(global_object, key), value);
            if (interpreter.exception())
                return {};
        }
    }
    return object;
}

void MemberExpression::dump(int indent) const
{
    print_indent(indent);
    outln("%{}(computed={})", class_name(), is_computed());
    m_object->dump(indent + 1);
    m_property->dump(indent + 1);
}

PropertyName MemberExpression::computed_property_name(Interpreter& interpreter, GlobalObject& global_object) const
{
    if (!is_computed()) {
        VERIFY(is<Identifier>(*m_property));
        return static_cast<const Identifier&>(*m_property).string();
    }
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
        object_string = static_cast<const Identifier&>(*m_object).string();
    if (is_computed())
        return String::formatted("{}[<computed>]", object_string);
    VERIFY(is<Identifier>(*m_property));
    return String::formatted("{}.{}", object_string, static_cast<const Identifier&>(*m_property).string());
}

Value MemberExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto object_value = m_object->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};
    auto* object_result = object_value.to_object(global_object);
    if (interpreter.exception())
        return {};
    auto property_name = computed_property_name(interpreter, global_object);
    if (!property_name.is_valid())
        return {};
    return object_result->get(property_name).value_or(js_undefined());
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    if (m_type == MetaProperty::Type::NewTarget)
        return interpreter.vm().get_new_target().value_or(js_undefined());
    if (m_type == MetaProperty::Type::ImportMeta)
        TODO();
    VERIFY_NOT_REACHED();
}

Value StringLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return js_string(interpreter.heap(), m_value);
}

Value NumericLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return Value(m_value);
}

Value BigIntLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base10(m_value.substring(0, m_value.length() - 1)));
}

Value BooleanLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return Value(m_value);
}

Value NullLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return js_null();
}

void RegExpLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("{} (/{}/{})", class_name(), content(), flags());
}

Value RegExpLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    return RegExpObject::create(global_object, content(), flags());
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto* array = Array::create(global_object);
    for (auto& element : m_elements) {
        auto value = Value();
        if (element) {
            value = element->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};

            if (is<SpreadExpression>(*element)) {
                get_iterator_values(global_object, value, [&](Value iterator_value) {
                    array->indexed_properties().append(iterator_value);
                    return IterationDecision::Continue;
                });
                if (interpreter.exception())
                    return {};
                continue;
            }
        }
        array->indexed_properties().append(value);
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    auto* strings = Array::create(global_object);
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

    auto* raw_strings = Array::create(global_object);
    for (auto& raw_string : m_template_literal->raw_strings()) {
        auto value = raw_string.execute(interpreter, global_object);
        if (vm.exception())
            return {};
        raw_strings->indexed_properties().append(value);
    }
    strings->define_property(vm.names.raw, raw_strings, 0);
    return vm.call(tag_function, js_undefined(), move(arguments));
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
    if (m_parameter.is_null())
        outln("CatchClause");
    else
        outln("CatchClause ({})", m_parameter);
    body().dump(indent + 1);
}

void ThrowStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    argument().dump(indent + 1);
}

Value TryStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    interpreter.execute_statement(global_object, m_block, ScopeType::Try);
    if (auto* exception = interpreter.exception()) {
        if (m_handler) {
            interpreter.vm().clear_exception();

            HashMap<FlyString, Variable> parameters;
            parameters.set(m_handler->parameter(), Variable { exception->value(), DeclarationKind::Var });
            auto* catch_scope = interpreter.heap().allocate<LexicalEnvironment>(global_object, move(parameters), interpreter.vm().call_frame().scope);
            TemporaryChange<ScopeObject*> scope_change(interpreter.vm().call_frame().scope, catch_scope);
            interpreter.execute_statement(global_object, m_handler->body());
        }
    }

    if (m_finalizer) {
        // Keep, if any, and then clear the current exception so we can
        // execute() the finalizer without an exception in our way.
        auto* previous_exception = interpreter.exception();
        interpreter.vm().clear_exception();
        interpreter.vm().stop_unwind();
        m_finalizer->execute(interpreter, global_object);
        // If we previously had an exception and the finalizer didn't
        // throw a new one, restore the old one.
        // FIXME: This will print debug output in throw_exception() for
        // a seconds time with m_should_log_exceptions enabled.
        if (previous_exception && !interpreter.exception())
            interpreter.vm().throw_exception(previous_exception);
    }

    return js_undefined();
}

Value CatchClause::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    // NOTE: CatchClause execution is handled by TryStatement.
    VERIFY_NOT_REACHED();
    return {};
}

Value ThrowStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto value = m_argument->execute(interpreter, global_object);
    if (interpreter.vm().exception())
        return {};
    interpreter.vm().throw_exception(global_object, value);
    return {};
}

Value SwitchStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    auto discriminant_result = m_discriminant->execute(interpreter, global_object);
    if (interpreter.exception())
        return {};

    bool falling_through = false;

    for (auto& switch_case : m_cases) {
        if (!falling_through && switch_case.test()) {
            auto test_result = switch_case.test()->execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            if (!strict_eq(discriminant_result, test_result))
                continue;
        }
        falling_through = true;

        for (auto& statement : switch_case.consequent()) {
            auto last_value = statement.execute(interpreter, global_object);
            if (interpreter.exception())
                return {};
            if (interpreter.vm().should_unwind()) {
                if (interpreter.vm().should_unwind_until(ScopeType::Continuable, m_label)) {
                    // No stop_unwind(), the outer loop will handle that - we just need to break out of the switch/case.
                    return {};
                } else if (interpreter.vm().should_unwind_until(ScopeType::Breakable, m_label)) {
                    interpreter.vm().stop_unwind();
                    return {};
                } else {
                    return last_value;
                }
            }
        }
    }

    return js_undefined();
}

Value SwitchCase::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    // NOTE: SwitchCase execution is handled by SwitchStatement.
    VERIFY_NOT_REACHED();
    return {};
}

Value BreakStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    interpreter.vm().unwind(ScopeType::Breakable, m_target_label);
    return js_undefined();
}

Value ContinueStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    interpreter.vm().unwind(ScopeType::Continuable, m_target_label);
    return js_undefined();
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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

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
    interpreter.enter_node(*this);
    ScopeGuard exit_node { [&] { interpreter.exit_node(*this); } };

    // Sorry, no JavaScript debugger available (yet)!
    return js_undefined();
}

void ScopeNode::add_variables(NonnullRefPtrVector<VariableDeclaration> variables)
{
    m_variables.append(move(variables));
}

void ScopeNode::add_functions(NonnullRefPtrVector<FunctionDeclaration> functions)
{
    m_functions.append(move(functions));
}

}
