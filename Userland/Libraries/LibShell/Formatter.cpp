/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatter.h"
#include "AST.h"
#include "Parser.h"
#include "PosixParser.h"
#include <AK/Hex.h>
#include <AK/ScopedValueRollback.h>
#include <AK/TemporaryChange.h>

namespace Shell {

ByteString Formatter::format()
{
    auto node = m_root_node ?: (m_parse_as_posix ? Posix::Parser(m_source).parse() : Parser(m_source).parse());
    if (m_cursor >= 0)
        m_output_cursor = m_cursor;

    if (!node)
        return ByteString();

    if (node->is_syntax_error())
        return m_source;

    if (m_cursor >= 0) {
        auto hit_test = node->hit_test_position(m_cursor);
        if (hit_test.matching_node)
            m_hit_node = hit_test.matching_node.ptr();
        else
            m_hit_node = nullptr;
    }

    m_parent_node = nullptr;

    node->visit(*this);

    VERIFY(m_builders.size() == 1);

    auto string = current_builder().string_view();

    if (!string.ends_with(' '))
        current_builder().append(m_trivia);

    return current_builder().to_byte_string();
}

void Formatter::with_added_indent(int indent, Function<void()> callback)
{
    TemporaryChange indent_change { m_current_indent, m_current_indent + indent };
    callback();
}

void Formatter::in_new_block(Function<void()> callback)
{
    current_builder().append('{');

    with_added_indent(1, [&] {
        insert_separator();
        callback();
    });

    insert_separator();
    current_builder().append('}');
}

ByteString Formatter::in_new_builder(Function<void()> callback, StringBuilder new_builder)
{
    m_builders.append(move(new_builder));
    callback();
    return m_builders.take_last().to_byte_string();
}

void Formatter::test_and_update_output_cursor(const AST::Node* node)
{
    if (!node)
        return;

    if (node != m_hit_node)
        return;

    m_output_cursor = current_builder().length() + m_cursor - node->position().start_offset;
}

void Formatter::visited(const AST::Node* node)
{
    m_last_visited_node = node;
}

void Formatter::will_visit(const AST::Node* node)
{
    if (!m_last_visited_node)
        return;

    if (!node)
        return;

    auto direct_sequence_child = !m_parent_node || m_parent_node->kind() == AST::Node::Kind::Sequence;

    if (direct_sequence_child && node->kind() != AST::Node::Kind::Sequence && node->kind() != AST::Node::Kind::Execute) {
        // Collapse more than one empty line to a single one.
        if (node->position().start_line.line_number - m_last_visited_node->position().end_line.line_number > 1)
            insert_separator();
    }
}

void Formatter::insert_separator(bool escaped)
{
    if (escaped)
        current_builder().append('\\');
    current_builder().append('\n');
    if (!escaped && !m_heredocs_to_append_after_sequence.is_empty()) {
        for (auto& entry : m_heredocs_to_append_after_sequence) {
            current_builder().append(entry);
        }
        m_heredocs_to_append_after_sequence.clear();
    }
    insert_indent();
}

void Formatter::insert_indent()
{
    for (size_t i = 0; i < m_current_indent; ++i)
        current_builder().append("  "sv);
}

void Formatter::visit(const AST::PathRedirectionNode* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::And* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->kind() != AST::Node::Kind::And;
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    with_added_indent(should_indent ? 1 : 0, [&] {
        node->left()->visit(*this);

        current_builder().append(' ');
        insert_separator(true);
        current_builder().append("&& "sv);

        node->right()->visit(*this);
    });
    visited(node);
}

void Formatter::visit(const AST::ListConcatenate* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& subnode : node->list()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        subnode->visit(*this);
    }
    visited(node);
}

void Formatter::visit(const AST::Background* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    current_builder().append(" &"sv);
    visited(node);
}

void Formatter::visit(const AST::BarewordLiteral* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
    visited(node);
}

void Formatter::visit(const AST::BraceExpansion* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    if (!m_parent_node || m_parent_node->kind() != AST::Node::Kind::Slice)
        current_builder().append('{');

    {
        TemporaryChange<const AST::Node*> parent { m_parent_node, node };
        bool first = true;
        for (auto& entry : node->entries()) {
            if (!first)
                current_builder().append(',');
            first = false;
            entry->visit(*this);
        }
    }

    if (!m_parent_node || m_parent_node->kind() != AST::Node::Kind::Slice)
        current_builder().append('}');
    visited(node);
}

