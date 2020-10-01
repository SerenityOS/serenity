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

#include <LibGUI/ShellSyntaxHighlighter.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <Shell/NodeVisitor.h>
#include <Shell/Parser.h>

namespace GUI {

using namespace Shell;

enum class AugmentedTokenKind : u32 {
    __TokenTypeCount = (u32)AST::Node::Kind::__Count,
    OpenParen,
    CloseParen,
};

class HighlightVisitor : public AST::NodeVisitor {
public:
    HighlightVisitor(Vector<GUI::TextDocumentSpan>& spans, const Gfx::Palette& palette)
        : m_spans(spans)
        , m_palette(palette)
    {
    }

private:
    struct PositionAndLine {
        size_t position { 0 };
        size_t line { 0 };
        size_t offset { 0 };
    };

    static void set_offset_range_end(TextRange& range, const AST::Position::Line& line, size_t offset = 1)
    {
        range.set_end({ line.line_number, line.line_column - min(line.line_column, offset) });
    }
    static void set_offset_range_start(TextRange& range, const AST::Position::Line& line, size_t offset = 1)
    {
        range.set_start({ line.line_number, line.line_column - min(line.line_column, offset) });
    }

    GUI::TextDocumentSpan& span_for_node(const AST::Node* node)
    {
        GUI::TextDocumentSpan span;
        span.range.set_start({ node->position().start_line.line_number, node->position().start_line.line_column });
        set_offset_range_end(span.range, node->position().end_line);
        span.data = (void*)static_cast<size_t>(node->kind());
        span.is_skippable = false;
        m_spans.append(move(span));

        return m_spans.last();
    }

    virtual void visit(const AST::PathRedirectionNode* node) override
    {
        if (node->path()->is_bareword()) {
            auto& span = span_for_node(node->path());
            span.color = m_palette.link();
            span.is_underlined = true;
        } else {
            NodeVisitor::visit(node);
        }
    }
    virtual void visit(const AST::And* node) override
    {
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->left()->visit(*this);
        }
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->right()->visit(*this);
        }

