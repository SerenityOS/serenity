/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"

namespace GLSL {

static ErrorOr<void> print_indent(AK::Stream& output, int indent)
{
    for (int i = 0; i < indent * 2; ++i)
        TRY(output.write_some(" "sv.bytes()));
    return {};
}

ErrorOr<void> ASTNode::dump(AK::Stream& output, size_t indent) const
{
    TRY(print_indent(output, indent));
    TRY(output.write_formatted("{}[{}:{}->{}:{}]\n", class_name(), start().line, start().column, end().line, end().column));
    return {};
}

ErrorOr<void> TranslationUnit::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    for (auto const& child : m_declarations) {
        TRY(child->dump(output, indent + 1));
    }
    return {};
}

ErrorOr<void> FunctionDeclaration::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));

    TRY(m_return_type->dump(output, indent + 1));

    if (!m_name.is_null()) {
        TRY(print_indent(output, indent + 1));
        TRY(output.write_formatted("{}\n", m_name->name()));
    }

    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("(\n"));

    for (auto const& arg : m_parameters) {
        TRY(arg->dump(output, indent + 1));
    }

    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted(")\n"));

    if (!m_definition.is_null()) {
        TRY(m_definition->dump(output, indent + 1));
    }

    return {};
}

Vector<NonnullRefPtr<Declaration const>> FunctionDeclaration::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    for (auto& arg : m_parameters) {
        declarations.append(arg);
    }

    if (m_definition)
        declarations.extend(m_definition->declarations());

    return declarations;
}

ErrorOr<void> Type::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));

    StringBuilder qualifiers_string;
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Const).has_value())
        qualifiers_string.append("const "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::In).has_value())
        qualifiers_string.append("in "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Out).has_value())
        qualifiers_string.append("out "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Inout).has_value())
        qualifiers_string.append("inout "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Centroid).has_value())
        qualifiers_string.append("centroid "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Patch).has_value())
        qualifiers_string.append("patch "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Sample).has_value())
        qualifiers_string.append("sample "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Uniform).has_value())
        qualifiers_string.append("uniform "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Buffer).has_value())
        qualifiers_string.append("buffer "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Shared).has_value())
        qualifiers_string.append("shared "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Coherent).has_value())
        qualifiers_string.append("coherent "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Volatile).has_value())
        qualifiers_string.append("volatile "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Restrict).has_value())
        qualifiers_string.append("restrict "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Readonly).has_value())
        qualifiers_string.append("readonly "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Writeonly).has_value())
        qualifiers_string.append("writeonly "sv);
    if (m_storage_qualifiers.find_first_index(StorageTypeQualifier::Subroutine).has_value())
        qualifiers_string.append("subroutine "sv);
    TRY(output.write_formatted("{}{}\n", qualifiers_string.string_view(), m_name->name()));
    return {};
}

ErrorOr<void> Parameter::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    if (!m_name.is_null()) {
        TRY(print_indent(output, indent));
        TRY(output.write_formatted("{}\n", m_name->name()));
    }
    if (m_type)
        TRY(m_type->dump(output, indent + 1));
    return {};
}

ErrorOr<void> FunctionDefinition::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent));
    TRY(output.write_formatted("{{\n"));
    for (auto const& statement : m_statements) {
        TRY(statement->dump(output, indent + 1));
    }
    TRY(print_indent(output, indent));
    TRY(output.write_formatted("}}\n"));
    return {};
}

Vector<NonnullRefPtr<Declaration const>> FunctionDefinition::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    for (auto& statement : m_statements) {
        declarations.extend(statement->declarations());
    }
    return declarations;
}

ErrorOr<void> VariableDeclaration::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    if (m_type)
        TRY(m_type->dump(output, indent + 1));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", m_name->name()));
    if (m_initial_value)
        TRY(m_initial_value->dump(output, indent + 1));
    return {};
}

ErrorOr<void> NumericLiteral::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", m_value));
    return {};
}

ErrorOr<void> BinaryExpression::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));

    char const* op_string = nullptr;
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
    case BinaryOp::LogicalXor:
        op_string = "^^";
        break;
    case BinaryOp::LogicalAnd:
        op_string = "&&";
        break;
    case BinaryOp::Assignment:
        op_string = "=";
        break;
    case BinaryOp::AdditionAssignment:
        op_string = "+=";
        break;
    case BinaryOp::SubtractionAssignment:
        op_string = "-=";
        break;
    case BinaryOp::MultiplicationAssignment:
        op_string = "*=";
        break;
    case BinaryOp::DivisionAssignment:
        op_string = "/=";
        break;
    case BinaryOp::ModuloAssignment:
        op_string = "%=";
        break;
    case BinaryOp::AndAssignment:
        op_string = "&=";
        break;
    case BinaryOp::OrAssignment:
        op_string = "|=";
        break;
    case BinaryOp::XorAssignment:
        op_string = "^=";
        break;
    case BinaryOp::LeftShiftAssignment:
        op_string = "<<=";
        break;
    case BinaryOp::RightShiftAssignment:
        op_string = ">>=";
        break;
    }

    TRY(m_lhs->dump(output, indent + 1));

    TRY(print_indent(output, indent + 1));
    VERIFY(op_string);
    TRY(output.write_formatted("{}\n", op_string));

    TRY(m_rhs->dump(output, indent + 1));

    return {};
}

