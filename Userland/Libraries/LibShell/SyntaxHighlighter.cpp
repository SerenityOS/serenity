/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/ScopedValueRollback.h>
#include <AK/TemporaryChange.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibShell/NodeVisitor.h>
#include <LibShell/Parser.h>
#include <LibShell/SyntaxHighlighter.h>
#include <LibSyntax/Document.h>

namespace Shell {

enum class AugmentedTokenKind : u32 {
    __TokenTypeCount = (u32)AST::Node::Kind::__Count,
    OpenParen,
    CloseParen,
};

class HighlightVisitor : public AST::NodeVisitor {
public:
    HighlightVisitor(Vector<Syntax::TextDocumentSpan>& spans, Gfx::Palette const& palette, Syntax::Document const& document)
        : m_spans(spans)
        , m_palette(palette)
        , m_document(document)
    {
    }

private:
    AST::Position::Line offset_line(AST::Position::Line const& line, size_t offset)
    {
        // We need to look at the line(s) above.
        AST::Position::Line new_line { line };
        while (new_line.line_column < offset) {
            offset -= new_line.line_column;
            --offset;

            if (new_line.line_number == 0)
                break;
            --new_line.line_number;

            auto& line = m_document.line(new_line.line_number);
            new_line.line_column = line.length();
        }
        if (offset > 0)
            new_line.line_column -= offset;

        return new_line;
    }
    void set_offset_range_end(Syntax::TextRange& range, AST::Position::Line const& line, size_t offset = 0)
    {
        auto new_line = offset_line(line, offset);
        range.set_end({ new_line.line_number, new_line.line_column });
    }
    void set_offset_range_start(Syntax::TextRange& range, AST::Position::Line const& line, size_t offset = 0)
    {
        auto new_line = offset_line(line, offset);
        range.set_start({ new_line.line_number, new_line.line_column });
    }

    Syntax::TextDocumentSpan& span_for_node(AST::Node const* node)
    {
        Syntax::TextDocumentSpan span;
        set_offset_range_start(span.range, node->position().start_line);
        set_offset_range_end(span.range, node->position().end_line);
        span.data = static_cast<u64>(node->kind());
        span.is_skippable = false;
        m_spans.append(move(span));

        return m_spans.last();
    }

    virtual void visit(AST::PathRedirectionNode const* node) override
    {
        if (node->path()->is_bareword()) {
            auto& span = span_for_node(node->path());
            span.attributes.color = m_palette.link();
            span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Solid;
        } else {
            NodeVisitor::visit(node);
        }
    }
    virtual void visit(AST::And const* node) override
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
        set_offset_range_start(span.range, node->and_position().start_line);
        set_offset_range_end(span.range, node->and_position().end_line);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(AST::ListConcatenate const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::Background const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        set_offset_range_start(span.range, node->position().end_line, 1);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(AST::BraceExpansion const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::BarewordLiteral const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        if (m_is_first_in_command) {
            span.attributes.color = m_palette.syntax_keyword();
            span.attributes.bold = true;
            m_is_first_in_command = false;
        } else if (node->text().starts_with('-')) {
            span.attributes.color = m_palette.syntax_preprocessor_statement();
        } else {
            span.attributes.color = m_palette.base_text();
        }
    }
    virtual void visit(AST::CastToCommand const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::CastToList const* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.attributes.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
        start_span.data = static_cast<u64>(AugmentedTokenKind::OpenParen);

        auto& end_span = span_for_node(node);
        end_span.attributes.color = m_palette.syntax_punctuation();
        set_offset_range_start(end_span.range, node->position().end_line, 1);
        end_span.data = static_cast<u64>(AugmentedTokenKind::CloseParen);
    }
    virtual void visit(AST::CloseFdRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::CommandLiteral const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::Comment const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_comment();
    }
    virtual void visit(AST::ContinuationControl const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_control_keyword();
    }
    virtual void visit(AST::DynamicEvaluate const* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.attributes.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
    }
    virtual void visit(AST::DoubleQuotedString const* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.attributes.color = m_palette.syntax_string();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
        start_span.is_skippable = true;

        auto& end_span = span_for_node(node);
        set_offset_range_start(end_span.range, node->position().end_line, 1);
        end_span.attributes.color = m_palette.syntax_string();
        end_span.is_skippable = true;

        if (m_is_first_in_command) {
            start_span.attributes.bold = true;
            end_span.attributes.bold = true;
        }
        m_is_first_in_command = false;
    }
    virtual void visit(AST::Fd2FdRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::FunctionDeclaration const* node) override
    {
        NodeVisitor::visit(node);

        // fn name
        auto& name_span = span_for_node(node);
        set_offset_range_start(name_span.range, node->name().position.start_line);
        set_offset_range_end(name_span.range, node->name().position.end_line);
        name_span.attributes.color = m_palette.syntax_identifier();

        // arguments
        for (auto& arg : node->arguments()) {
            auto& name_span = span_for_node(node);
            set_offset_range_start(name_span.range, arg.position.start_line);
            set_offset_range_end(name_span.range, arg.position.end_line);
            name_span.attributes.color = m_palette.syntax_identifier();
        }
    }
    virtual void visit(AST::ForLoop const* node) override
    {
        // The iterated expression is an expression, not a command.
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "for"
        auto& for_span = span_for_node(node);
        // FIXME: "fo\\\nr" is valid too
        for_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 3 });
        for_span.attributes.color = m_palette.syntax_keyword();

