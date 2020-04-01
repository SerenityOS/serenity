/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>
#include <stdio.h>

namespace JS {

Value ScopeNode::execute(Interpreter& interpreter) const
{
    return interpreter.run(*this);
}

Value FunctionDeclaration::execute(Interpreter& interpreter) const
{
    auto* function = interpreter.heap().allocate<ScriptFunction>(body(), parameters());
    interpreter.set_variable(name(), function);
    return {};
}

Value FunctionExpression::execute(Interpreter& interpreter) const
{
    return interpreter.heap().allocate<ScriptFunction>(body(), parameters());
}

Value ExpressionStatement::execute(Interpreter& interpreter) const
{
    return m_expression->execute(interpreter);
}

CallExpression::ThisAndCallee CallExpression::compute_this_and_callee(Interpreter& interpreter) const
{
    if (is_new_expression()) {
        // Computing |this| is irrelevant for "new" expression.
        return { {}, m_callee->execute(interpreter) };
    }

    if (m_callee->is_member_expression()) {
        auto& member_expression = static_cast<const MemberExpression&>(*m_callee);
        auto object_value = member_expression.object().execute(interpreter);
        if (interpreter.exception())
            return {};
        auto* this_value = object_value.to_object(interpreter.heap());
        if (interpreter.exception())
            return {};
        auto callee = this_value->get(member_expression.computed_property_name(interpreter)).value_or({});
        return { this_value, callee };
    }
    return { &interpreter.global_object(), m_callee->execute(interpreter) };
}

Value CallExpression::execute(Interpreter& interpreter) const
{
    auto [this_value, callee] = compute_this_and_callee(interpreter);
    if (interpreter.exception())
        return {};

    if (!callee.is_object() || !callee.as_object()->is_function())
        return interpreter.throw_exception<Error>("TypeError", String::format("%s is not a function", callee.to_string().characters()));

    auto* function = static_cast<Function*>(callee.as_object());

    auto& call_frame = interpreter.push_call_frame();
    for (size_t i = 0; i < m_arguments.size(); ++i) {
        auto value = m_arguments[i].execute(interpreter);
        if (interpreter.exception())
            return {};
        call_frame.arguments.append(value);
        if (interpreter.exception())
            return {};
    }

    Object* new_object = nullptr;
    if (is_new_expression()) {
        new_object = interpreter.heap().allocate<Object>();
        auto prototype = function->get("prototype");
        if (prototype.has_value() && prototype.value().is_object())
            new_object->set_prototype(prototype.value().as_object());
        call_frame.this_value = new_object;
    } else {
        call_frame.this_value = this_value;
    }

    auto result = function->call(interpreter);
    interpreter.pop_call_frame();

    if (is_new_expression()) {
        if (result.is_object())
            return result;
        return new_object;
    }
    return result;
}

Value ReturnStatement::execute(Interpreter& interpreter) const
{
    auto value = argument() ? argument()->execute(interpreter) : js_undefined();
    if (interpreter.exception())
        return {};
    interpreter.unwind(ScopeType::Function);
    return value;
}

Value IfStatement::execute(Interpreter& interpreter) const
{
    auto predicate_result = m_predicate->execute(interpreter);
    if (interpreter.exception())
        return {};

    if (predicate_result.to_boolean())
        return interpreter.run(*m_consequent);

    if (m_alternate)
        return interpreter.run(*m_alternate);

    return {};
}

Value WhileStatement::execute(Interpreter& interpreter) const
{
    Value last_value = js_undefined();
    while (m_predicate->execute(interpreter).to_boolean()) {
        if (interpreter.exception())
            return {};
        last_value = interpreter.run(*m_body);
        if (interpreter.exception())
            return {};
    }

    return last_value;
}

Value ForStatement::execute(Interpreter& interpreter) const
{
    RefPtr<BlockStatement> wrapper;

    if (m_init && m_init->is_variable_declaration() && static_cast<const VariableDeclaration*>(m_init.ptr())->declaration_type() != DeclarationType::Var) {
        wrapper = create_ast_node<BlockStatement>();
        interpreter.enter_scope(*wrapper, {}, ScopeType::Block);
    }

    Value last_value = js_undefined();

    if (m_init) {
        m_init->execute(interpreter);
        if (interpreter.exception())
            return {};
    }

    if (m_test) {
        while (m_test->execute(interpreter).to_boolean()) {
            if (interpreter.exception())
                return {};
            last_value = interpreter.run(*m_body);
            if (interpreter.exception())
                return {};
            if (m_update) {
                m_update->execute(interpreter);
                if (interpreter.exception())
                    return {};
            }
        }
    } else {
        while (true) {
            last_value = interpreter.run(*m_body);
            if (interpreter.exception())
                return {};
            if (m_update) {
                m_update->execute(interpreter);
                if (interpreter.exception())
                    return {};
            }
        }
    }

    if (wrapper)
        interpreter.exit_scope(*wrapper);

    return last_value;
}

Value BinaryExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_result = m_rhs->execute(interpreter);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case BinaryOp::Plus:
        return add(lhs_result, rhs_result);
    case BinaryOp::Minus:
        return sub(lhs_result, rhs_result);
    case BinaryOp::Asterisk:
        return mul(lhs_result, rhs_result);
    case BinaryOp::Slash:
        return div(lhs_result, rhs_result);
    case BinaryOp::TypedEquals:
        return typed_eq(lhs_result, rhs_result);
    case BinaryOp::TypedInequals:
        return Value(!typed_eq(lhs_result, rhs_result).as_bool());
    case BinaryOp::AbstractEquals:
        return eq(lhs_result, rhs_result);
    case BinaryOp::AbstractInequals:
        return Value(!eq(lhs_result, rhs_result).as_bool());
    case BinaryOp::GreaterThan:
        return greater_than(lhs_result, rhs_result);
    case BinaryOp::GreaterThanEquals:
        return greater_than_equals(lhs_result, rhs_result);
    case BinaryOp::LessThan:
        return less_than(lhs_result, rhs_result);
    case BinaryOp::LessThanEquals:
        return less_than_equals(lhs_result, rhs_result);
    case BinaryOp::BitwiseAnd:
        return bitwise_and(lhs_result, rhs_result);
    case BinaryOp::BitwiseOr:
        return bitwise_or(lhs_result, rhs_result);
    case BinaryOp::BitwiseXor:
        return bitwise_xor(lhs_result, rhs_result);
    case BinaryOp::LeftShift:
        return left_shift(lhs_result, rhs_result);
    case BinaryOp::RightShift:
        return right_shift(lhs_result, rhs_result);
    case BinaryOp::InstanceOf:
        return instance_of(lhs_result, rhs_result);
    }

    ASSERT_NOT_REACHED();
}