void Formatter::visit(const AST::CastToCommand* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);

    visited(node);
}

void Formatter::visit(const AST::CastToList* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append('(');

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);

    current_builder().append(')');
    visited(node);
}

void Formatter::visit(const AST::CloseFdRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().appendff("{}>&-", node->fd());
    visited(node);
}

void Formatter::visit(const AST::CommandLiteral*)
{
    VERIFY_NOT_REACHED();
}

void Formatter::visit(const AST::Comment* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append("#"sv);
    current_builder().append(node->text());
    visited(node);
}

void Formatter::visit(const AST::ContinuationControl* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    if (node->continuation_kind() == AST::ContinuationControl::Break)
        current_builder().append("break"sv);
    else if (node->continuation_kind() == AST::ContinuationControl::Continue)
        current_builder().append("continue"sv);
    else
        VERIFY_NOT_REACHED();
    visited(node);
}

void Formatter::visit(const AST::DynamicEvaluate* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append('$');
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::DoubleQuotedString* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto not_in_heredoc = m_parent_node->kind() != AST::Node::Kind::Heredoc;
    if (not_in_heredoc)
        current_builder().append("\""sv);

    TemporaryChange quotes { m_options.in_double_quotes, true };
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    NodeVisitor::visit(node);

    if (not_in_heredoc)
        current_builder().append("\""sv);
    visited(node);
}

void Formatter::visit(const AST::Fd2FdRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().appendff("{}>&{}", node->source_fd(), node->dest_fd());
    if (m_hit_node == node)
        ++m_output_cursor;
    visited(node);
}

void Formatter::visit(const AST::FunctionDeclaration* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append(node->name().name);
    current_builder().append('(');
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& arg : node->arguments()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        current_builder().append(arg.name);
    }

    current_builder().append(") "sv);

    in_new_block([&] {
        if (node->block())
            node->block()->visit(*this);
    });
    visited(node);
}

void Formatter::visit(const AST::ForLoop* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto is_loop = node->iterated_expression().is_null();
    current_builder().append(is_loop ? "loop"sv : "for "sv);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (!is_loop) {
        if (node->index_variable().has_value()) {
            current_builder().append("index "sv);
            current_builder().append(node->index_variable()->name);
            current_builder().append(" "sv);
        }
        if (node->variable().has_value() && node->variable()->name != "it") {
            current_builder().append(node->variable()->name);
            current_builder().append(" in "sv);
        }

        node->iterated_expression()->visit(*this);
    }

    current_builder().append(' ');
    in_new_block([&] {
        if (node->block())
            node->block()->visit(*this);
    });
    visited(node);
}

void Formatter::visit(const AST::Glob* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
    visited(node);
}

void Formatter::visit(const AST::Heredoc* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    current_builder().append("<<"sv);
    if (node->deindent())
        current_builder().append('~');
    else
        current_builder().append('-');

    if (node->allow_interpolation())
        current_builder().appendff("{}", node->end());
    else
        current_builder().appendff("'{}'", node->end());

    auto content = in_new_builder([&] {
        if (!node->contents())
            return;

        TemporaryChange<const AST::Node*> parent { m_parent_node, node };
        TemporaryChange heredoc { m_options.in_heredoc, true };

        auto& contents = *node->contents();
        contents.visit(*this);
        current_builder().appendff("\n{}\n", node->end());
    });

    m_heredocs_to_append_after_sequence.append(move(content));

    visited(node);
}

