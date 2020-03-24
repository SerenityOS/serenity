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
    return function;
}

Value FunctionExpression::execute(Interpreter& interpreter) const
{
    return interpreter.heap().allocate<ScriptFunction>(body(), parameters());
}

Value ExpressionStatement::execute(Interpreter& interpreter) const
{
    return m_expression->execute(interpreter);
}

Value CallExpression::execute(Interpreter& interpreter) const
{
    auto callee = m_callee->execute(interpreter);
    if (interpreter.exception())
        return {};

    ASSERT(callee.is_object());
    ASSERT(callee.as_object()->is_function());
    auto* function = static_cast<Function*>(callee.as_object());

    auto& call_frame = interpreter.push_call_frame();
    for (size_t i = 0; i < m_arguments.size(); ++i) {
        call_frame.arguments.append(m_arguments[i].execute(interpreter));
        if (interpreter.exception())
            return {};
    }

    if (m_callee->is_member_expression()) {
        auto object_value = static_cast<const MemberExpression&>(*m_callee).object().execute(interpreter);
        if (interpreter.exception())
            return {};
        auto this_value = object_value.to_object(interpreter.heap());
        if (interpreter.exception())
            return {};
        call_frame.this_value = this_value;
    }

    auto result = function->call(interpreter, call_frame.arguments);
    interpreter.pop_call_frame();
    return result;
}

Value ReturnStatement::execute(Interpreter& interpreter) const
{
    auto value = argument() ? argument()->execute(interpreter) : js_undefined();
    interpreter.unwind(ScopeType::Function);
    return value;
}

Value IfStatement::execute(Interpreter& interpreter) const
{
    auto predicate_result = m_predicate->execute(interpreter);

    if (predicate_result.to_boolean())
        return interpreter.run(*m_consequent);

    if (m_alternate)
        return interpreter.run(*m_alternate);

    return js_undefined();
}

Value WhileStatement::execute(Interpreter& interpreter) const
{
    Value last_value = js_undefined();
    while (m_predicate->execute(interpreter).to_boolean()) {
        last_value = interpreter.run(*m_body);
    }

    return last_value;
}

Value ForStatement::execute(Interpreter& interpreter) const
{
    RefPtr<BlockStatement> wrapper;

    if (m_init->is_variable_declaration() && static_cast<const VariableDeclaration*>(m_init.ptr())->declaration_type() != DeclarationType::Var) {
        wrapper = create_ast_node<BlockStatement>();
        interpreter.enter_scope(*wrapper, {}, ScopeType::Block);
    }

    Value last_value = js_undefined();

    if (m_init)
        m_init->execute(interpreter);

    if (m_test) {
        while (m_test->execute(interpreter).to_boolean()) {
            last_value = interpreter.run(*m_body);
            if (m_update)
                m_update->execute(interpreter);
        }
    } else {
        while (true) {
            last_value = interpreter.run(*m_body);
            if (m_update)
                m_update->execute(interpreter);
        }
    }

    if (wrapper)
        interpreter.exit_scope(*wrapper);

    return last_value;
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
    }

    ASSERT_NOT_REACHED();
}

Value LogicalExpression::execute(Interpreter& interpreter) const
{
    auto lhs_result = m_lhs->execute(interpreter).to_boolean();
    auto rhs_result = m_rhs->execute(interpreter).to_boolean();
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
    auto value = interpreter.get_variable(string());
    if (value.is_undefined())
        return interpreter.throw_exception(interpreter.heap().allocate<Error>("ReferenceError", String::format("'%s' not known", string().characters())));
    return value;
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
            auto object = static_cast<const MemberExpression&>(*m_lhs).object().execute(interpreter).to_object(interpreter.heap());
            ASSERT(object.is_object());
            auto property_name = static_cast<const MemberExpression&>(*m_lhs).computed_property_name(interpreter);
            object.as_object()->put(property_name, value);
        };
    } else {
        ASSERT_NOT_REACHED();
    }

    auto rhs_result = m_rhs->execute(interpreter);

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
    commit(rhs_result);
    return rhs_result;
}

Value UpdateExpression::execute(Interpreter& interpreter) const
{
    ASSERT(m_argument->is_identifier());
    auto name = static_cast<const Identifier&>(*m_argument).string();

    auto previous_value = interpreter.get_variable(name);
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
        interpreter.set_variable(name().string(), initalizer_result, true);
    }

    return js_undefined();
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
    for (auto it : m_properties)
        object->put(it.key, it.value->execute(interpreter));
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
    auto object_result = m_object->execute(interpreter).to_object(interpreter.heap());
    ASSERT(object_result.is_object());
    return object_result.as_object()->get(computed_property_name(interpreter));
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
    return js_undefined();
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
        array->push(element.execute(interpreter));
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

Value TryStatement::execute(Interpreter& interpreter) const
{
    interpreter.run(block(), {}, ScopeType::Try);
    if (auto* exception = interpreter.exception()) {
        if (m_handler) {
            interpreter.clear_exception();
            Vector<Argument> arguments { { m_handler->parameter(), Value(exception) } };
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

}
