/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include "AST.h"
#include "AK/LogStream.h"

namespace Cpp {

static void print_indent(int indent)
{
    for (int i = 0; i < indent * 2; ++i)
        dbgprintf(" ");
}

void ASTNode::dump(size_t indent) const
{
    print_indent(indent);
    dbgprintf("%s[%lu:%lu->%lu:%lu]\n", class_name(), start().line, start().column, end().line, end().column);
}

void TranslationUnit::dump(size_t indent) const
{
    ASTNode::dump(indent);
    for (const auto& child : m_children) {
        child.dump(indent + 1);
    }
}

void FunctionDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_return_type->dump(indent + 1);
    if (!m_name.is_null()) {
        print_indent(indent + 1);
        dbgprintf("%s\n", m_name.to_string().characters());
    }
    print_indent(indent + 1);
    dbgprintf("(\n");
    for (const auto& arg : m_parameters) {
        arg.dump(indent + 1);
    }
    print_indent(indent + 1);
    dbgprintf(")\n");
    if (!m_definition.is_null()) {
        m_definition->dump(indent + 1);
    }
}

NonnullRefPtrVector<Declaration> FunctionDeclaration::declarations() const
{
    NonnullRefPtrVector<Declaration> declarations;
    for (auto& arg : m_parameters) {
        declarations.append(arg);
    }
    return declarations;
}

void Type::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    dbgprintf("%s\n", m_name.to_string().characters());
}

void Parameter::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (!m_name.is_null()) {
        print_indent(indent);
        dbgprintf("%s\n", m_name.to_string().characters());
    }
    m_type->dump(indent + 1);
    // print_indent(indent);
    // dbgprintf("%s [%s]\n", m_name.is_null() ? "" : m_name.to_string().characters(), m_type->name().to_string().characters());
}

void FunctionDefinition::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("{\n");
    for (const auto& statement : m_statements) {
        statement.dump(indent + 1);
    }
    print_indent(indent);
    dbgprintf("}\n");
}

NonnullRefPtrVector<Declaration> FunctionDefinition::declarations() const
{
    NonnullRefPtrVector<Declaration> declarations;
    for (auto& statement : m_statements) {
        declarations.append(statement.declarations());
    }
    return declarations;
}

void VariableDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_type->dump(indent + 1);
    print_indent(indent + 1);
    dbgprintf("%s\n", m_name.to_string().characters());
    if (m_initial_value)
        m_initial_value->dump(indent + 1);
}

void Identifier::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("%s\n", m_name.to_string().characters());
}

void NumericLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("%s\n", m_value.to_string().characters());
}

void BinaryExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);

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

    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    ASSERT(op_string);
    dbgprintf("%s\n", op_string);
    m_rhs->dump(indent + 1);
}

void AssignmentExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);

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
    }

    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    ASSERT(op_string);
    dbgprintf("%s\n", op_string);
    m_rhs->dump(indent + 1);
}

void FunctionCall::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("%s\n", m_name.to_string().characters());
    for (const auto& arg : m_arguments) {
        arg.dump(indent + 1);
    }
}

void StringLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    dbgprintf("%s\n", m_value.to_string().characters());
}

void ReturnStatement::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_value->dump(indent + 1);
}

void EnumDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("%s\n", m_name.to_string().characters());
    for (auto& entry : m_entries) {
        print_indent(indent + 1);
        dbgprintf("%s\n", entry.to_string().characters());
    }
}

void StructOrClassDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    dbgprintf("%s\n", m_name.to_string().characters());
    for (auto& member : m_members) {
        member.dump(indent + 1);
    }
}

void MemberDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_type->dump(indent + 1);
    print_indent(indent + 1);
    dbgprintf("%s\n", m_name.to_string().characters());
    if (m_initial_value) {
        m_initial_value->dump(indent + 2);
    }
}

void UnaryExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);

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
    case UnaryOp::PlusPlus:
        op_string = "++";
        break;
    default:
        op_string = "<invalid>";
    }

    ASSERT(op_string);
    print_indent(indent + 1);
    dbgprintf("%s\n", op_string);
    m_lhs->dump(indent + 1);
}

void BooleanLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    dbgprintf("%s\n", m_value ? "true" : "false");
}

void Pointer::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (!m_pointee.is_null()) {
        m_pointee->dump(indent + 1);
    }
}

void MemberExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_object->dump(indent + 1);
    m_property->dump(indent + 1);
}

void BlockStatement::dump(size_t indent) const
{
    ASTNode::dump(indent);
    for (auto& statement : m_statements) {
        statement.dump(indent + 1);
    }
}

void ForStatement::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_init)
        m_init->dump(indent + 1);
    if (m_test)
        m_test->dump(indent + 1);
    if (m_update)
        m_update->dump(indent + 1);
    if (m_body)
        m_body->dump(indent + 1);
}

NonnullRefPtrVector<Declaration> Statement::declarations() const
{
    if (is_declaration()) {
        NonnullRefPtrVector<Declaration> vec;
        const auto& decl = static_cast<const Declaration&>(*this);
        vec.empend(const_cast<Declaration&>(decl));
        return vec;
    }
    return {};
}

NonnullRefPtrVector<Declaration> ForStatement::declarations() const
{
    auto declarations = m_init->declarations();
    declarations.append(m_body->declarations());
    return declarations;
}

NonnullRefPtrVector<Declaration> BlockStatement::declarations() const
{
    NonnullRefPtrVector<Declaration> declarations;
    for (auto& statement : m_statements) {
        declarations.append(statement.declarations());
    }
    return declarations;
}

}