        // "in"
        if (auto maybe_position = node->in_keyword_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& in_span = span_for_node(node);
            set_offset_range_start(in_span.range, position.start_line);
            set_offset_range_end(in_span.range, position.end_line);
            in_span.attributes.color = m_palette.syntax_keyword();
        }

        // "index"
        if (auto maybe_position = node->index_keyword_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& index_span = span_for_node(node);
            set_offset_range_start(index_span.range, position.start_line);
            set_offset_range_end(index_span.range, position.end_line);
            index_span.attributes.color = m_palette.syntax_keyword();
        }

        // variables
        if (auto maybe_variable = node->variable(); maybe_variable.has_value()) {
            auto& position = maybe_variable->position;

            auto& variable_span = span_for_node(node);
            set_offset_range_start(variable_span.range, position.start_line);
            set_offset_range_end(variable_span.range, position.end_line);
            variable_span.attributes.color = m_palette.syntax_identifier();
        }

        if (auto maybe_variable = node->index_variable(); maybe_variable.has_value()) {
            auto& position = maybe_variable->position;

            auto& variable_span = span_for_node(node);
            set_offset_range_start(variable_span.range, position.start_line);
            set_offset_range_end(variable_span.range, position.end_line);
            variable_span.attributes.color = m_palette.syntax_identifier();
        }
    }
    virtual void visit(AST::Glob const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_preprocessor_value();
    }
    virtual void visit(AST::Execute const* node) override
    {
        TemporaryChange first { m_is_first_in_command, true };
        NodeVisitor::visit(node);

        if (node->does_capture_stdout()) {
            auto& start_span = span_for_node(node);
            start_span.attributes.color = m_palette.syntax_punctuation();
            start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
            start_span.data = static_cast<u64>(AugmentedTokenKind::OpenParen);

            auto& end_span = span_for_node(node);
            end_span.attributes.color = m_palette.syntax_punctuation();
            set_offset_range_start(end_span.range, node->position().end_line, 1);
            end_span.data = static_cast<u64>(AugmentedTokenKind::CloseParen);
        }
    }
    virtual void visit(AST::IfCond const* node) override
    {
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "if"
        auto& if_span = span_for_node(node);
        // FIXME: "i\\\nf" is valid too
        if_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
        if_span.attributes.color = m_palette.syntax_keyword();

        // "else"
        if (auto maybe_position = node->else_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& else_span = span_for_node(node);
            set_offset_range_start(else_span.range, position.start_line);
            set_offset_range_end(else_span.range, position.end_line);
            else_span.attributes.color = m_palette.syntax_keyword();
        }
    }

    virtual void visit(AST::ImmediateExpression const* node) override
    {
        TemporaryChange first { m_is_first_in_command, false };
        NodeVisitor::visit(node);

        // ${
        auto& start_span = span_for_node(node);
        start_span.attributes.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
        start_span.data = static_cast<u64>(AugmentedTokenKind::OpenParen);

        // Function name
        auto& name_span = span_for_node(node);
        name_span.attributes.color = m_palette.syntax_preprocessor_statement(); // Closest thing we have to this
        set_offset_range_start(name_span.range, node->function_position().start_line);
        set_offset_range_end(name_span.range, node->function_position().end_line);

        // }
        auto& end_span = span_for_node(node);
        end_span.attributes.color = m_palette.syntax_punctuation();
        set_offset_range_start(end_span.range, node->position().end_line, 1);
        end_span.data = static_cast<u64>(AugmentedTokenKind::CloseParen);
    }

