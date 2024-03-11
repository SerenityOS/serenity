/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>

#include "AST/AST.h"
#include "Compiler/ControlFlowGraph.h"
#include "Function.h"

namespace JSSpecCompiler {

void Node::format_tree(StringBuilder& builder)
{
    static int current_depth = -1;
    TemporaryChange<int> depth_change(current_depth, current_depth + 1);
    builder.append_repeated(' ', current_depth * 2);
    dump_tree(builder);
}

template<typename... Parameters>
void Node::dump_node(StringBuilder& builder, AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
    MUST(AK::vformat(builder, fmtstr.view(), variadic_format_params));
    builder.append("\n"sv);
}

void ErrorNode::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "Error \"{}\"", m_error);
}

void WellKnownNode::dump_tree(StringBuilder& builder)
{
    static constexpr StringView type_to_name[] = {
        "False"sv,
        "NewTarget"sv,
        "Null"sv,
        "This"sv,
        "True"sv,
        "Undefined"sv,
    };
    dump_node(builder, "WellKnownNode {}", type_to_name[m_type]);
}

void ControlFlowFunctionReturn::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "ControlFlowFunctionReturn");
    m_return_value->format_tree(builder);
}

void ControlFlowJump::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "ControlFlowJump jump={}", m_block->m_index);
}

void ControlFlowBranch::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "ControlFlowBranch true={} false={}", m_then->m_index, m_else->m_index);
    m_condition->format_tree(builder);
}

void MathematicalConstant::dump_tree(StringBuilder& builder)
{
    String representation;
    if (Crypto::UnsignedBigInteger { 1000 }.divided_by(m_number.denominator()).remainder == 0)
        representation = MUST(String::from_byte_string(m_number.to_byte_string(3)));
    else
        representation = MUST(String::formatted("{}/{}", MUST(m_number.numerator().to_base(10)), MUST(m_number.denominator().to_base(10))));
    dump_node(builder, "MathematicalConstant {}", representation);
}

void StringLiteral::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "StringLiteral {}", m_literal);
}

void BinaryOperation::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "BinaryOperation {}", binary_operator_names[to_underlying(m_operation)]);
    m_left->format_tree(builder);
    m_right->format_tree(builder);
}

void UnaryOperation::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "UnaryOperation {}", unary_operator_names[to_underlying(m_operation)]);
    m_operand->format_tree(builder);
}

void IsOneOfOperation::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "IsOneOf");
    m_operand->format_tree(builder);
    for (auto const& compare_value : m_compare_values)
        compare_value->format_tree(builder);
}

void UnresolvedReference::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "UnresolvedReference {}", m_name);
}

void ReturnNode::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "ReturnNode");
    m_return_value->format_tree(builder);
}

void AssertExpression::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "AssertExpression");
    m_condition->format_tree(builder);
}

void IfBranch::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "IfBranch");
    m_condition->format_tree(builder);
    m_branch->format_tree(builder);
}

void ElseIfBranch::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "ElseIfBranch {}", m_condition ? "ElseIf" : "Else");
    if (m_condition)
        m_condition->format_tree(builder);
    m_branch->format_tree(builder);
}

void IfElseIfChain::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "IfElseIfChain");

    for (size_t i = 0; i < branches_count(); ++i) {
        m_conditions[i]->format_tree(builder);
        m_branches[i]->format_tree(builder);
    }
    if (m_else_branch)
        m_else_branch->format_tree(builder);
}

void TreeList::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "TreeList");
    for (auto const& expression : m_trees)
        expression->format_tree(builder);
}

void RecordDirectListInitialization::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "RecordDirectListInitialization");
    m_type_reference->format_tree(builder);
    for (auto const& argument : m_arguments)
        builder.appendff("{}{}", argument.name, argument.value);
}

void FunctionCall::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "FunctionCall");
    m_name->format_tree(builder);
    for (auto const& argument : m_arguments)
        argument->format_tree(builder);
}

void SlotName::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "Slot {}", m_member_name);
}

void Variable::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "Var {}", name());
}

void Enumerator::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "Enumerator {}", m_value);
}

void FunctionPointer::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "Func \"{}\"", m_declaration->name());
}

void List::dump_tree(StringBuilder& builder)
{
    dump_node(builder, "List");
    for (auto const& element : m_elements)
        element->format_tree(builder);
}

}
