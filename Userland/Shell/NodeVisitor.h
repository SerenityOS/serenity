/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"

namespace Shell::AST {

class NodeVisitor {
public:
    virtual void visit(const AST::PathRedirectionNode*);
    virtual void visit(const AST::And*);
    virtual void visit(const AST::ListConcatenate*);
    virtual void visit(const AST::Background*);
    virtual void visit(const AST::BarewordLiteral*);
    virtual void visit(const AST::BraceExpansion*);
    virtual void visit(const AST::CastToCommand*);
    virtual void visit(const AST::CastToList*);
    virtual void visit(const AST::CloseFdRedirection*);
    virtual void visit(const AST::CommandLiteral*);
    virtual void visit(const AST::Comment*);
    virtual void visit(const AST::ContinuationControl*);
    virtual void visit(const AST::DynamicEvaluate*);
    virtual void visit(const AST::DoubleQuotedString*);
    virtual void visit(const AST::Fd2FdRedirection*);
    virtual void visit(const AST::FunctionDeclaration*);
    virtual void visit(const AST::ForLoop*);
    virtual void visit(const AST::Glob*);
    virtual void visit(const AST::Heredoc*);
    virtual void visit(const AST::HistoryEvent*);
    virtual void visit(const AST::Execute*);
    virtual void visit(const AST::IfCond*);
    virtual void visit(const AST::ImmediateExpression*);
    virtual void visit(const AST::Join*);
    virtual void visit(const AST::MatchExpr*);
    virtual void visit(const AST::Or*);
    virtual void visit(const AST::Pipe*);
    virtual void visit(const AST::Range*);
    virtual void visit(const AST::ReadRedirection*);
    virtual void visit(const AST::ReadWriteRedirection*);
    virtual void visit(const AST::Sequence*);
    virtual void visit(const AST::Subshell*);
    virtual void visit(const AST::Slice*);
    virtual void visit(const AST::SimpleVariable*);
    virtual void visit(const AST::SpecialVariable*);
    virtual void visit(const AST::Juxtaposition*);
    virtual void visit(const AST::StringLiteral*);
    virtual void visit(const AST::StringPartCompose*);
    virtual void visit(const AST::SyntaxError*);
    virtual void visit(const AST::SyntheticNode*);
    virtual void visit(const AST::Tilde*);
    virtual void visit(const AST::VariableDeclarations*);
    virtual void visit(const AST::WriteAppendRedirection*);
    virtual void visit(const AST::WriteRedirection*);

protected:
    virtual ~NodeVisitor() = default;
};

}