ErrorOr<void> FunctionCall::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(m_callee->dump(output, indent + 1));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("(\n"));
    for (auto const& arg : m_arguments) {
        TRY(arg->dump(output, indent + 1));
    }
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted(")\n"));
    return {};
}

ErrorOr<void> StringLiteral::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", m_value));
    return {};
}

ErrorOr<void> ReturnStatement::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    if (m_value)
        TRY(m_value->dump(output, indent + 1));
    return {};
}

ErrorOr<void> StructDeclaration::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", m_name->name()));
    for (auto& member : m_members) {
        TRY(member->dump(output, indent + 1));
    }
    return {};
}
Vector<NonnullRefPtr<Declaration const>> StructDeclaration::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    for (auto& member : m_members)
        declarations.append(member);
    return declarations;
}

ErrorOr<void> UnaryExpression::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));

    char const* op_string = nullptr;
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
    case UnaryOp::MinusMinus:
        op_string = "--";
        break;
    default:
        op_string = "<invalid>";
    }

    VERIFY(op_string);
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{} {}\n", m_is_postfix ? "postfix"sv : "prefix"sv, op_string));
    TRY(m_lhs->dump(output, indent + 1));
    return {};
}

ErrorOr<void> BooleanLiteral::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", m_value ? "true" : "false"));
    return {};
}

ErrorOr<void> MemberExpression::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(m_object->dump(output, indent + 1));
    TRY(m_property->dump(output, indent + 1));
    return {};
}

ErrorOr<void> ArrayElementExpression::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(m_array->dump(output, indent + 1));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("[\n"));
    TRY(m_index->dump(output, indent + 1));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("]\n"));
    return {};
}

ErrorOr<void> BlockStatement::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    for (auto& statement : m_statements) {
        TRY(statement->dump(output, indent + 1));
    }
    return {};
}

ErrorOr<void> ForStatement::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    if (m_init) {
        TRY(print_indent(output, indent));
        TRY(output.write_formatted("Initializer:\n"));
        TRY(m_init->dump(output, indent + 1));
    }
    if (m_test) {
        TRY(print_indent(output, indent));
        TRY(output.write_formatted("Test expression:\n"));
        TRY(m_test->dump(output, indent + 1));
    }
    if (m_update) {
        TRY(print_indent(output, indent));
        TRY(output.write_formatted("Update expression:\n"));
        TRY(m_update->dump(output, indent + 1));
    }
    if (m_body) {
        TRY(print_indent(output, indent));
        TRY(output.write_formatted("Body:\n"));
        TRY(m_body->dump(output, indent + 1));
    }
    return {};
}

Vector<NonnullRefPtr<Declaration const>> Statement::declarations() const
{
    if (is_declaration()) {
        Vector<NonnullRefPtr<Declaration const>> vec;
        auto const& decl = static_cast<Declaration const&>(*this);
        vec.empend(const_cast<Declaration&>(decl));
        return vec;
    }
    return {};
}

Vector<NonnullRefPtr<Declaration const>> ForStatement::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    if (m_init)
        declarations.extend(m_init->declarations());
    if (m_body)
        declarations.extend(m_body->declarations());
    return declarations;
}

Vector<NonnullRefPtr<Declaration const>> BlockStatement::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    for (auto& statement : m_statements) {
        declarations.extend(statement->declarations());
    }
    return declarations;
}

ErrorOr<void> IfStatement::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    if (m_predicate) {
        TRY(print_indent(output, indent + 1));
        TRY(output.write_formatted("Predicate:\n"));
        TRY(m_predicate->dump(output, indent + 1));
    }
    if (m_then) {
        TRY(print_indent(output, indent + 1));
        TRY(output.write_formatted("Then:\n"));
        TRY(m_then->dump(output, indent + 1));
    }
    if (m_else) {
        TRY(print_indent(output, indent + 1));
        TRY(output.write_formatted("Else:\n"));
        TRY(m_else->dump(output, indent + 1));
    }
    return {};
}

Vector<NonnullRefPtr<Declaration const>> IfStatement::declarations() const
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    if (m_predicate)
        declarations.extend(m_predicate->declarations());
    if (m_then)
        declarations.extend(m_then->declarations());
    if (m_else)
        declarations.extend(m_else->declarations());
    return declarations;
}

ErrorOr<void> Name::dump(AK::Stream& output, size_t indent) const
{
    TRY(ASTNode::dump(output, indent));
    TRY(print_indent(output, indent + 1));
    TRY(output.write_formatted("{}\n", name()));
    return {};
}

ErrorOr<void> SizedName::dump(AK::Stream& output, size_t indent) const
{
    TRY(Name::dump(output, indent));
    TRY(print_indent(output, indent + 1));

    StringBuilder dimension_info;
    for (auto const& dim : m_dimensions) {
        dimension_info.append('[');
        dimension_info.append(dim);
        dimension_info.append(']');
    }

    if (dimension_info.is_empty()) {
        dimension_info.append("[]"sv);
    }
    TRY(output.write_formatted("{}\n", dimension_info.string_view()));
    return {};
}

}
