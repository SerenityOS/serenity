/*
 * Copyright (c) 2020, the SerenityOS developers.
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
    node->iterated_expression()->visit(*this);
    if (node->block())
        node->block()->visit(*this);
}

void NodeVisitor::visit(const AST::Glob*)
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

void NodeVisitor::visit(const AST::Join* node)
{
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::MatchExpr* node)
{
    node->matched_expr()->visit(*this);
    for (auto& entry : node->entries()) {
        for (auto& option : entry.options)
            option.visit(*this);
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
    node->left()->visit(*this);
    node->right()->visit(*this);
}

void NodeVisitor::visit(const AST::Subshell* node)
{
    if (node->block())
        node->block()->visit(*this);
}

void NodeVisitor::visit(const AST::SimpleVariable*)
{
}

void NodeVisitor::visit(const AST::SpecialVariable*)
{
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