Value LogicalExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter).to_boolean();
    if (interpreter.exception())
        return {};
    auto rhs_result = m_rhs->execute(interpreter).to_boolean();
    if (interpreter.exception())
        return {};
    switch (m_op) {
    case LogicalOp::And:
        return Value(lhs_result && rhs_result);
    case LogicalOp::Or:
        return Value(lhs_result || rhs_result);
    }

    ASSERT_NOT_REACHED();
}

Value UnaryExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter);
    switch (m_op) {
    case UnaryOp::BitwiseNot:
        return bitwise_not(lhs_result);
    case UnaryOp::Not:
        return Value(!lhs_result.to_boolean());
    case UnaryOp::Typeof:
        switch (lhs_result.type()) {
        case Value::Type::Undefined:
            return js_string(interpreter.heap(), "undefined");
        case Value::Type::Null:
            // yes, this is on purpose. yes, this is how javascript works.
            // yes, it's silly.
            return js_string(interpreter.heap(), "object");
        case Value::Type::Number:
            return js_string(interpreter.heap(), "number");
        case Value::Type::String:
            return js_string(interpreter.heap(), "string");
        case Value::Type::Object:
            return js_string(interpreter.heap(), "object");
        case Value::Type::Boolean:
            return js_string(interpreter.heap(), "boolean");
        }
    }

    ASSERT_NOT_REACHED();
}

