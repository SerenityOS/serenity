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

#include "NodeVisitor.h"
#include <AK/Forward.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <ctype.h>

namespace Shell {

class Formatter final : public AST::NodeVisitor {
public:
    Formatter(const StringView& source, ssize_t cursor = -1)
        : m_builder(round_up_to_power_of_two(source.length(), 16))
        , m_source(source)
        , m_cursor(cursor)
    {
        size_t offset = 0;
        for (auto ptr = m_source.end() - 1; ptr >= m_source.begin() && isspace(*ptr); --ptr)
            ++offset;

        m_trivia = m_source.substring_view(m_source.length() - offset, offset);
    }

    String format();
    size_t cursor() const { return m_output_cursor; }

private:
    virtual void visit(const AST::PathRedirectionNode*) override;
    virtual void visit(const AST::And*) override;
    virtual void visit(const AST::ListConcatenate*) override;
    virtual void visit(const AST::Background*) override;
    virtual void visit(const AST::BarewordLiteral*) override;
    virtual void visit(const AST::CastToCommand*) override;
    virtual void visit(const AST::CastToList*) override;
    virtual void visit(const AST::CloseFdRedirection*) override;
    virtual void visit(const AST::CommandLiteral*) override;
    virtual void visit(const AST::Comment*) override;
    virtual void visit(const AST::DynamicEvaluate*) override;
    virtual void visit(const AST::DoubleQuotedString*) override;
    virtual void visit(const AST::Fd2FdRedirection*) override;
    virtual void visit(const AST::FunctionDeclaration*) override;
    virtual void visit(const AST::ForLoop*) override;
    virtual void visit(const AST::Glob*) override;
    virtual void visit(const AST::Execute*) override;
    virtual void visit(const AST::IfCond*) override;
    virtual void visit(const AST::Join*) override;
    virtual void visit(const AST::MatchExpr*) override;
    virtual void visit(const AST::Or*) override;
    virtual void visit(const AST::Pipe*) override;
    virtual void visit(const AST::ReadRedirection*) override;
    virtual void visit(const AST::ReadWriteRedirection*) override;
    virtual void visit(const AST::Sequence*) override;
    virtual void visit(const AST::Subshell*) override;
    virtual void visit(const AST::SimpleVariable*) override;
    virtual void visit(const AST::SpecialVariable*) override;
    virtual void visit(const AST::Juxtaposition*) override;
    virtual void visit(const AST::StringLiteral*) override;
    virtual void visit(const AST::StringPartCompose*) override;
    virtual void visit(const AST::SyntaxError*) override;
    virtual void visit(const AST::Tilde*) override;
    virtual void visit(const AST::VariableDeclarations*) override;
    virtual void visit(const AST::WriteAppendRedirection*) override;
    virtual void visit(const AST::WriteRedirection*) override;

    void test_and_update_output_cursor(const AST::Node*);
    void insert_separator();
    void insert_indent();

    ALWAYS_INLINE void with_added_indent(int indent, Function<void()>);
    ALWAYS_INLINE void in_new_block(Function<void()>);

    StringBuilder& current_builder() { return m_builder; }

    struct Options {
        size_t max_line_length_hint { 80 };
        bool explicit_parentheses { false };
        bool explicit_braces { false };
        bool in_double_quotes { false };
    } m_options;

    size_t m_current_line_length { 0 };
    size_t m_current_indent { 0 };

    StringBuilder m_builder;

    StringView m_source;
    size_t m_output_cursor { 0 };
    ssize_t m_cursor { -1 };
    AST::Node* m_hit_node { nullptr };

    const AST::Node* m_parent_node { nullptr };

    StringView m_trivia;
};

}