        auto& span = span_for_node(node);
        span.range.set_start({ node->and_position().start_line.line_number, node->and_position().start_line.line_column });
        set_offset_range_end(span.range, node->and_position().end_line);
        span.color = m_palette.syntax_punctuation();
        span.font = &Gfx::Font::default_bold_fixed_width_font();
    }
    virtual void visit(const AST::ListConcatenate* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::Background* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        set_offset_range_start(span.range, node->position().end_line);
        span.color = m_palette.syntax_punctuation();
        span.font = &Gfx::Font::default_bold_fixed_width_font();
    }
    virtual void visit(const AST::BarewordLiteral* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        if (m_is_first_in_command) {
            span.color = m_palette.syntax_keyword();
            span.font = &Gfx::Font::default_bold_fixed_width_font();
            m_is_first_in_command = false;
        } else if (node->text().starts_with("-")) {
            span.color = m_palette.syntax_preprocessor_statement();
        }
    }
    virtual void visit(const AST::CastToCommand* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::CastToList* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
        start_span.data = (void*)static_cast<size_t>(AugmentedTokenKind::OpenParen);

        auto& end_span = span_for_node(node);
        end_span.color = m_palette.syntax_punctuation();
        set_offset_range_start(end_span.range, node->position().end_line);
        end_span.data = (void*)static_cast<size_t>(AugmentedTokenKind::CloseParen);
    }
    virtual void visit(const AST::CloseFdRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::CommandLiteral* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::Comment* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.color = m_palette.syntax_comment();
    }
    virtual void visit(const AST::DynamicEvaluate* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column });
    }
    virtual void visit(const AST::DoubleQuotedString* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.color = m_palette.syntax_string();
        set_offset_range_end(start_span.range, node->position().start_line, 0);
        start_span.is_skippable = true;

        auto& end_span = span_for_node(node);
        set_offset_range_start(end_span.range, node->position().end_line);
        end_span.color = m_palette.syntax_string();
        end_span.is_skippable = true;

        if (m_is_first_in_command) {
            start_span.font = &Gfx::Font::default_bold_fixed_width_font();
            end_span.font = &Gfx::Font::default_bold_fixed_width_font();
        }
        m_is_first_in_command = false;
    }
    virtual void visit(const AST::Fd2FdRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::FunctionDeclaration* node) override
    {
        NodeVisitor::visit(node);

        // fn name
        auto& name_span = span_for_node(node);
        name_span.range.set_start({ node->name().position.start_line.line_number, node->name().position.start_line.line_column });
        set_offset_range_end(name_span.range, node->name().position.end_line);
        name_span.color = m_palette.syntax_identifier();

        // arguments
        for (auto& arg : node->arguments()) {
            auto& name_span = span_for_node(node);
            name_span.range.set_start({ arg.position.start_line.line_number, arg.position.start_line.line_column });
            set_offset_range_end(name_span.range, arg.position.end_line);
            name_span.color = m_palette.syntax_identifier();
        }
    }
    virtual void visit(const AST::ForLoop* node) override
    {
        // The iterated expression is an expression, not a command.
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "for"
        auto& for_span = span_for_node(node);
        // FIXME: "fo\\\nr" is valid too
        for_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
        for_span.color = m_palette.syntax_keyword();

        // "in"
        if (auto maybe_position = node->in_keyword_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& in_span = span_for_node(node);
            in_span.range.set_start({ position.start_line.line_number, position.start_line.line_column });
            set_offset_range_end(in_span.range, position.end_line);
            in_span.color = m_palette.syntax_keyword();
        }
    }
    virtual void visit(const AST::Glob* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.color = m_palette.syntax_preprocessor_value();
    }
    virtual void visit(const AST::Execute* node) override
    {
        TemporaryChange first { m_is_first_in_command, true };
        NodeVisitor::visit(node);

        if (node->does_capture_stdout()) {
            auto& start_span = span_for_node(node);
            start_span.color = m_palette.syntax_punctuation();
            start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
            start_span.data = (void*)static_cast<size_t>(AugmentedTokenKind::OpenParen);

            auto& end_span = span_for_node(node);
            end_span.color = m_palette.syntax_punctuation();
            set_offset_range_start(end_span.range, node->position().end_line);
            end_span.data = (void*)static_cast<size_t>(AugmentedTokenKind::CloseParen);
        }
    }
    virtual void visit(const AST::IfCond* node) override
    {
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "if"
        auto& if_span = span_for_node(node);
        // FIXME: "i\\\nf" is valid too
        if_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
        if_span.color = m_palette.syntax_keyword();

        // "else"
        if (auto maybe_position = node->else_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& else_span = span_for_node(node);
            else_span.range.set_start({ position.start_line.line_number, position.start_line.line_column });
            set_offset_range_end(else_span.range, node->position().end_line);
            else_span.color = m_palette.syntax_keyword();
        }
    }
    virtual void visit(const AST::Join* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::MatchExpr* node) override
    {
        // The matched expression is an expression, not a command.
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "match"
        auto& match_expr = span_for_node(node);
        // FIXME: "mat\\\nch" is valid too
        match_expr.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 4 });
        match_expr.color = m_palette.syntax_keyword();

        // "as"
        if (auto maybe_position = node->as_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& as_span = span_for_node(node);
            as_span.range.set_start({ position.start_line.line_number, position.start_line.line_column });
            as_span.range.set_end({ position.end_line.line_number, position.end_line.line_column });
            as_span.color = m_palette.syntax_keyword();
        }
    }
    virtual void visit(const AST::Or* node) override
    {
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->left()->visit(*this);
        }
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->right()->visit(*this);
        }

        auto& span = span_for_node(node);
        span.range.set_start({ node->or_position().start_line.line_number, node->or_position().start_line.line_column });
        set_offset_range_end(span.range, node->or_position().end_line);
        span.color = m_palette.syntax_punctuation();
        span.font = &Gfx::Font::default_bold_fixed_width_font();
    }
    virtual void visit(const AST::Pipe* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::ReadRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::ReadWriteRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::Sequence* node) override
    {
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->left()->visit(*this);
        }
        {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            node->right()->visit(*this);
        }

        auto& span = span_for_node(node);
        span.range.set_start({ node->separator_position().start_line.line_number, node->separator_position().start_line.line_column });
        set_offset_range_end(span.range, node->separator_position().end_line);
        span.color = m_palette.syntax_punctuation();
        span.font = &Gfx::Font::default_bold_fixed_width_font();
        span.is_skippable = true;
    }
    virtual void visit(const AST::Subshell* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::SimpleVariable* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.color = m_palette.syntax_identifier();
    }
    virtual void visit(const AST::SpecialVariable* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.color = m_palette.syntax_identifier();
    }
    virtual void visit(const AST::Juxtaposition* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::StringLiteral* node) override
    {
        NodeVisitor::visit(node);

        if (node->text().is_empty())
            return;

        auto& span = span_for_node(node);
        span.color = m_palette.syntax_string();
        if (m_is_first_in_command)
            span.font = &Gfx::Font::default_bold_fixed_width_font();
        m_is_first_in_command = false;
    }
    virtual void visit(const AST::StringPartCompose* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::SyntaxError* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.is_underlined = true;
        span.background_color = Color(Color::NamedColor::MidRed).lightened(1.3f).with_alpha(128);
    }
    virtual void visit(const AST::Tilde* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.color = m_palette.link();
    }
    virtual void visit(const AST::VariableDeclarations* node) override
    {
        TemporaryChange first_in_command { m_is_first_in_command, false };
        for (auto& decl : node->variables()) {
            auto& name_span = span_for_node(decl.name);
            name_span.color = m_palette.syntax_identifier();

            decl.name->visit(*this);
            decl.value->visit(*this);

            auto& start_span = span_for_node(decl.name);
            start_span.range.set_start({ decl.name->position().end_line.line_number, decl.name->position().end_line.line_column });
            start_span.range.set_end({ decl.value->position().start_line.line_number, decl.value->position().start_line.line_column });
            start_span.color = m_palette.syntax_punctuation();
            start_span.data = (void*)static_cast<size_t>(AugmentedTokenKind::OpenParen);
        }
    }
    virtual void visit(const AST::WriteAppendRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::WriteRedirection* node) override
    {
        NodeVisitor::visit(node);
    }

    Vector<GUI::TextDocumentSpan>& m_spans;
    const Gfx::Palette& m_palette;
    bool m_is_first_in_command { false };
};