static void print_indent(int indent)
{
    for (int i = 0; i < indent * 2; ++i)
        putchar(' ');
}

void ASTNode::dump(int indent) const
{
    print_indent(indent);
    printf("%s\n", class_name());
}

void ScopeNode::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& child : children())
        child.dump(indent + 1);
}

void BinaryExpression::dump(int indent) const
{
    const char* op_string = nullptr;
    switch (m_op) {
    case BinaryOp::Plus:
        op_string = "+";
        break;
    case BinaryOp::Minus:
        op_string = "-";
        break;
    case BinaryOp::Asterisk:
        op_string = "*";
        break;
    case BinaryOp::Slash:
        op_string = "/";
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
    case BinaryOp::InstanceOf:
        op_string = "instanceof";
        break;
    }

    print_indent(indent);
    printf("%s\n", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    printf("%s\n", op_string);
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
    }

    print_indent(indent);
    printf("%s\n", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    printf("%s\n", op_string);
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
    case UnaryOp::Typeof:
        op_string = "typeof ";
        break;
    }

    print_indent(indent);
    printf("%s\n", class_name());
    print_indent(indent + 1);
    printf("%s\n", op_string);
    m_lhs->dump(indent + 1);
}

void CallExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    m_callee->dump(indent + 1);
    for (auto& argument : m_arguments)
        argument.dump(indent + 1);
}

void StringLiteral::dump(int indent) const
{
    print_indent(indent);
    printf("StringLiteral \"%s\"\n", m_value.characters());
}

void NumericLiteral::dump(int indent) const
{
    print_indent(indent);
    printf("NumericLiteral %g\n", m_value);
}

void BooleanLiteral::dump(int indent) const
{
    print_indent(indent);
    printf("BooleanLiteral %s\n", m_value ? "true" : "false");
}

void UndefinedLiteral::dump(int indent) const
{
    print_indent(indent);
    printf("undefined\n");
}

void NullLiteral::dump(int indent) const
{
    print_indent(indent);
    printf("null\n");
}

void FunctionNode::dump(int indent, const char* class_name) const
{
    StringBuilder parameters_builder;
    parameters_builder.join(',', parameters());

    print_indent(indent);
    printf("%s '%s(%s)'\n", class_name, name().characters(), parameters_builder.build().characters());
    body().dump(indent + 1);
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
    printf("If\n");
    predicate().dump(indent + 1);
    consequent().dump(indent + 1);
    if (alternate()) {
        print_indent(indent);
        printf("Else\n");
        alternate()->dump(indent + 1);
    }
}

void WhileStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    printf("While\n");
    predicate().dump(indent + 1);
    body().dump(indent + 1);
}

void ForStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    printf("For\n");
    if (init())
        init()->dump(indent + 1);
    if (test())
        test()->dump(indent + 1);
    if (update())
        update()->dump(indent + 1);
    body().dump(indent + 1);
}

Value Identifier::execute(Interpreter& interpreter) const
{
    auto variable = interpreter.get_variable(string());
    if (!variable.has_value())
        return interpreter.throw_exception<Error>("ReferenceError", String::format("'%s' not known", string().characters()));
    return variable.value();
}

void Identifier::dump(int indent) const
{
    print_indent(indent);
    printf("Identifier \"%s\"\n", m_string.characters());
}