void Formatter::visit(const AST::HistoryEvent* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    current_builder().append('!');
    switch (node->selector().event.kind) {
    case AST::HistorySelector::EventKind::ContainingStringLookup:
        current_builder().append('?');
        current_builder().append(node->selector().event.text);
        break;
    case AST::HistorySelector::EventKind::StartingStringLookup:
        current_builder().append(node->selector().event.text);
        break;
    case AST::HistorySelector::EventKind::IndexFromStart:
        current_builder().append(node->selector().event.text);
        break;
    case AST::HistorySelector::EventKind::IndexFromEnd:
        if (node->selector().event.index == 0)
            current_builder().append('!');
        else
            current_builder().append(node->selector().event.text);
        break;
    }

    auto& range = node->selector().word_selector_range;
    if (!range.end.has_value()
        || range.end.value().kind != AST::HistorySelector::WordSelectorKind::Last
        || range.start.kind != AST::HistorySelector::WordSelectorKind::Index || range.start.selector != 0) {

        auto append_word = [this](auto& selector) {
            switch (selector.kind) {
            case AST::HistorySelector::WordSelectorKind::Index:
                if (selector.selector == 0)
                    current_builder().append('^');
                else
                    current_builder().appendff("{}", selector.selector);
                break;
            case AST::HistorySelector::WordSelectorKind::Last:
                current_builder().append('$');
                break;
            }
        };

        current_builder().append(':');
        append_word(range.start);

        if (range.end.has_value()) {
            current_builder().append('-');
            append_word(range.end.value());
        }
    }

    visited(node);
}

void Formatter::visit(const AST::Execute* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto& builder = current_builder();
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    ScopedValueRollback options_rollback { m_options };

    if (node->does_capture_stdout())
        builder.append("$("sv);

    NodeVisitor::visit(node);

    if (node->does_capture_stdout())
        builder.append(")"sv);

    visited(node);
}

void Formatter::visit(const AST::IfCond* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    current_builder().append("if "sv);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->condition()->visit(*this);

    current_builder().append(' ');

    in_new_block([&] {
        if (node->true_branch())
            node->true_branch()->visit(*this);
    });

    if (node->false_branch()) {
        current_builder().append(" else "sv);
        if (node->false_branch()->kind() != AST::Node::Kind::IfCond) {
            in_new_block([&] {
                node->false_branch()->visit(*this);
            });
        } else {
            node->false_branch()->visit(*this);
        }
    } else if (node->else_position().has_value()) {
        current_builder().append(" else "sv);
    }
    visited(node);
}

void Formatter::visit(const AST::ImmediateExpression* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    current_builder().append("${"sv);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().append(node->function_name());

    for (auto& node : node->arguments()) {
        current_builder().append(' ');
        node->visit(*this);
    }

    if (node->has_closing_brace())
        current_builder().append('}');

    visited(node);
}

void Formatter::visit(const AST::Join* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto should_parenthesise = m_options.explicit_parentheses;

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    TemporaryChange parens { m_options.explicit_parentheses, false };

    if (should_parenthesise)
        current_builder().append('(');

    node->left()->visit(*this);

    current_builder().append(' ');

    node->right()->visit(*this);

    if (should_parenthesise)
        current_builder().append(')');
    visited(node);
}

void Formatter::visit(const AST::MatchExpr* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append("match "sv);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->matched_expr()->visit(*this);

    if (!node->expr_name().is_empty()) {
        current_builder().append(" as "sv);
        current_builder().append(node->expr_name());
    }

    current_builder().append(' ');
    in_new_block([&] {
        auto first_entry = true;
        for (auto& entry : node->entries()) {
            if (!first_entry)
                insert_separator();
            first_entry = false;
            auto first = true;
            entry.options.visit(
                [&](Vector<NonnullRefPtr<AST::Node>> const& patterns) {
                    for (auto& option : patterns) {
                        if (!first)
                            current_builder().append(" | "sv);
                        first = false;
                        option->visit(*this);
                    }
                },
                [&](Vector<Regex<ECMA262>> const& patterns) {
                    for (auto& option : patterns) {
                        if (!first)
                            current_builder().append(" | "sv);
                        first = false;
                        auto node = make_ref_counted<AST::BarewordLiteral>(AST::Position {}, String::from_byte_string(option.pattern_value).release_value_but_fixme_should_propagate_errors());
                        node->visit(*this);
                    }
                });

            current_builder().append(' ');
            if (entry.match_names.has_value() && !entry.match_names.value().is_empty()) {
                current_builder().append("as ("sv);
                auto first = true;
                for (auto& name : entry.match_names.value()) {
                    if (!first)
                        current_builder().append(' ');
                    first = false;
                    current_builder().append(name);
                }
                current_builder().append(") "sv);
            }
            in_new_block([&] {
                if (entry.body)
                    entry.body->visit(*this);
            });
        }
    });
    visited(node);
}

