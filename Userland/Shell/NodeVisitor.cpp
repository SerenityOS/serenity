/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NodeVisitor.h"
#include "AST.h"

namespace Shell::AST {

void NodeVisitor::visit(const AST::PathRedirectionNode* node)
{
    node->path()->visit(*this);
}

void NodeVisitor::visit(const AST::And* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::ListConcatenate* node)
{
    for (auto& subnode : node->list())
        subnode->visit(*this);
}

void NodeVisitor::visit(const AST::Background* node)
{
    node->command()->visit(*this);
}

void NodeVisitor::visit(const AST::BarewordLiteral*)
{
}

void NodeVisitor::visit(const AST::BraceExpansion* node)
{
    for (auto& entry : node->entries())
        entry->visit(*this);
}

void NodeVisitor::visit(const AST::CastToCommand* node)
{
    node->inner()->visit(*this);
}

void NodeVisitor::visit(const AST::CastToList* node)
{
    if (node->inner())
        node->inner()->visit(*this);
}

void NodeVisitor::visit(const AST::CloseFdRedirection*)
{
}

void NodeVisitor::visit(const AST::CommandLiteral*)
{
}

void NodeVisitor::visit(const AST::Comment*)
{
}

void NodeVisitor::visit(const AST::ContinuationControl*)
{
}

void NodeVisitor::visit(const AST::DynamicEvaluate* node)
{
    node->inner()->visit(*this);
}

void NodeVisitor::visit(const AST::DoubleQuotedString* node)
{
    if (node->inner())
        node->inner()->visit(*this);
}

void NodeVisitor::visit(const AST::Fd2FdRedirection*)
{
}

void NodeVisitor::visit(const AST::FunctionDeclaration* node)
{
    if (node->block())
        node->block()->visit(*this);
}

void NodeVisitor::visit(const AST::ForLoop* node)
{
    if (node->iterated_expression())
        node->iterated_expression()->visit(*this);
    if (node->block())
        node->block()->visit(*this);
}

void NodeVisitor::visit(const AST::Glob*)
{
}

void NodeVisitor::visit(const AST::Heredoc* node)
{
    if (node->contents())
        node->contents()->visit(*this);
}

void NodeVisitor::visit(const AST::HistoryEvent*)
{
}

void NodeVisitor::visit(const AST::Execute* node)
{
    node->command()->visit(*this);
}

void NodeVisitor::visit(const AST::IfCond* node)
{
    node->condition()->visit(*this);
    if (node->true_branch())
        node->true_branch()->visit(*this);
    if (node->false_branch())
        node->false_branch()->visit(*this);
}

void NodeVisitor::visit(const AST::ImmediateExpression* node)
{
    for (auto& node : node->arguments())
        node->visit(*this);
}

void NodeVisitor::visit(const AST::Join* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::MatchExpr* node)
{
    node->matched_expr()->visit(*this);
    for (auto& entry : node->entries()) {
        if (auto* ptr = entry.options.get_pointer<Vector<NonnullRefPtr<Node>>>()) {
            for (auto& option : *ptr)
                option->visit(*this);
        }
        if (entry.body)
            entry.body->visit(*this);
    }
}

void NodeVisitor::visit(const AST::Or* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::Pipe* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::Range* node)
{
    node->start()->visit(*this);
    node->end()->visit(*this);
}

void NodeVisitor::visit(const AST::ReadRedirection* node)
{
    visit(static_cast<const AST::PathRedirectionNode*>(node));
}

void NodeVisitor::visit(const AST::ReadWriteRedirection* node)
{
    visit(static_cast<const AST::PathRedirectionNode*>(node));
}

void NodeVisitor::visit(const AST::Sequence* node)
{
    for (auto& entry : node->entries())
        entry->visit(*this);
}

void NodeVisitor::visit(const AST::Subshell* node)
{
    if (node->block())
        node->block()->visit(*this);
}

void NodeVisitor::visit(const AST::Slice* node)
{
    node->selector()->visit(*this);
}

void NodeVisitor::visit(const AST::SimpleVariable* node)
{
    if (const AST::Node* slice = node->slice())
        slice->visit(*this);
}

void NodeVisitor::visit(const AST::SpecialVariable* node)
{
    if (const AST::Node* slice = node->slice())
        slice->visit(*this);
}

void NodeVisitor::visit(const AST::Juxtaposition* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::StringLiteral*)
{
}

void NodeVisitor::visit(const AST::StringPartCompose* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::SyntaxError*)
{
}

void NodeVisitor::visit(const AST::SyntheticNode*)
{
}

void NodeVisitor::visit(const AST::Tilde*)
{
}

void NodeVisitor::visit(const AST::VariableDeclarations* node)
{
    for (auto& entry : node->variables()) {
        entry.name->visit(*this);
        entry.value->visit(*this);
    }
}

void NodeVisitor::visit(const AST::WriteAppendRedirection* node)
{
    visit(static_cast<const AST::PathRedirectionNode*>(node));
}

void NodeVisitor::visit(const AST::WriteRedirection* node)
{
    visit(static_cast<const AST::PathRedirectionNode*>(node));
}

}
