/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AST.h"
#include "NodeVisitor.h"
#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <ctype.h>

namespace Shell {

class Formatter final : public AST::NodeVisitor {
public:
    Formatter(StringView source, ssize_t cursor = -1, bool parse_as_posix = false)
        : m_builders({ StringBuilder { round_up_to_power_of_two(source.length(), 16) } })
        , m_source(source)
        , m_cursor(cursor)
        , m_parse_as_posix(parse_as_posix)
    {
        if (m_source.is_empty())
            return;

        size_t offset = 0;
        for (auto ptr = m_source.end() - 1; isspace(*ptr); --ptr) {
            ++offset;
            if (ptr == m_source.begin())
                break;
        }

        m_trivia = m_source.substring_view(m_source.length() - offset, offset);
    }

    explicit Formatter(const AST::Node& node)
        : m_builders({ StringBuilder {} })
        , m_cursor(-1)
        , m_root_node(node)
    {
    }

    ByteString format();
    size_t cursor() const { return m_output_cursor; }

private:
    virtual void visit(const AST::PathRedirectionNode*) override;
    virtual void visit(const AST::And*) override;
    virtual void visit(const AST::ListConcatenate*) override;
    virtual void visit(const AST::Background*) override;
    virtual void visit(const AST::BarewordLiteral*) override;
    virtual void visit(const AST::BraceExpansion*) override;
    virtual void visit(const AST::CastToCommand*) override;
    virtual void visit(const AST::CastToList*) override;
    virtual void visit(const AST::CloseFdRedirection*) override;
    virtual void visit(const AST::CommandLiteral*) override;
    virtual void visit(const AST::Comment*) override;
    virtual void visit(const AST::ContinuationControl*) override;
    virtual void visit(const AST::DynamicEvaluate*) override;
    virtual void visit(const AST::DoubleQuotedString*) override;
    virtual void visit(const AST::Fd2FdRedirection*) override;
    virtual void visit(const AST::FunctionDeclaration*) override;
    virtual void visit(const AST::ForLoop*) override;
    virtual void visit(const AST::Glob*) override;
    virtual void visit(const AST::Heredoc*) override;
    virtual void visit(const AST::HistoryEvent*) override;
    virtual void visit(const AST::Execute*) override;
    virtual void visit(const AST::IfCond*) override;
    virtual void visit(const AST::ImmediateExpression*) override;
    virtual void visit(const AST::Join*) override;
    virtual void visit(const AST::MatchExpr*) override;
    virtual void visit(const AST::Or*) override;
    virtual void visit(const AST::Pipe*) override;
    virtual void visit(const AST::Range*) override;
    virtual void visit(const AST::ReadRedirection*) override;
    virtual void visit(const AST::ReadWriteRedirection*) override;
    virtual void visit(const AST::Sequence*) override;
    virtual void visit(const AST::Subshell*) override;
    virtual void visit(const AST::Slice*) override;
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
    void visited(const AST::Node*);
    void will_visit(const AST::Node*);
    void insert_separator(bool escaped = false);
    void insert_indent();

    ALWAYS_INLINE void with_added_indent(int indent, Function<void()>);
    ALWAYS_INLINE void in_new_block(Function<void()>);
    ALWAYS_INLINE ByteString in_new_builder(Function<void()>, StringBuilder new_builder = StringBuilder {});

    StringBuilder& current_builder() { return m_builders.last(); }

    struct Options {
        size_t max_line_length_hint { 80 };
        bool explicit_parentheses { false };
        bool explicit_braces { false };
        bool in_double_quotes { false };
        bool in_heredoc { false };
    } m_options;

    size_t m_current_indent { 0 };

    Vector<StringBuilder> m_builders;

    StringView m_source;
    size_t m_output_cursor { 0 };
    ssize_t m_cursor { -1 };
    RefPtr<AST::Node const> m_root_node;
    AST::Node const* m_hit_node { nullptr };

    const AST::Node* m_parent_node { nullptr };
    const AST::Node* m_last_visited_node { nullptr };

    StringView m_trivia;
    Vector<ByteString> m_heredocs_to_append_after_sequence;

    bool m_parse_as_posix { false };
};

}
