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

#include <LibJS/AST.h>
#include <LibJS/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Value.h>
#include <stdio.h>

namespace JS {

Value ScopeNode::execute(Interpreter& interpreter) const
{
    return interpreter.run(*this);
}

Value FunctionDeclaration::execute(Interpreter& interpreter) const
{
    auto* function = interpreter.heap().allocate<Function>(name(), body());
    interpreter.global_object().put(m_name, Value(function));
    return Value(function);
}

Value CallExpression::execute(Interpreter& interpreter) const
{
    auto callee = interpreter.global_object().get(name());
    ASSERT(callee.is_object());
    auto* callee_object = callee.as_object();
    ASSERT(callee_object->is_function());
    auto& function = static_cast<Function&>(*callee_object);
    return interpreter.run(function.body());
}

Value ReturnStatement::execute(Interpreter& interpreter) const
{
    auto value = argument().execute(interpreter);
    interpreter.do_return();
    return value;
}

Value IfStatement::execute(Interpreter& interpreter) const
{
    auto predicate_result = m_predicate->execute(interpreter);

    if (predicate_result.as_bool())
        return interpreter.run(*m_consequent);
    else
        return interpreter.run(*m_alternate);
}

Value add(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() + rhs.as_double());
}

Value sub(Value lhs, Value rhs)
{
    ASSERT(lhs.is_number());
    ASSERT(rhs.is_number());
    return Value(lhs.as_double() - rhs.as_double());
}

const Value typed_eq(const Value lhs, const Value rhs)
{
    if (rhs.type() != lhs.type())
        return Value(false);

    switch (lhs.type()) {
    case Value::Type::Undefined:
        return Value(true);
    case Value::Type::Null:
        return Value(true);
    case Value::Type::Number:
        return Value(lhs.as_double() == rhs.as_double());
    case Value::Type::String:
        return Value(lhs.as_string() == rhs.as_string());
    case Value::Type::Boolean:
        return Value(lhs.as_bool() == rhs.as_bool());
    case Value::Type::Object:
        return Value(lhs.as_object() == rhs.as_object());
    }

    ASSERT_NOT_REACHED();
}

Value BinaryExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter);
    auto rhs_result = m_rhs->execute(interpreter);

    switch (m_op) {
    case BinaryOp::Plus:
        return add(lhs_result, rhs_result);
    case BinaryOp::Minus:
        return sub(lhs_result, rhs_result);
    case BinaryOp::TypedEquals:
        return typed_eq(lhs_result, rhs_result);
    }

    ASSERT_NOT_REACHED();
}

Value LogicalExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter).as_bool();

    if (m_op == LogicalOp::Not)
        return Value(!lhs_result);

    auto rhs_result = m_rhs->execute(interpreter).as_bool();
    switch (m_op) {
    case LogicalOp::And:
        return Value(lhs_result && rhs_result);
    case LogicalOp::Or:
        return Value(lhs_result || rhs_result);
    case LogicalOp::Not:
        ASSERT_NOT_REACHED();
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
    case BinaryOp::TypedEquals:
        op_string = "===";
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
    case LogicalOp::Not:
        op_string = "!";
        print_indent(indent);
        printf("%s\n", class_name());
        print_indent(indent + 1);
        printf("%s\n", op_string);
        m_lhs->dump(indent + 1);
        return;
    }

    print_indent(indent);
    printf("%s\n", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    printf("%s\n", op_string);
    m_rhs->dump(indent + 1);
}

void CallExpression::dump(int indent) const
{
    print_indent(indent);
    printf("%s '%s'\n", class_name(), name().characters());
}

void Literal::dump(int indent) const
{
    print_indent(indent);
    if (m_value.is_object())
        ASSERT_NOT_REACHED();

    if (m_value.is_string())
        printf("%s\n", m_value.as_string()->characters());
    else
        printf("%s\n", m_value.to_string().characters());
}

void FunctionDeclaration::dump(int indent) const
{
    print_indent(indent);
    printf("%s '%s'\n", class_name(), name().characters());
    body().dump(indent + 1);
}

void ReturnStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    argument().dump(indent + 1);
}

void IfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    printf("If\n");
    predicate().dump(indent + 1);
    consequent().dump(indent + 1);
    print_indent(indent);
    printf("Else\n");
    alternate().dump(indent + 1);
}

}