bool ShellSyntaxHighlighter::is_identifier(void* token) const
{
    if (!token)
        return false;

    auto kind = (size_t)token;
    return kind == (size_t)AST::Node::Kind::BarewordLiteral
        || kind == (size_t)AST::Node::Kind::StringLiteral
        || kind == (size_t)AST::Node::Kind::Tilde;
}

bool ShellSyntaxHighlighter::is_navigatable(void* token) const
{
    if (!token)
        return false;

    auto kind = static_cast<AugmentedTokenKind>((size_t)token);
    return (size_t)kind == (size_t)AST::Node::Kind::BarewordLiteral;
}

void ShellSyntaxHighlighter::rehighlight(Gfx::Palette palette)
{
    ASSERT(m_editor);
    auto text = m_editor->text();

    Parser parser(text);
    auto ast = parser.parse();

    Vector<GUI::TextDocumentSpan> spans;
    GUI::TextPosition position { 0, 0 };
    HighlightVisitor visitor { spans, palette };

    if (ast)
        ast->visit(visitor);

    quick_sort(spans, [](auto& a, auto& b) { return a.range.start() < b.range.start(); });

    m_editor->document().set_spans(spans);
    m_has_brace_buddies = false;
    highlight_matching_token_pair();
    m_editor->update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> ShellSyntaxHighlighter::matching_token_pairs() const
{
    static Vector<SyntaxHighlighter::MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({
            (void*)static_cast<size_t>(AugmentedTokenKind::OpenParen),
            (void*)static_cast<size_t>(AugmentedTokenKind::CloseParen),
        });
    }
    return pairs;
}

bool ShellSyntaxHighlighter::token_types_equal(void* token0, void* token1) const
{
    return token0 == token1;
}

ShellSyntaxHighlighter::~ShellSyntaxHighlighter()
{
}

}
