/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/ScopedValueRollback.h>
#include <AK/TemporaryChange.h>
#include <LibGUI/TextEditor.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <Shell/NodeVisitor.h>
#include <Shell/Parser.h>
#include <Shell/SyntaxHighlighter.h>

namespace Shell {

enum class AugmentedTokenKind : u32 {
    __TokenTypeCount = (u32)AST::Node::Kind::__Count,
    OpenParen,
    CloseParen,
};

class HighlightVisitor : public AST::NodeVisitor {
public:
    HighlightVisitor(Vector<GUI::TextDocumentSpan>& spans, const Gfx::Palette& palette, const GUI::TextDocument& document)
        : m_spans(spans)
        , m_palette(palette)
        , m_document(document)
    {
    }

private:
    AST::Position::Line offset_line(const AST::Position::Line& line, size_t offset)
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
    void set_offset_range_end(GUI::TextRange& range, const AST::Position::Line& line, size_t offset = 0)
    {
        auto new_line = offset_line(line, offset);
        range.set_end({ new_line.line_number, new_line.line_column });
    }
    void set_offset_range_start(GUI::TextRange& range, const AST::Position::Line& line, size_t offset = 0)
    {
        auto new_line = offset_line(line, offset);
        range.set_start({ new_line.line_number, new_line.line_column });
    }

    GUI::TextDocumentSpan& span_for_node(const AST::Node* node)
    {
        GUI::TextDocumentSpan span;
        set_offset_range_start(span.range, node->position().start_line);
        set_offset_range_end(span.range, node->position().end_line);
        span.data = static_cast<u64>(node->kind());
        span.is_skippable = false;
        m_spans.append(move(span));

        return m_spans.last();
    }

    virtual void visit(const AST::PathRedirectionNode* node) override
    {
        if (node->path()->is_bareword()) {
            auto& span = span_for_node(node->path());
            span.attributes.color = m_palette.link();
            span.attributes.underline = true;
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
        set_offset_range_start(span.range, node->and_position().start_line);
        set_offset_range_end(span.range, node->and_position().end_line);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(const AST::ListConcatenate* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::Background* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        set_offset_range_start(span.range, node->position().end_line, 1);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(const AST::BraceExpansion* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::BarewordLiteral* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        if (m_is_first_in_command) {
            span.attributes.color = m_palette.syntax_keyword();
            span.attributes.bold = true;
            m_is_first_in_command = false;
        } else if (node->text().starts_with("-")) {
            span.attributes.color = m_palette.syntax_preprocessor_statement();
        } else {
            span.attributes.color = m_palette.base_text();
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
        start_span.attributes.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 2 });
        start_span.data = static_cast<u64>(AugmentedTokenKind::OpenParen);

        auto& end_span = span_for_node(node);
        end_span.attributes.color = m_palette.syntax_punctuation();
        set_offset_range_start(end_span.range, node->position().end_line, 1);
        end_span.data = static_cast<u64>(AugmentedTokenKind::CloseParen);
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
        span.attributes.color = m_palette.syntax_comment();
    }
    virtual void visit(const AST::ContinuationControl* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_control_keyword();
    }
    virtual void visit(const AST::DynamicEvaluate* node) override
    {
        NodeVisitor::visit(node);

        auto& start_span = span_for_node(node);
        start_span.attributes.color = m_palette.syntax_punctuation();
        start_span.range.set_end({ node->position().start_line.line_number, node->position().start_line.line_column + 1 });
    }
    virtual void visit(const AST::DoubleQuotedString* node) override
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
    virtual void visit(const AST::Fd2FdRedirection* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::FunctionDeclaration* node) override
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
    virtual void visit(const AST::ForLoop* node) override
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
    virtual void visit(const AST::Glob* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_preprocessor_value();
    }
    virtual void visit(const AST::Execute* node) override
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
    virtual void visit(const AST::IfCond* node) override
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

    virtual void visit(const AST::ImmediateExpression* node) override
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
        set_offset_range_start(span.range, node->or_position().start_line);
        set_offset_range_end(span.range, node->or_position().end_line);
        span.attributes.color = m_palette.syntax_punctuation();
        span.attributes.bold = true;
    }
    virtual void visit(const AST::Pipe* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::Range* node) override
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
        for (auto& entry : node->entries()) {
            ScopedValueRollback first_in_command { m_is_first_in_command };
            entry.visit(*this);
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
    virtual void visit(const AST::Subshell* node) override
    {
        NodeVisitor::visit(node);
    }
    virtual void visit(const AST::SimpleVariable* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_identifier();
    }
    virtual void visit(const AST::SpecialVariable* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.syntax_identifier();
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
        span.attributes.color = m_palette.syntax_string();
        if (m_is_first_in_command)
            span.attributes.bold = true;
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
        span.attributes.underline = true;
        span.attributes.background_color = Color(Color::NamedColor::MidRed).lightened(1.3f).with_alpha(128);
        span.attributes.color = m_palette.base_text();
    }
    virtual void visit(const AST::Tilde* node) override
    {
        NodeVisitor::visit(node);

        auto& span = span_for_node(node);
        span.attributes.color = m_palette.link();
    }
    virtual void visit(const AST::VariableDeclarations* node) override
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
    const GUI::TextDocument& m_document;
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

void SyntaxHighlighter::rehighlight(const Palette& palette)
{
    auto text = m_client->get_text();

    Parser parser(text);
    auto ast = parser.parse();

    Vector<GUI::TextDocumentSpan> spans;
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

SyntaxHighlighter::~SyntaxHighlighter()
{
}

}
