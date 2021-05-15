/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"

namespace Cpp {

static void print_indent(int indent)
{
    for (int i = 0; i < indent * 2; ++i)
        out(" ");
}

void ASTNode::dump(size_t indent) const
{
    print_indent(indent);
    outln("{}[{}:{}->{}:{}]", class_name(), start().line, start().column, end().line, end().column);
}

void TranslationUnit::dump(size_t indent) const
{
    ASTNode::dump(indent);
    for (const auto& child : m_declarations) {
        child.dump(indent + 1);
    }
}

void FunctionDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);

    String qualifiers_string;
    if (!m_qualifiers.is_empty()) {
        print_indent(indent + 1);
        outln("[{}]", String::join(" ", m_qualifiers));
    }

    m_return_type->dump(indent + 1);
    if (!m_name.is_null()) {
        print_indent(indent + 1);
        outln("{}", m_name);
    }
    print_indent(indent + 1);
    outln("(");
    for (const auto& arg : m_parameters) {
        arg.dump(indent + 1);
    }
    print_indent(indent + 1);
    outln(")");
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
    outln("{}", to_string());
}

String Type::to_string() const
{
    String qualifiers_string;
    if (!m_qualifiers.is_empty())
        qualifiers_string = String::formatted("[{}] ", String::join(" ", m_qualifiers));
    return String::formatted("{}{}", qualifiers_string, m_name.is_null() ? "" : m_name->full_name());
}

String Pointer::to_string() const
{
    if (!m_pointee)
        return {};
    StringBuilder builder;
    builder.append(m_pointee->to_string());
    builder.append("*");
    return builder.to_string();
}

void Parameter::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_is_ellipsis) {
        print_indent(indent + 1);
        outln("...");
    }
    if (!m_name.is_null()) {
        print_indent(indent);
        outln("{}", m_name);
    }
    if (m_type)
        m_type->dump(indent + 1);
}

void FunctionDefinition::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{{");
    for (const auto& statement : m_statements) {
        statement.dump(indent + 1);
    }
    print_indent(indent);
    outln("}}");
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
    if (m_type)
        m_type->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", m_name);
    if (m_initial_value)
        m_initial_value->dump(indent + 1);
}

void Identifier::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{}", m_name);
}

void NumericLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{}", m_value);
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
    case BinaryOp::EqualsEquals:
        op_string = "==";
        break;
    case BinaryOp::NotEqual:
        op_string = "!=";
        break;
    case BinaryOp::LogicalOr:
        op_string = "||";
        break;
    case BinaryOp::LogicalAnd:
        op_string = "&&";
        break;
    case BinaryOp::Arrow:
        op_string = "->";
        break;
    }

    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    VERIFY(op_string);
    outln("{}", op_string);
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
    VERIFY(op_string);
    outln("{}", op_string);
    m_rhs->dump(indent + 1);
}

void FunctionCall::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_callee->dump(indent + 1);
    for (const auto& arg : m_arguments) {
        arg.dump(indent + 1);
    }
}

void StringLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", m_value);
}

void ReturnStatement::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_value)
        m_value->dump(indent + 1);
}

void EnumDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{}", m_name);
    for (auto& entry : m_entries) {
        print_indent(indent + 1);
        outln("{}", entry);
    }
}

void StructOrClassDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{}", m_name);
    for (auto& member : m_members) {
        member.dump(indent + 1);
    }
}
NonnullRefPtrVector<Declaration> StructOrClassDeclaration::declarations() const
{
    NonnullRefPtrVector<Declaration> declarations;
    for (auto& member : m_members)
        declarations.append(member);
    return declarations;
}

void MemberDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    m_type->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", m_name);
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
    case UnaryOp::Address:
        op_string = "&";
        break;
    default:
        op_string = "<invalid>";
    }

    VERIFY(op_string);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_lhs->dump(indent + 1);
}

void BooleanLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", m_value ? "true" : "false");
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
    NonnullRefPtrVector<Declaration> declarations;
    if (m_init)
        declarations.append(m_init->declarations());
    if (m_body)
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

void IfStatement::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_predicate) {
        print_indent(indent + 1);
        outln("Predicate:");
        m_predicate->dump(indent + 1);
    }
    if (m_then) {
        print_indent(indent + 1);
        outln("Then:");
        m_then->dump(indent + 1);
    }
    if (m_else) {
        print_indent(indent + 1);
        outln("Else:");
        m_else->dump(indent + 1);
    }
}

NonnullRefPtrVector<Declaration> IfStatement::declarations() const
{
    NonnullRefPtrVector<Declaration> declarations;
    if (m_predicate)
        declarations.append(m_predicate->declarations());
    if (m_then)
        declarations.append(m_then->declarations());
    if (m_else)
        declarations.append(m_else->declarations());
    return declarations;
}

void NamespaceDeclaration::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", m_name);
    for (auto& decl : m_declarations)
        decl.dump(indent + 1);
}

void NullPointerLiteral::dump(size_t indent) const
{
    ASTNode::dump(indent);
}

void Name::dump(size_t indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("{}", full_name());
}

String Name::full_name() const
{
    StringBuilder builder;
    if (!m_scope.is_empty()) {
        for (auto& scope : m_scope) {
            builder.appendff("{}::", scope.m_name);
        }
    }
    return String::formatted("{}{}", builder.to_string(), m_name.is_null() ? "" : m_name->m_name);
}

String TemplatizedName::full_name() const
{
    StringBuilder name;
    name.append(Name::full_name());
    name.append('<');
    for (auto& type : m_template_arguments) {
        name.append(type.to_string());
    }
    name.append('>');
    return name.to_string();
}

void CppCastExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("{}", m_cast_type);

    print_indent(indent + 1);
    outln("<");
    if (m_type)
        m_type->dump(indent + 1);
    print_indent(indent + 1);
    outln(">");

    if (m_expression)
        m_expression->dump(indent + 1);
}

void SizeofExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_type)
        m_type->dump(indent + 1);
}

void BracedInitList::dump(size_t indent) const
{
    ASTNode::dump(indent);
    for (auto& exp : m_expressions) {
        exp.dump(indent + 1);
    }
}

void CStyleCastExpression::dump(size_t indent) const
{
    ASTNode::dump(indent);
    if (m_type)
        m_type->dump(indent + 1);
    if (m_expression)
        m_expression->dump(indent + 1);
}

}
