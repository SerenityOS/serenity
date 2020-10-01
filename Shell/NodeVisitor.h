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
    virtual void visit(const AST::CastToCommand*);
    virtual void visit(const AST::CastToList*);
    virtual void visit(const AST::CloseFdRedirection*);
    virtual void visit(const AST::CommandLiteral*);
    virtual void visit(const AST::Comment*);
    virtual void visit(const AST::DynamicEvaluate*);
    virtual void visit(const AST::DoubleQuotedString*);
    virtual void visit(const AST::Fd2FdRedirection*);
    virtual void visit(const AST::FunctionDeclaration*);
    virtual void visit(const AST::ForLoop*);
    virtual void visit(const AST::Glob*);
    virtual void visit(const AST::Execute*);
    virtual void visit(const AST::IfCond*);
    virtual void visit(const AST::Join*);
    virtual void visit(const AST::MatchExpr*);
    virtual void visit(const AST::Or*);
    virtual void visit(const AST::Pipe*);
    virtual void visit(const AST::ReadRedirection*);
    virtual void visit(const AST::ReadWriteRedirection*);
    virtual void visit(const AST::Sequence*);
    virtual void visit(const AST::Subshell*);
    virtual void visit(const AST::SimpleVariable*);
    virtual void visit(const AST::SpecialVariable*);
    virtual void visit(const AST::Juxtaposition*);
    virtual void visit(const AST::StringLiteral*);
    virtual void visit(const AST::StringPartCompose*);
    virtual void visit(const AST::SyntaxError*);
    virtual void visit(const AST::Tilde*);
    virtual void visit(const AST::VariableDeclarations*);
    virtual void visit(const AST::WriteAppendRedirection*);
    virtual void visit(const AST::WriteRedirection*);
};

}