Value AssignmentExpression::execute(Interpreter& interpreter) const
{
    AK::Function<void(Value)> commit;
    if (m_lhs->is_identifier()) {
        commit = [&](Value value) {
            auto name = static_cast<const Identifier&>(*m_lhs).string();
            interpreter.set_variable(name, value);
        };
    } else if (m_lhs->is_member_expression()) {
        commit = [&](Value value) {
            if (auto* object = static_cast<const MemberExpression&>(*m_lhs).object().execute(interpreter).to_object(interpreter.heap())) {
                auto property_name = static_cast<const MemberExpression&>(*m_lhs).computed_property_name(interpreter);
                object->put(property_name, value);
            }
        };
    } else {
        ASSERT_NOT_REACHED();
    }

    auto rhs_result = m_rhs->execute(interpreter);
    if (interpreter.exception())
        return {};

    switch (m_op) {
    case AssignmentOp::Assignment:
        break;
    case AssignmentOp::AdditionAssignment:
        rhs_result = add(m_lhs->execute(interpreter), rhs_result);
        break;
    case AssignmentOp::SubtractionAssignment:
        rhs_result = sub(m_lhs->execute(interpreter), rhs_result);
        break;
    case AssignmentOp::MultiplicationAssignment:
        rhs_result = mul(m_lhs->execute(interpreter), rhs_result);
        break;
    case AssignmentOp::DivisionAssignment:
        rhs_result = div(m_lhs->execute(interpreter), rhs_result);
        break;
    }
    if (interpreter.exception())
        return {};
    commit(rhs_result);
    return rhs_result;
}

Value UpdateExpression::execute(Interpreter& interpreter) const
{
    ASSERT(m_argument->is_identifier());
    auto name = static_cast<const Identifier&>(*m_argument).string();

    auto previous_variable = interpreter.get_variable(name);
    ASSERT(previous_variable.has_value());
    auto previous_value = previous_variable.value();
    ASSERT(previous_value.is_number());

    int op_result = 0;
    switch (m_op) {
    case UpdateOp::Increment:
        op_result = 1;
        break;
    case UpdateOp::Decrement:
        op_result = -1;
        break;
    }

    interpreter.set_variable(name, Value(previous_value.as_double() + op_result));

    if (m_prefixed)
        return JS::Value(previous_value.as_double() + op_result);

    return previous_value;
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
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    printf("%s\n", op_string);
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
    print_indent(indent + 1);
    if (m_prefixed)
        printf("%s\n", op_string);
    m_argument->dump(indent + 1);
    if (!m_prefixed) {
        print_indent(indent + 1);
        printf("%s\n", op_string);
    }
}

Value VariableDeclaration::execute(Interpreter& interpreter) const
{
    interpreter.declare_variable(name().string(), m_declaration_type);
    if (m_initializer) {
        auto initalizer_result = m_initializer->execute(interpreter);
        if (interpreter.exception())
            return {};
        interpreter.set_variable(name().string(), initalizer_result, true);
    }

    return {};
}

void VariableDeclaration::dump(int indent) const
{
    const char* declaration_type_string = nullptr;
    switch (m_declaration_type) {
    case DeclarationType::Let:
        declaration_type_string = "Let";
        break;
    case DeclarationType::Var:
        declaration_type_string = "Var";
        break;
    case DeclarationType::Const:
        declaration_type_string = "Const";
        break;
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    printf("%s\n", declaration_type_string);
    m_name->dump(indent + 1);
    if (m_initializer)
        m_initializer->dump(indent + 1);
}

void ObjectExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto it : m_properties) {
        print_indent(indent + 1);
        printf("%s: ", it.key.characters());
        it.value->dump(0);
    }
}

void ExpressionStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_expression->dump(indent + 1);
}

Value ObjectExpression::execute(Interpreter& interpreter) const
{
    auto object = interpreter.heap().allocate<Object>();
    for (auto it : m_properties) {
        auto value = it.value->execute(interpreter);
        if (interpreter.exception())
            return {};
        object->put(it.key, value);
    }
    return object;
}

void MemberExpression::dump(int indent) const
{
    print_indent(indent);
    printf("%s (computed=%s)\n", class_name(), is_computed() ? "true" : "false");
    m_object->dump(indent + 1);
    m_property->dump(indent + 1);
}

FlyString MemberExpression::computed_property_name(Interpreter& interpreter) const
{
    if (!is_computed()) {
        ASSERT(m_property->is_identifier());
        return static_cast<const Identifier&>(*m_property).string();
    }
    return m_property->execute(interpreter).to_string();
}