void Formatter::visit(const AST::Or* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->kind() != AST::Node::Kind::Or;
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    with_added_indent(should_indent ? 1 : 0, [&] {
        node->left()->visit(*this);

        current_builder().append(" "sv);
        insert_separator(true);
        current_builder().append("|| "sv);

        node->right()->visit(*this);
    });
    visited(node);
}

void Formatter::visit(const AST::Pipe* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->kind() != AST::Node::Kind::Pipe;
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->left()->visit(*this);
    current_builder().append(" "sv);

    with_added_indent(should_indent ? 1 : 0, [&] {
        insert_separator(true);
        current_builder().append("| "sv);

        node->right()->visit(*this);
    });
    visited(node);
}

void Formatter::visit(const AST::Range* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    if (!m_parent_node || m_parent_node->kind() != AST::Node::Kind::Slice)
        current_builder().append('{');

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    node->start()->visit(*this);
    current_builder().append(".."sv);
    node->end()->visit(*this);

    if (!m_parent_node || m_parent_node->kind() != AST::Node::Kind::Slice)
        current_builder().append('}');
    visited(node);
}

void Formatter::visit(const AST::ReadRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 0)
        current_builder().appendff(" {}<", node->fd());
    else
        current_builder().append(" <"sv);
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::ReadWriteRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 0)
        current_builder().appendff(" {}<>", node->fd());
    else
        current_builder().append(" <>"sv);
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::Sequence* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    bool first = true;
    for (auto& entry : node->entries()) {
        if (first)
            first = false;
        else
            insert_separator();

        entry->visit(*this);
    }

    visited(node);
}

void Formatter::visit(const AST::Subshell* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    in_new_block([&] {
        NodeVisitor::visit(node);
    });
    visited(node);
}

void Formatter::visit(const AST::Slice* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().append('[');
    node->selector()->visit(*this);
    current_builder().append(']');

    visited(node);
}

void Formatter::visit(const AST::SimpleVariable* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append('$');
    current_builder().append(node->name());
    if (const AST::Node* slice = node->slice())
        slice->visit(*this);
    visited(node);
}

void Formatter::visit(const AST::SpecialVariable* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append('$');
    current_builder().append(node->name());
    if (const AST::Node* slice = node->slice())
        slice->visit(*this);
    visited(node);
}

void Formatter::visit(const AST::Juxtaposition* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::StringLiteral* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    if (!m_options.in_double_quotes && !m_options.in_heredoc)
        current_builder().append("'"sv);

    if (m_options.in_double_quotes && !m_options.in_heredoc) {
        for (auto ch : node->text().bytes_as_string_view()) {
            switch (ch) {
            case '"':
            case '\\':
            case '$':
                current_builder().append('\\');
                break;
            case '\n':
                current_builder().append("\\n"sv);
                continue;
            case '\r':
                current_builder().append("\\r"sv);
                continue;
            case '\t':
                current_builder().append("\\t"sv);
                continue;
            case '\v':
                current_builder().append("\\v"sv);
                continue;
            case '\f':
                current_builder().append("\\f"sv);
                continue;
            case '\a':
                current_builder().append("\\a"sv);
                continue;
            case '\e':
                current_builder().append("\\e"sv);
                continue;
            default:
                break;
            }
            current_builder().append(ch);
        }
    } else {
        current_builder().append(node->text());
    }

    if (!m_options.in_double_quotes && !m_options.in_heredoc)
        current_builder().append("'"sv);
    visited(node);
}

void Formatter::visit(const AST::StringPartCompose* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::SyntaxError* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::Tilde* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
    visited(node);
}

void Formatter::visit(const AST::VariableDeclarations* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& entry : node->variables()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        entry.name->visit(*this);
        current_builder().append('=');

        if (entry.value->is_command())
            current_builder().append('(');

        entry.value->visit(*this);

        if (entry.value->is_command())
            current_builder().append(')');
    }
    visited(node);
}

void Formatter::visit(const AST::WriteAppendRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 1)
        current_builder().appendff(" {}>>", node->fd());
    else
        current_builder().append(" >>"sv);
    NodeVisitor::visit(node);
    visited(node);
}

void Formatter::visit(const AST::WriteRedirection* node)
{
    will_visit(node);
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 1)
        current_builder().appendff(" {}>", node->fd());
    else
        current_builder().append(" >"sv);
    NodeVisitor::visit(node);
    visited(node);
}

}