    virtual void visit(AST::Join const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::MatchExpr const* node) override
    {
        // The matched expression is an expression, not a command.
        m_is_first_in_command = false;
        NodeVisitor::visit(node);

        // "match"
        auto& match_expr = span_for_node(node);
        // FIXME: "mat\\\nch" is valid too
        match_expr.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 5 });
        match_expr.attributes.color = m_palette.syntax_keyword();

        // "as"
        if (auto maybe_position = node->as_position(); maybe_position.has_value()) {
            auto& position = maybe_position.value();

            auto& as_span = span_for_node(node);
            as_span.range.set_start({ position.start_line.line_number, position.start_line.line_column });
            as_span.range.set_end({ position.end_line.line_number, position.end_line.line_column + 1 });
            as_span.attributes.color = m_palette.syntax_keyword();
        }
    }
    virtual void visit(AST::Or const* node) override
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
        set_offset_range_start(span.range, node->or_position().start_line);
        set_offset_range_end(span.range, node->or_position().end_line);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(AST::Pipe const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::Range const* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node->start());
        auto& start_position = node->start()->position();
        set_offset_range_start(start_span.range, start_position.start_line, 1);
        start_span.range.set_end({ start_position.start_line.line_number, start_position.start_line.line_column + 1 });
        start_span.attributes.color = m_palette.syntax_punctuation();

        auto& end_span = span_for_node(node->start());
        auto& end_position = node->end()->position();
        set_offset_range_start(end_span.range, end_position.end_line, 1);
        start_span.range.set_end({ end_position.start_line.line_number, end_position.start_line.line_column + 1 });

        end_span.attributes.color = m_palette.syntax_punctuation();
    }
    virtual void visit(AST::ReadRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::ReadWriteRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::Sequence const* node) override
    {
        for (auto& entry : node->entries()) {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            entry->visit(*this);
        }

        for (auto& position : node->separator_positions()) {
            if (position.start_offset == position.end_offset)
                continue;
            auto& span = span_for_node(node);
            set_offset_range_start(span.range, position.start_line);
            set_offset_range_end(span.range, position.end_line);
            span.attributes.color = m_palette.syntax_punctuation();
            span.attributes.bold = true;
            span.is_skippable = true;
        }
    }
    virtual void visit(AST::Subshell const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::SimpleVariable const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_identifier();
    }
    virtual void visit(AST::SpecialVariable const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_identifier();
    }
    virtual void visit(AST::Juxtaposition const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::StringLiteral const* node) override
    {
        NodeVisitor::visit(node);

        if (node->text().is_empty())
            return;

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_string();
        if (m_is_first_in_command)
            span.attributes.bold = true;
        m_is_first_in_command = false;
    }
    virtual void visit(AST::StringPartCompose const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::SyntaxError const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.underline_style = Gfx::TextAttributes::UnderlineStyle::Solid;
        span.attributes.background_color = Color(Color::NamedColor::MidRed).lightened(1.3f).with_alpha(128);
        span.attributes.color = m_palette.base_text();
    }
    virtual void visit(AST::Tilde const* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.link();
    }
    virtual void visit(AST::VariableDeclarations const* node) override
    {
        TemporaryChange first_in_command { m_is_first_in_command, false };
        for (auto& decl : node->variables()) {
            auto& name_span = span_for_node(decl.name);
            name_span.attributes.color = m_palette.syntax_identifier();

            decl.value->visit(*this);

            auto& start_span = span_for_node(decl.name);
            start_span.range.set_start({ decl.name->position().end_line.line_number, decl.name->position().end_line.line_column });
            start_span.range.set_end({ decl.value->position().start_line.line_number, decl.value->position().start_line.line_column + 1 });
            start_span.attributes.color = m_palette.syntax_punctuation();
            start_span.data = static_cast<u64>(AugmentedTokenKind::OpenParen);
        }
    }
    virtual void visit(AST::WriteAppendRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(AST::WriteRedirection const* node) override
    {
        NodeVisitor::visit(node);
    }

    Vector<Syntax::TextDocumentSpan>& m_spans;
    Gfx::Palette const& m_palette;
    Syntax::Document const& m_document;
    bool m_is_first_in_command { false };
};

bool SyntaxHighlighter::is_identifier(u64 token) const
{
    if (!token)
        return false;

    auto kind = (size_t)token;
    return kind == (size_t)AST::Node::Kind::BarewordLiteral
        || kind == (size_t)AST::Node::Kind::StringLiteral
        || kind == (size_t)AST::Node::Kind::Tilde;
}

bool SyntaxHighlighter::is_navigatable(u64) const
{
    return false;
}

void SyntaxHighlighter::rehighlight(Palette const& palette)
{
    auto text = m_client->get_text();

    Parser parser(text);
    auto ast = parser.parse();

    Vector<Syntax::TextDocumentSpan> spans;
    HighlightVisitor visitor { spans, palette, m_client->get_document() };

    if (ast)
        ast->visit(visitor);

    quick_sort(spans, [](auto& a, auto& b) { return a.range.start() < b.range.start() && a.range.end() < b.range.end(); });

    if constexpr (SYNTAX_HIGHLIGHTING_DEBUG) {
        for (auto& span : spans) {
            dbgln("Kind {}, range {}.", span.data, span.range);
        }
    }

    m_client->do_set_spans(move(spans));
    m_has_brace_buddies = false;
    highlight_matching_token_pair();
    m_client->do_update();
}

Vector<Syntax::Highlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({
            static_cast<u64>(AugmentedTokenKind::OpenParen),
            static_cast<u64>(AugmentedTokenKind::CloseParen),
        });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token0, u64 token1) const
{
    return token0 == token1;
}

}