Value MemberExpression::execute(Interpreter& interpreter) const
{
    auto* object_result = m_object->execute(interpreter).to_object(interpreter.heap());
    if (interpreter.exception())
        return {};
    auto result = object_result->get(computed_property_name(interpreter));
    return result.value_or({});
}

Value StringLiteral::execute(Interpreter& interpreter) const
{
    return js_string(interpreter.heap(), m_value);
}

Value NumericLiteral::execute(Interpreter&) const
{
    return Value(m_value);
}

Value BooleanLiteral::execute(Interpreter&) const
{
    return Value(m_value);
}

Value UndefinedLiteral::execute(Interpreter&) const
{
    return {};
}

Value NullLiteral::execute(Interpreter&) const
{
    return js_null();
}

void ArrayExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& element : m_elements) {
        element.dump(indent + 1);
    }
}

Value ArrayExpression::execute(Interpreter& interpreter) const
{
    auto* array = interpreter.heap().allocate<Array>();
    for (auto& element : m_elements) {
        auto value = element.execute(interpreter);
        if (interpreter.exception())
            return {};
        array->push(value);
    }
    return array;
}

void TryStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    printf("(Block)\n");
    block().dump(indent + 1);

    if (handler()) {
        print_indent(indent);
        printf("(Handler)\n");
        handler()->dump(indent + 1);
    }

    if (finalizer()) {
        print_indent(indent);
        printf("(Finalizer)\n");
        finalizer()->dump(indent + 1);
    }
}

void CatchClause::dump(int indent) const
{
    print_indent(indent);
    printf("CatchClause");
    if (!m_parameter.is_null())
        printf(" (%s)", m_parameter.characters());
    printf("\n");
    body().dump(indent + 1);
}

void ThrowStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    argument().dump(indent + 1);
}

Value TryStatement::execute(Interpreter& interpreter) const
{
    interpreter.run(block(), {}, ScopeType::Try);
    if (auto* exception = interpreter.exception()) {
        if (m_handler) {
            interpreter.clear_exception();
            Vector<Argument> arguments { { m_handler->parameter(), exception->value() } };
            interpreter.run(m_handler->body(), move(arguments));
        }
    }

    if (m_finalizer)
        m_finalizer->execute(interpreter);

    return {};
}

Value CatchClause::execute(Interpreter&) const
{
    // NOTE: CatchClause execution is handled by TryStatement.
    ASSERT_NOT_REACHED();
    return {};
}

Value ThrowStatement::execute(Interpreter& interpreter) const
{
    auto value = m_argument->execute(interpreter);
    if (interpreter.exception())
        return {};
    return interpreter.throw_exception(value);
}

Value SwitchStatement::execute(Interpreter& interpreter) const
{
    auto discriminant_result = m_discriminant->execute(interpreter);
    if (interpreter.exception())
        return {};

    bool falling_through = false;

    for (auto& switch_case : m_cases) {
        if (!falling_through && switch_case.test()) {
            auto test_result = switch_case.test()->execute(interpreter);
            if (interpreter.exception())
                return {};
            if (!eq(discriminant_result, test_result).to_boolean())
                continue;
        }
        falling_through = true;

        for (auto& statement : switch_case.consequent()) {
            statement.execute(interpreter);
            if (interpreter.exception())
                return {};
            if (interpreter.should_unwind())
                return {};
        }
    }

    return {};
}

Value SwitchCase::execute(Interpreter& interpreter) const
{
    (void)interpreter;
    return {};
}

Value BreakStatement::execute(Interpreter& interpreter) const
{
    interpreter.unwind(ScopeType::Breakable);
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
    print_indent(indent);
    if (m_test) {
        printf("(Test)\n");
        m_test->dump(indent + 1);
    } else {
        printf("(Default)\n");
    }
    print_indent(indent);
    printf("(Consequent)\n");
    int i = 0;
    for (auto& statement : m_consequent) {
        print_indent(indent);
        printf("[%d]\n", i++);
        statement.dump(indent + 1);
    }
}
}
