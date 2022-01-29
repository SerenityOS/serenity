/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"
#include "Shell.h"
#include <AK/MemoryStream.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <unistd.h>

ErrorOr<void> AK::Formatter<Shell::AST::Command>::format(FormatBuilder& builder, Shell::AST::Command const& value)
{
    if (m_sign_mode != FormatBuilder::SignMode::Default)
        VERIFY_NOT_REACHED();
    if (m_alternative_form)
        VERIFY_NOT_REACHED();
    if (m_zero_pad)
        VERIFY_NOT_REACHED();
    if (m_mode != Mode::Default && m_mode != Mode::String)
        VERIFY_NOT_REACHED();
    if (m_width.has_value())
        VERIFY_NOT_REACHED();
    if (m_precision.has_value())
        VERIFY_NOT_REACHED();

    if (value.argv.is_empty()) {
        TRY(builder.put_literal("(ShellInternal)"));
    } else {
        bool first = true;
        for (auto& arg : value.argv) {
            if (!first)
                TRY(builder.put_literal(" "));
            first = false;
            TRY(builder.put_literal(arg));
        }
    }

    for (auto& redir : value.redirections) {
        TRY(builder.put_padding(' ', 1));
        if (redir.is_path_redirection()) {
            auto path_redir = (const Shell::AST::PathRedirection*)&redir;
            TRY(builder.put_i64(path_redir->fd));
            switch (path_redir->direction) {
            case Shell::AST::PathRedirection::Read:
                TRY(builder.put_literal("<"));
                break;
            case Shell::AST::PathRedirection::Write:
                TRY(builder.put_literal(">"));
                break;
            case Shell::AST::PathRedirection::WriteAppend:
                TRY(builder.put_literal(">>"));
                break;
            case Shell::AST::PathRedirection::ReadWrite:
                TRY(builder.put_literal("<>"));
                break;
            }
            TRY(builder.put_literal(path_redir->path));
        } else if (redir.is_fd_redirection()) {
            auto* fdredir = (const Shell::AST::FdRedirection*)&redir;
            TRY(builder.put_i64(fdredir->new_fd));
            TRY(builder.put_literal(">"));
            TRY(builder.put_i64(fdredir->old_fd));
        } else if (redir.is_close_redirection()) {
            auto close_redir = (const Shell::AST::CloseRedirection*)&redir;
            TRY(builder.put_i64(close_redir->fd));
            TRY(builder.put_literal(">&-"));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    if (!value.next_chain.is_empty()) {
        for (auto& command : value.next_chain) {
            switch (command.action) {
            case Shell::AST::NodeWithAction::And:
                TRY(builder.put_literal(" && "));
                break;
            case Shell::AST::NodeWithAction::Or:
                TRY(builder.put_literal(" || "));
                break;
            case Shell::AST::NodeWithAction::Sequence:
                TRY(builder.put_literal("; "));
                break;
            }

            TRY(builder.put_literal("("));
            TRY(builder.put_literal(command.node->class_name()));
            TRY(builder.put_literal("...)"));
        }
    }
    if (!value.should_wait)
        TRY(builder.put_literal("&"));
    return {};
}

namespace Shell::AST {

static inline void print_indented(StringView str, int indent)
{
    dbgln("{}{}", String::repeated(' ', indent * 2), str);
}

static inline Optional<Position> merge_positions(const Optional<Position>& left, const Optional<Position>& right)
{
    if (!left.has_value())
        return right;

    if (!right.has_value())
        return left;

    return Position {
        .start_offset = left->start_offset,
        .end_offset = right->end_offset,
        .start_line = left->start_line,
        .end_line = right->end_line,
    };
}

static inline Vector<Command> join_commands(Vector<Command> left, Vector<Command> right)
{
    Command command;

    auto last_in_left = left.take_last();
    auto first_in_right = right.take_first();

    command.argv.extend(last_in_left.argv);
    command.argv.extend(first_in_right.argv);

    command.redirections.extend(last_in_left.redirections);
    command.redirections.extend(first_in_right.redirections);

    command.should_wait = first_in_right.should_wait && last_in_left.should_wait;
    command.is_pipe_source = first_in_right.is_pipe_source;
    command.should_notify_if_in_background = first_in_right.should_notify_if_in_background || last_in_left.should_notify_if_in_background;

    command.position = merge_positions(last_in_left.position, first_in_right.position);

    Vector<Command> commands;
    commands.extend(left);
    commands.append(command);
    commands.extend(right);

    return commands;
}

static String resolve_slices(RefPtr<Shell> shell, String&& input_value, NonnullRefPtrVector<Slice> slices)
{
    if (slices.is_empty())
        return move(input_value);

    for (auto& slice : slices) {
        auto value = slice.run(shell);
        if (shell && shell->has_any_error())
            break;

        if (!value) {
            shell->raise_error(Shell::ShellError::InvalidSliceContentsError, "Invalid slice contents", slice.position());
            return move(input_value);
        }

        auto index_values = value->resolve_as_list(shell);
        Vector<size_t> indices;
        indices.ensure_capacity(index_values.size());

        size_t i = 0;
        for (auto& value : index_values) {
            auto maybe_index = value.to_int();
            if (!maybe_index.has_value()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, String::formatted("Invalid value in slice index {}: {} (expected a number)", i, value), slice.position());
                return move(input_value);
            }
            ++i;

            auto index = maybe_index.value();
            auto original_index = index;
            if (index < 0)
                index += input_value.length();

            if (index < 0 || (size_t)index >= input_value.length()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, String::formatted("Slice index {} (evaluated as {}) out of value bounds [0-{})", index, original_index, input_value.length()), slice.position());
                return move(input_value);
            }
            indices.unchecked_append(index);
        }

        StringBuilder builder { indices.size() };
        for (auto& index : indices)
            builder.append(input_value[index]);

        input_value = builder.build();
    }

    return move(input_value);
}

static Vector<String> resolve_slices(RefPtr<Shell> shell, Vector<String>&& values, NonnullRefPtrVector<Slice> slices)
{
    if (slices.is_empty())
        return move(values);

    for (auto& slice : slices) {
        auto value = slice.run(shell);
        if (shell && shell->has_any_error())
            break;

        if (!value) {
            shell->raise_error(Shell::ShellError::InvalidSliceContentsError, "Invalid slice contents", slice.position());
            return move(values);
        }

        auto index_values = value->resolve_as_list(shell);
        Vector<size_t> indices;
        indices.ensure_capacity(index_values.size());

        size_t i = 0;
        for (auto& value : index_values) {
            auto maybe_index = value.to_int();
            if (!maybe_index.has_value()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, String::formatted("Invalid value in slice index {}: {} (expected a number)", i, value), slice.position());
                return move(values);
            }
            ++i;

            auto index = maybe_index.value();
            auto original_index = index;
            if (index < 0)
                index += values.size();

            if (index < 0 || (size_t)index >= values.size()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, String::formatted("Slice index {} (evaluated as {}) out of value bounds [0-{})", index, original_index, values.size()), slice.position());
                return move(values);
            }
            indices.unchecked_append(index);
        }

        Vector<String> result;
        result.ensure_capacity(indices.size());
        for (auto& index : indices)
            result.unchecked_append(values[index]);

        values = move(result);
    }

    return move(values);
}

void Node::clear_syntax_error()
{
    m_syntax_error_node->clear_syntax_error();
}

void Node::set_is_syntax_error(const SyntaxError& error_node)
{
    if (!m_syntax_error_node) {
        m_syntax_error_node = error_node;
    } else {
        m_syntax_error_node->set_is_syntax_error(error_node);
    }
}

bool Node::is_syntax_error() const
{
    return m_syntax_error_node && m_syntax_error_node->is_syntax_error();
}

void Node::for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback)
{
    auto value = run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return;

    if (value->is_job()) {
        callback(value);
        return;
    }

    if (value->is_list_without_resolution()) {
        auto list = value->resolve_without_cast(shell);
        for (auto& element : static_cast<ListValue*>(list.ptr())->values()) {
            if (callback(element) == IterationDecision::Break)
                break;
        }
        return;
    }

    auto list = value->resolve_as_list(shell);
    for (auto& element : list) {
        if (callback(make_ref_counted<StringValue>(move(element))) == IterationDecision::Break)
            break;
    }
}

Vector<Command> Node::to_lazy_evaluated_commands(RefPtr<Shell> shell)
{
    if (would_execute()) {
        // Wrap the node in a "should immediately execute next" command.
        return {
            Command { {}, {}, true, false, true, true, {}, { NodeWithAction(*this, NodeWithAction::Sequence) }, position() }
        };
    }

    return run(shell)->resolve_as_commands(shell);
}

void Node::dump(int level) const
{
    print_indented(String::formatted("{} at {}:{} (from {}.{} to {}.{})",
                       class_name().characters(),
                       m_position.start_offset,
                       m_position.end_offset,
                       m_position.start_line.line_number,
                       m_position.start_line.line_column,
                       m_position.end_line.line_number,
                       m_position.end_line.line_column),
        level);
}

Node::Node(Position position)
    : m_position(position)
{
}

Vector<Line::CompletionSuggestion> Node::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (matching_node) {
        if (matching_node->is_bareword()) {
            auto* node = static_cast<BarewordLiteral*>(matching_node.ptr());
            auto corrected_offset = find_offset_into_node(node->text(), offset - matching_node->position().start_offset);

            if (corrected_offset > node->text().length())
                return {};
            auto& text = node->text();

            // If the literal isn't an option, treat it as a path.
            if (!(text.starts_with("-") || text == "--" || text == "-"))
                return shell.complete_path("", text, corrected_offset, Shell::ExecutableOnly::No);

            // If the literal is an option, we have to know the program name
            // should we have no way to get that, bail early.

            if (!hit_test_result.closest_command_node)
                return {};

            auto program_name_node = hit_test_result.closest_command_node->leftmost_trivial_literal();
            if (!program_name_node)
                return {};

            String program_name;
            if (program_name_node->is_bareword())
                program_name = static_cast<BarewordLiteral*>(program_name_node.ptr())->text();
            else
                program_name = static_cast<StringLiteral*>(program_name_node.ptr())->text();

            return shell.complete_option(program_name, text, corrected_offset);
        }
        return {};
    }
    auto result = hit_test_position(offset);
    if (!result.matching_node)
        return {};
    auto node = result.matching_node;
    if (node->is_bareword() || node != result.closest_node_with_semantic_meaning)
        node = result.closest_node_with_semantic_meaning;

    if (!node)
        return {};

    return node->complete_for_editor(shell, offset, result);
}

Vector<Line::CompletionSuggestion> Node::complete_for_editor(Shell& shell, size_t offset)
{
    return Node::complete_for_editor(shell, offset, { nullptr, nullptr, nullptr });
}

Node::~Node()
{
}

void And::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> And::run(RefPtr<Shell> shell)
{
    auto commands = m_left->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    commands.last().next_chain.append(NodeWithAction { *m_right, NodeWithAction::And });
    return make_ref_counted<CommandSequenceValue>(move(commands));
}

void And::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;
    m_left->highlight_in_editor(editor, shell, metadata);
    m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult And::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node) {
        if (!result.closest_command_node)
            result.closest_command_node = m_right;
        return result;
    }

    result = m_right->hit_test_position(offset);
    if (!result.closest_command_node)
        result.closest_command_node = m_right;
    return result;
}

And::And(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right, Position and_position)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
    , m_and_position(and_position)
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

And::~And()
{
}

void ListConcatenate::dump(int level) const
{
    Node::dump(level);
    for (auto& element : m_list)
        element->dump(level + 1);
}

RefPtr<Value> ListConcatenate::run(RefPtr<Shell> shell)
{
    RefPtr<Value> result = nullptr;

    for (auto& element : m_list) {
        if (shell && shell->has_any_error())
            break;
        if (!result) {
            result = make_ref_counted<ListValue>({ element->run(shell)->resolve_without_cast(shell) });
            continue;
        }
        auto element_value = element->run(shell)->resolve_without_cast(shell);
        if (shell && shell->has_any_error())
            break;

        if (result->is_command() || element_value->is_command()) {
            auto joined_commands = join_commands(result->resolve_as_commands(shell), element_value->resolve_as_commands(shell));

            if (joined_commands.size() == 1) {
                auto& command = joined_commands[0];
                command.position = position();
                result = make_ref_counted<CommandValue>(command);
            } else {
                result = make_ref_counted<CommandSequenceValue>(move(joined_commands));
            }
        } else {
            NonnullRefPtrVector<Value> values;

            if (result->is_list_without_resolution()) {
                values.extend(static_cast<ListValue*>(result.ptr())->values());
            } else {
                for (auto& result : result->resolve_as_list(shell))
                    values.append(make_ref_counted<StringValue>(result));
            }

            values.append(element_value);

            result = make_ref_counted<ListValue>(move(values));
        }
    }
    if (!result)
        return make_ref_counted<ListValue>({});

    return result;
}

void ListConcatenate::for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback)
{
    for (auto& entry : m_list) {
        auto value = entry->run(shell);
        if (shell && shell->has_any_error())
            break;
        if (!value)
            continue;
        if (callback(value.release_nonnull()) == IterationDecision::Break)
            break;
    }
}

void ListConcatenate::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    auto first = metadata.is_first_in_list;
    metadata.is_first_in_list = false;

    metadata.is_first_in_list = first;
    for (auto& element : m_list) {
        element->highlight_in_editor(editor, shell, metadata);
        metadata.is_first_in_list = false;
    }
}

HitTestResult ListConcatenate::hit_test_position(size_t offset) const
{
    bool first = true;
    for (auto& element : m_list) {
        auto result = element->hit_test_position(offset);
        if (!result.closest_node_with_semantic_meaning && !first)
            result.closest_node_with_semantic_meaning = this;
        if (result.matching_node)
            return result;
        first = false;
    }

    return {};
}

RefPtr<Node> ListConcatenate::leftmost_trivial_literal() const
{
    if (m_list.is_empty())
        return nullptr;

    return m_list.first()->leftmost_trivial_literal();
}

ListConcatenate::ListConcatenate(Position position, Vector<NonnullRefPtr<Node>> list)
    : Node(move(position))
    , m_list(move(list))
{
    for (auto& element : m_list) {
        if (element->is_syntax_error()) {
            set_is_syntax_error(element->syntax_error_node());
            break;
        }
    }
}

ListConcatenate::~ListConcatenate()
{
}

void Background::dump(int level) const
{
    Node::dump(level);
    m_command->dump(level + 1);
}

RefPtr<Value> Background::run(RefPtr<Shell> shell)
{
    auto commands = m_command->to_lazy_evaluated_commands(shell);
    for (auto& command : commands)
        command.should_wait = false;

    return make_ref_counted<CommandSequenceValue>(move(commands));
}

void Background::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_command->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Background::hit_test_position(size_t offset) const
{
    return m_command->hit_test_position(offset);
}

Background::Background(Position position, NonnullRefPtr<Node> command)
    : Node(move(position))
    , m_command(move(command))
{
    if (m_command->is_syntax_error())
        set_is_syntax_error(m_command->syntax_error_node());
}

Background::~Background()
{
}

void BarewordLiteral::dump(int level) const
{
    Node::dump(level);
    print_indented(m_text, level + 1);
}

RefPtr<Value> BarewordLiteral::run(RefPtr<Shell>)
{
    return make_ref_counted<StringValue>(m_text);
}

void BarewordLiteral::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (metadata.is_first_in_list) {
        if (shell.is_runnable(m_text)) {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Bold });
        } else {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Red) });
        }

        return;
    }

    if (m_text.starts_with('-')) {
        if (m_text == "--") {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });
            return;
        }
        if (m_text == "-")
            return;

        if (m_text.starts_with("--")) {
            auto index = m_text.find('=').value_or(m_text.length() - 1) + 1;
            editor.stylize({ m_position.start_offset, m_position.start_offset + index }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan) });
        } else {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan) });
        }
    }
    if (Core::File::exists(m_text)) {
        auto realpath = shell.resolve_path(m_text);
        auto url = URL::create_with_file_protocol(realpath);
        url.set_host(shell.hostname);
        editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Hyperlink(url.to_string()) });
    }
}

BarewordLiteral::BarewordLiteral(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

BarewordLiteral::~BarewordLiteral()
{
}

void BraceExpansion::dump(int level) const
{
    Node::dump(level);
    for (auto& entry : m_entries)
        entry.dump(level + 1);
}

RefPtr<Value> BraceExpansion::run(RefPtr<Shell> shell)
{
    NonnullRefPtrVector<Value> values;
    for (auto& entry : m_entries) {
        if (shell && shell->has_any_error())
            break;
        auto value = entry.run(shell);
        if (value)
            values.append(value.release_nonnull());
    }

    return make_ref_counted<ListValue>(move(values));
}

HitTestResult BraceExpansion::hit_test_position(size_t offset) const
{
    for (auto& entry : m_entries) {
        auto result = entry.hit_test_position(offset);
        if (result.matching_node) {
            if (!result.closest_command_node)
                result.closest_command_node = &entry;
            return result;
        }
    }

    return {};
}

void BraceExpansion::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    for (auto& entry : m_entries) {
        entry.highlight_in_editor(editor, shell, metadata);
        metadata.is_first_in_list = false;
    }
}

BraceExpansion::BraceExpansion(Position position, NonnullRefPtrVector<Node> entries)
    : Node(move(position))
    , m_entries(move(entries))
{
    for (auto& entry : m_entries) {
        if (entry.is_syntax_error()) {
            set_is_syntax_error(entry.syntax_error_node());
            break;
        }
    }
}

BraceExpansion::~BraceExpansion()
{
}

void CastToCommand::dump(int level) const
{
    Node::dump(level);
    m_inner->dump(level + 1);
}

RefPtr<Value> CastToCommand::run(RefPtr<Shell> shell)
{
    if (m_inner->is_command())
        return m_inner->run(shell);

    auto value = m_inner->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (value->is_command())
        return value;

    auto argv = value->resolve_as_list(shell);
    return make_ref_counted<CommandValue>(move(argv), position());
}

void CastToCommand::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_inner->highlight_in_editor(editor, shell, metadata);
}

HitTestResult CastToCommand::hit_test_position(size_t offset) const
{
    auto result = m_inner->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    return result;
}

Vector<Line::CompletionSuggestion> CastToCommand::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral*>(matching_node.ptr());

    if (corrected_offset > node->text().length())
        return {};

    return shell.complete_program_name(node->text(), corrected_offset);
}

RefPtr<Node> CastToCommand::leftmost_trivial_literal() const
{
    return m_inner->leftmost_trivial_literal();
}

CastToCommand::CastToCommand(Position position, NonnullRefPtr<Node> inner)
    : Node(move(position))
    , m_inner(move(inner))
{
    if (m_inner->is_syntax_error())
        set_is_syntax_error(m_inner->syntax_error_node());
}

CastToCommand::~CastToCommand()
{
}

void CastToList::dump(int level) const
{
    Node::dump(level);
    if (m_inner)
        m_inner->dump(level + 1);
    else
        print_indented("(empty)", level + 1);
}

RefPtr<Value> CastToList::run(RefPtr<Shell> shell)
{
    if (!m_inner)
        return make_ref_counted<ListValue>({});

    auto inner_value = m_inner->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (inner_value->is_command() || inner_value->is_list())
        return inner_value;

    auto values = inner_value->resolve_as_list(shell);
    NonnullRefPtrVector<Value> cast_values;
    for (auto& value : values)
        cast_values.append(make_ref_counted<StringValue>(value));

    return make_ref_counted<ListValue>(cast_values);
}

void CastToList::for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback)
{
    if (m_inner)
        m_inner->for_each_entry(shell, move(callback));
}

void CastToList::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (m_inner)
        m_inner->highlight_in_editor(editor, shell, metadata);
}

HitTestResult CastToList::hit_test_position(size_t offset) const
{
    if (!m_inner)
        return {};

    return m_inner->hit_test_position(offset);
}

RefPtr<Node> CastToList::leftmost_trivial_literal() const
{
    return m_inner->leftmost_trivial_literal();
}

CastToList::CastToList(Position position, RefPtr<Node> inner)
    : Node(move(position))
    , m_inner(move(inner))
{
    if (m_inner && m_inner->is_syntax_error())
        set_is_syntax_error(m_inner->syntax_error_node());
}

CastToList::~CastToList()
{
}

void CloseFdRedirection::dump(int level) const
{
    Node::dump(level);
    print_indented(String::formatted("{} -> Close", m_fd), level);
}

RefPtr<Value> CloseFdRedirection::run(RefPtr<Shell>)
{
    Command command;
    command.position = position();
    command.redirections.append(adopt_ref(*new CloseRedirection(m_fd)));
    return make_ref_counted<CommandValue>(move(command));
}

void CloseFdRedirection::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset - 1 }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
    editor.stylize({ m_position.end_offset - 1, m_position.end_offset }, { Line::Style::Foreground(0xff, 0x7e, 0x00) });   // Amber
}

CloseFdRedirection::CloseFdRedirection(Position position, int fd)
    : Node(move(position))
    , m_fd(fd)
{
}

CloseFdRedirection::~CloseFdRedirection()
{
}

void CommandLiteral::dump(int level) const
{
    Node::dump(level);
    print_indented(String::formatted("(Generated command literal: {})", m_command), level + 1);
}

RefPtr<Value> CommandLiteral::run(RefPtr<Shell>)
{
    return make_ref_counted<CommandValue>(m_command);
}

CommandLiteral::CommandLiteral(Position position, Command command)
    : Node(move(position))
    , m_command(move(command))
{
}

CommandLiteral::~CommandLiteral()
{
}

void Comment::dump(int level) const
{
    Node::dump(level);
    print_indented(m_text, level + 1);
}

RefPtr<Value> Comment::run(RefPtr<Shell>)
{
    return make_ref_counted<ListValue>({});
}

void Comment::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(150, 150, 150) }); // Light gray
}

Comment::Comment(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

Comment::~Comment()
{
}

void ContinuationControl::dump(int level) const
{
    Node::dump(level);
    print_indented(m_kind == Continue ? "(Continue)" : "(Break)", level + 1);
}

RefPtr<Value> ContinuationControl::run(RefPtr<Shell> shell)
{
    if (m_kind == Break)
        shell->raise_error(Shell::ShellError::InternalControlFlowBreak, {}, position());
    else if (m_kind == Continue)
        shell->raise_error(Shell::ShellError::InternalControlFlowContinue, {}, position());
    else
        VERIFY_NOT_REACHED();
    return make_ref_counted<ListValue>({});
}

void ContinuationControl::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
}

void DoubleQuotedString::dump(int level) const
{
    Node::dump(level);
    m_inner->dump(level + 1);
}

RefPtr<Value> DoubleQuotedString::run(RefPtr<Shell> shell)
{
    StringBuilder builder;
    auto values = m_inner->run(shell)->resolve_as_list(shell);

    builder.join("", values);

    return make_ref_counted<StringValue>(builder.to_string());
}

void DoubleQuotedString::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });

    editor.stylize({ m_position.start_offset, m_position.end_offset }, style);
    metadata.is_first_in_list = false;
    m_inner->highlight_in_editor(editor, shell, metadata);
}

HitTestResult DoubleQuotedString::hit_test_position(size_t offset) const
{
    return m_inner->hit_test_position(offset);
}

DoubleQuotedString::DoubleQuotedString(Position position, RefPtr<Node> inner)
    : Node(move(position))
    , m_inner(move(inner))
{
    if (m_inner->is_syntax_error())
        set_is_syntax_error(m_inner->syntax_error_node());
}

DoubleQuotedString::~DoubleQuotedString()
{
}

void DynamicEvaluate::dump(int level) const
{
    Node::dump(level);
    m_inner->dump(level + 1);
}

RefPtr<Value> DynamicEvaluate::run(RefPtr<Shell> shell)
{
    auto result = m_inner->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    // Dynamic Evaluation behaves differently between strings and lists.
    // Strings are treated as variables, and Lists are treated as commands.
    if (result->is_string()) {
        auto name_part = result->resolve_as_list(shell);
        VERIFY(name_part.size() == 1);
        return make_ref_counted<SimpleVariableValue>(name_part[0]);
    }

    // If it's anything else, we're just gonna cast it to a list.
    auto list = result->resolve_as_list(shell);
    return make_ref_counted<CommandValue>(move(list), position());
}

void DynamicEvaluate::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    m_inner->highlight_in_editor(editor, shell, metadata);
}

HitTestResult DynamicEvaluate::hit_test_position(size_t offset) const
{
    return m_inner->hit_test_position(offset);
}

DynamicEvaluate::DynamicEvaluate(Position position, NonnullRefPtr<Node> inner)
    : Node(move(position))
    , m_inner(move(inner))
{
    if (m_inner->is_syntax_error())
        set_is_syntax_error(m_inner->syntax_error_node());
}

DynamicEvaluate::~DynamicEvaluate()
{
}

void Fd2FdRedirection::dump(int level) const
{
    Node::dump(level);
    print_indented(String::formatted("{} -> {}", m_old_fd, m_new_fd), level);
}

RefPtr<Value> Fd2FdRedirection::run(RefPtr<Shell>)
{
    Command command;
    command.position = position();
    command.redirections.append(FdRedirection::create(m_new_fd, m_old_fd, Rewiring::Close::None));
    return make_ref_counted<CommandValue>(move(command));
}

void Fd2FdRedirection::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
}

Fd2FdRedirection::Fd2FdRedirection(Position position, int src, int dst)
    : Node(move(position))
    , m_old_fd(src)
    , m_new_fd(dst)
{
}

Fd2FdRedirection::~Fd2FdRedirection()
{
}

void FunctionDeclaration::dump(int level) const
{
    Node::dump(level);
    print_indented(String::formatted("(name: {})\n", m_name.name), level + 1);
    print_indented("(argument names)", level + 1);
    for (auto& arg : m_arguments)
        print_indented(String::formatted("(name: {})\n", arg.name), level + 2);

    print_indented("(body)", level + 1);
    if (m_block)
        m_block->dump(level + 2);
    else
        print_indented("(null)", level + 2);
}

RefPtr<Value> FunctionDeclaration::run(RefPtr<Shell> shell)
{
    Vector<String> args;
    for (auto& arg : m_arguments)
        args.append(arg.name);

    shell->define_function(m_name.name, move(args), m_block);

    return make_ref_counted<ListValue>({});
}

void FunctionDeclaration::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_name.position.start_offset, m_name.position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue) });

    for (auto& arg : m_arguments)
        editor.stylize({ arg.position.start_offset, arg.position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    metadata.is_first_in_list = true;
    if (m_block)
        m_block->highlight_in_editor(editor, shell, metadata);
}

HitTestResult FunctionDeclaration::hit_test_position(size_t offset) const
{
    if (!m_block)
        return {};

    auto result = m_block->hit_test_position(offset);
    if (result.matching_node && result.matching_node->is_simple_variable())
        result.closest_node_with_semantic_meaning = this;
    return result;
}

Vector<Line::CompletionSuggestion> FunctionDeclaration::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return {};

    if (!matching_node->is_simple_variable())
        return matching_node->complete_for_editor(shell, offset, hit_test_result);

    auto corrected_offset = offset - matching_node->position().start_offset - 1; // Skip the first '$'
    auto* node = static_cast<SimpleVariable*>(matching_node.ptr());

    auto name = node->name().substring_view(0, corrected_offset);

    Vector<Line::CompletionSuggestion> results;
    for (auto& arg : m_arguments) {
        if (arg.name.starts_with(name))
            results.append(arg.name);
    }

    results.extend(matching_node->complete_for_editor(shell, offset, hit_test_result));

    return results;
}

FunctionDeclaration::FunctionDeclaration(Position position, NameWithPosition name, Vector<NameWithPosition> arguments, RefPtr<AST::Node> body)
    : Node(move(position))
    , m_name(move(name))
    , m_arguments(arguments)
    , m_block(move(body))
{
    if (m_block && m_block->is_syntax_error())
        set_is_syntax_error(m_block->syntax_error_node());
}

FunctionDeclaration::~FunctionDeclaration()
{
}

void ForLoop::dump(int level) const
{
    Node::dump(level);
    if (m_variable.has_value())
        print_indented(String::formatted("iterating with {} in", m_variable->name), level + 1);
    if (m_index_variable.has_value())
        print_indented(String::formatted("with index name {} in", m_index_variable->name), level + 1);
    if (m_iterated_expression)
        m_iterated_expression->dump(level + 2);
    else
        print_indented("(ever)", level + 2);
    print_indented("Running", level + 1);
    if (m_block)
        m_block->dump(level + 2);
    else
        print_indented("(null)", level + 2);
}

RefPtr<Value> ForLoop::run(RefPtr<Shell> shell)
{
    if (!m_block)
        return make_ref_counted<ListValue>({});

    size_t consecutive_interruptions = 0;
    auto run = [&](auto& block_value) {
        if (shell->has_error(Shell::ShellError::InternalControlFlowBreak)) {
            shell->take_error();
            return IterationDecision::Break;
        }

        if (shell->has_error(Shell::ShellError::InternalControlFlowContinue)) {
            shell->take_error();
            return IterationDecision::Continue;
        }

        if (shell->has_any_error() && !shell->has_error(Shell::ShellError::InternalControlFlowInterrupted))
            return IterationDecision::Break;

        if (block_value->is_job()) {
            auto job = static_cast<JobValue*>(block_value.ptr())->job();
            if (!job || job->is_running_in_background())
                return IterationDecision::Continue;
            shell->block_on_job(job);

            if (shell->has_any_error()) {
                if (shell->has_error(Shell::ShellError::InternalControlFlowInterrupted))
                    ++consecutive_interruptions;

                if (shell->has_error(Shell::ShellError::InternalControlFlowKilled))
                    return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    };

    if (m_iterated_expression) {
        auto variable_name = m_variable.has_value() ? m_variable->name : "it";
        Optional<StringView> index_name = m_index_variable.has_value() ? Optional<StringView>(m_index_variable->name) : Optional<StringView>();
        size_t i = 0;
        m_iterated_expression->for_each_entry(shell, [&](auto value) {
            if (consecutive_interruptions >= 2)
                return IterationDecision::Break;

            if (shell) {
                if (shell->has_error(Shell::ShellError::InternalControlFlowInterrupted))
                    shell->take_error();

                if (shell->has_any_error())
                    return IterationDecision::Break;
            }

            RefPtr<Value> block_value;

            {
                auto frame = shell->push_frame(String::formatted("for ({})", this));
                shell->set_local_variable(variable_name, value, true);

                if (index_name.has_value())
                    shell->set_local_variable(index_name.value(), make_ref_counted<AST::StringValue>(String::number(i)), true);

                ++i;

                block_value = m_block->run(shell);
            }

            return run(block_value);
        });
    } else {
        for (;;) {
            if (consecutive_interruptions >= 2)
                break;

            if (shell) {
                if (shell->has_error(Shell::ShellError::InternalControlFlowInterrupted))
                    shell->take_error();

                if (shell->has_any_error())
                    break;
            }

            RefPtr<Value> block_value = m_block->run(shell);
            if (run(block_value) == IterationDecision::Break)
                break;
        }
    }

    return make_ref_counted<ListValue>({});
}

void ForLoop::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    auto is_loop = m_iterated_expression.is_null();
    editor.stylize({ m_position.start_offset, m_position.start_offset + (is_loop ? 4 : 3) }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (!is_loop) {
        if (m_in_kw_position.has_value())
            editor.stylize({ m_in_kw_position.value().start_offset, m_in_kw_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        if (m_index_kw_position.has_value())
            editor.stylize({ m_index_kw_position.value().start_offset, m_index_kw_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        metadata.is_first_in_list = false;
        m_iterated_expression->highlight_in_editor(editor, shell, metadata);
    }

    if (m_index_variable.has_value())
        editor.stylize({ m_index_variable->position.start_offset, m_index_variable->position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    if (m_variable.has_value())
        editor.stylize({ m_variable->position.start_offset, m_variable->position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    metadata.is_first_in_list = true;
    if (m_block)
        m_block->highlight_in_editor(editor, shell, metadata);
}

HitTestResult ForLoop::hit_test_position(size_t offset) const
{
    if (m_iterated_expression) {
        if (auto result = m_iterated_expression->hit_test_position(offset); result.matching_node)
            return result;
    }

    if (!m_block)
        return {};

    return m_block->hit_test_position(offset);
}

ForLoop::ForLoop(Position position, Optional<NameWithPosition> variable, Optional<NameWithPosition> index_variable, RefPtr<AST::Node> iterated_expr, RefPtr<AST::Node> block, Optional<Position> in_kw_position, Optional<Position> index_kw_position)
    : Node(move(position))
    , m_variable(move(variable))
    , m_index_variable(move(index_variable))
    , m_iterated_expression(move(iterated_expr))
    , m_block(move(block))
    , m_in_kw_position(move(in_kw_position))
    , m_index_kw_position(move(index_kw_position))
{
    if (m_iterated_expression && m_iterated_expression->is_syntax_error())
        set_is_syntax_error(m_iterated_expression->syntax_error_node());
    else if (m_block && m_block->is_syntax_error())
        set_is_syntax_error(m_block->syntax_error_node());
}

ForLoop::~ForLoop()
{
}

void Glob::dump(int level) const
{
    Node::dump(level);
    print_indented(m_text, level + 1);
}

RefPtr<Value> Glob::run(RefPtr<Shell>)
{
    return make_ref_counted<GlobValue>(m_text, position());
}

void Glob::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Cyan) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
}

Glob::Glob(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

Glob::~Glob()
{
}

void Heredoc::dump(int level) const
{
    Node::dump(level);
    print_indented("(End Key)", level + 1);
    print_indented(m_end, level + 2);
    print_indented("(Allows Interpolation)", level + 1);
    print_indented(String::formatted("{}", m_allows_interpolation), level + 2);
    print_indented("(Contents)", level + 1);
    if (m_contents)
        m_contents->dump(level + 2);
    else
        print_indented("(null)", level + 2);
}

RefPtr<Value> Heredoc::run(RefPtr<Shell> shell)
{
    if (!m_deindent)
        return m_contents->run(shell);

    // To deindent, first split to lines...
    auto value = m_contents->run(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (!value)
        return value;
    auto list = value->resolve_as_list(shell);
    // The list better have one entry, otherwise we've put the wrong kind of node inside this heredoc
    VERIFY(list.size() == 1);
    auto lines = list.first().split_view('\n');

    // Now just trim each line and put them back in a string
    StringBuilder builder { list.first().length() };
    for (auto& line : lines) {
        builder.append(line.trim_whitespace(TrimMode::Left));
        builder.append('\n');
    }

    return make_ref_counted<StringValue>(builder.to_string());
}

void Heredoc::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style content_style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        content_style.unify_with({ Line::Style::Bold });

    if (!m_contents)
        content_style.unify_with({ Line::Style::Foreground(Line::Style::XtermColor::Red) }, true);

    editor.stylize({ m_position.start_offset, m_position.end_offset }, content_style);
    if (m_contents)
        m_contents->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Heredoc::hit_test_position(size_t offset) const
{
    if (!m_contents)
        return {};

    return m_contents->hit_test_position(offset);
}

Heredoc::Heredoc(Position position, String end, bool allow_interpolation, bool deindent)
    : Node(move(position))
    , m_end(move(end))
    , m_allows_interpolation(allow_interpolation)
    , m_deindent(deindent)
{
}

Heredoc::~Heredoc()
{
}

void HistoryEvent::dump(int level) const
{
    Node::dump(level);
    print_indented("Event Selector", level + 1);
    switch (m_selector.event.kind) {
    case HistorySelector::EventKind::IndexFromStart:
        print_indented("IndexFromStart", level + 2);
        break;
    case HistorySelector::EventKind::IndexFromEnd:
        print_indented("IndexFromEnd", level + 2);
        break;
    case HistorySelector::EventKind::ContainingStringLookup:
        print_indented("ContainingStringLookup", level + 2);
        break;
    case HistorySelector::EventKind::StartingStringLookup:
        print_indented("StartingStringLookup", level + 2);
        break;
    }
    print_indented(String::formatted("{}({})", m_selector.event.index, m_selector.event.text), level + 3);

    print_indented("Word Selector", level + 1);
    auto print_word_selector = [&](const HistorySelector::WordSelector& selector) {
        switch (selector.kind) {
        case HistorySelector::WordSelectorKind::Index:
            print_indented(String::formatted("Index {}", selector.selector), level + 3);
            break;
        case HistorySelector::WordSelectorKind::Last:
            print_indented(String::formatted("Last"), level + 3);
            break;
        }
    };

    if (m_selector.word_selector_range.end.has_value()) {
        print_indented("Range Start", level + 2);
        print_word_selector(m_selector.word_selector_range.start);
        print_indented("Range End", level + 2);
        print_word_selector(m_selector.word_selector_range.end.value());
    } else {
        print_indented("Direct Address", level + 2);
        print_word_selector(m_selector.word_selector_range.start);
    }
}

RefPtr<Value> HistoryEvent::run(RefPtr<Shell> shell)
{
    if (!shell)
        return make_ref_counted<AST::ListValue>({});

    auto editor = shell->editor();
    if (!editor) {
        shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "No history available!", position());
        return make_ref_counted<AST::ListValue>({});
    }
    auto& history = editor->history();

    // FIXME: Implement reverse iterators and find()?
    auto find_reverse = [](auto it_start, auto it_end, auto finder) {
        auto it = it_end;
        while (it != it_start) {
            --it;
            if (finder(*it))
                return it;
        }
        return it_end;
    };
    // First, resolve the event itself.
    String resolved_history;
    switch (m_selector.event.kind) {
    case HistorySelector::EventKind::IndexFromStart:
        if (m_selector.event.index >= history.size()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History event index out of bounds", m_selector.event.text_position);
            return make_ref_counted<AST::ListValue>({});
        }
        resolved_history = history[m_selector.event.index].entry;
        break;
    case HistorySelector::EventKind::IndexFromEnd:
        if (m_selector.event.index >= history.size()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History event index out of bounds", m_selector.event.text_position);
            return make_ref_counted<AST::ListValue>({});
        }
        resolved_history = history[history.size() - m_selector.event.index - 1].entry;
        break;
    case HistorySelector::EventKind::ContainingStringLookup: {
        auto it = find_reverse(history.begin(), history.end(), [&](auto& entry) { return entry.entry.contains(m_selector.event.text); });
        if (it.is_end()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History event did not match any entry", m_selector.event.text_position);
            return make_ref_counted<AST::ListValue>({});
        }
        resolved_history = it->entry;
        break;
    }
    case HistorySelector::EventKind::StartingStringLookup: {
        auto it = find_reverse(history.begin(), history.end(), [&](auto& entry) { return entry.entry.starts_with(m_selector.event.text); });
        if (it.is_end()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History event did not match any entry", m_selector.event.text_position);
            return make_ref_counted<AST::ListValue>({});
        }
        resolved_history = it->entry;
        break;
    }
    }

    // Then, split it up to "words".
    auto nodes = Parser { resolved_history }.parse_as_multiple_expressions();

    // Now take the "words" as described by the word selectors.
    bool is_range = m_selector.word_selector_range.end.has_value();
    if (is_range) {
        auto start_index = m_selector.word_selector_range.start.resolve(nodes.size());
        auto end_index = m_selector.word_selector_range.end->resolve(nodes.size());
        if (start_index >= nodes.size()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History word index out of bounds", m_selector.word_selector_range.start.position);
            return make_ref_counted<AST::ListValue>({});
        }
        if (end_index >= nodes.size()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History word index out of bounds", m_selector.word_selector_range.end->position);
            return make_ref_counted<AST::ListValue>({});
        }

        decltype(nodes) resolved_nodes;
        resolved_nodes.append(nodes.data() + start_index, end_index - start_index + 1);
        NonnullRefPtr<AST::Node> list = make_ref_counted<AST::ListConcatenate>(position(), move(resolved_nodes));
        return list->run(shell);
    }

    auto index = m_selector.word_selector_range.start.resolve(nodes.size());
    if (index >= nodes.size()) {
        shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History word index out of bounds", m_selector.word_selector_range.start.position);
        return make_ref_counted<AST::ListValue>({});
    }
    return nodes[index].run(shell);
}

void HistoryEvent::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Green) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
}

HistoryEvent::HistoryEvent(Position position, HistorySelector selector)
    : Node(move(position))
    , m_selector(move(selector))
{
    if (m_selector.word_selector_range.start.syntax_error_node)
        set_is_syntax_error(*m_selector.word_selector_range.start.syntax_error_node);
    else if (m_selector.word_selector_range.end.has_value() && m_selector.word_selector_range.end->syntax_error_node)
        set_is_syntax_error(*m_selector.word_selector_range.end->syntax_error_node);
}

HistoryEvent::~HistoryEvent()
{
}

void Execute::dump(int level) const
{
    Node::dump(level);
    if (m_capture_stdout)
        print_indented("(Capturing stdout)", level + 1);
    m_command->dump(level + 1);
}

void Execute::for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback)
{
    if (m_command->would_execute())
        return m_command->for_each_entry(shell, move(callback));

    auto unexpanded_commands = m_command->run(shell)->resolve_as_commands(shell);
    if (shell && shell->has_any_error())
        return;

    auto commands = shell->expand_aliases(move(unexpanded_commands));

    if (m_capture_stdout) {
        // Make sure that we're going to be running _something_.
        auto has_one_command = false;
        for (auto& command : commands) {
            if (command.argv.is_empty() && !command.pipeline && command.next_chain.is_empty())
                continue;
            has_one_command = true;
            break;
        }

        if (!has_one_command) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Cannot capture standard output when no command is being executed", m_position);
            return;
        }
        int pipefd[2];
        int rc = pipe(pipefd);
        if (rc < 0) {
            dbgln("Error: cannot pipe(): {}", strerror(errno));
            return;
        }
        auto& last_in_commands = commands.last();

        last_in_commands.redirections.prepend(FdRedirection::create(pipefd[1], STDOUT_FILENO, Rewiring::Close::Old));
        last_in_commands.should_wait = false;
        last_in_commands.should_notify_if_in_background = false;
        last_in_commands.is_pipe_source = false;

        Core::EventLoop loop;

        auto notifier = Core::Notifier::construct(pipefd[0], Core::Notifier::Read);
        DuplexMemoryStream stream;

        enum {
            Continue,
            Break,
            NothingLeft,
        };
        auto check_and_call = [&] {
            auto ifs = shell->local_variable_or("IFS", "\n");

            if (auto offset = stream.offset_of(ifs.bytes()); offset.has_value()) {
                auto line_end = offset.value();
                if (line_end == 0) {
                    auto rc = stream.discard_or_error(ifs.length());
                    VERIFY(rc);

                    if (shell->options.inline_exec_keep_empty_segments)
                        if (callback(make_ref_counted<StringValue>("")) == IterationDecision::Break) {
                            loop.quit(Break);
                            notifier->set_enabled(false);
                            return Break;
                        }
                } else {
                    auto entry_result = ByteBuffer::create_uninitialized(line_end + ifs.length());
                    if (entry_result.is_error()) {
                        loop.quit(Break);
                        notifier->set_enabled(false);
                        return Break;
                    }
                    auto entry = entry_result.release_value();
                    auto rc = stream.read_or_error(entry);
                    VERIFY(rc);

                    auto str = StringView(entry.data(), entry.size() - ifs.length());
                    if (callback(make_ref_counted<StringValue>(str)) == IterationDecision::Break) {
                        loop.quit(Break);
                        notifier->set_enabled(false);
                        return Break;
                    }
                }

                return Continue;
            }

            return NothingLeft;
        };

        notifier->on_ready_to_read = [&] {
            constexpr static auto buffer_size = 16;
            u8 buffer[buffer_size];
            size_t remaining_size = buffer_size;

            for (;;) {
                notifier->set_event_mask(Core::Notifier::None);
                bool should_enable_notifier = false;

                ScopeGuard notifier_enabler { [&] {
                    if (should_enable_notifier)
                        notifier->set_event_mask(Core::Notifier::Read);
                } };

                if (check_and_call() == Break) {
                    loop.quit(Break);
                    return;
                }

                auto read_size = read(pipefd[0], buffer, remaining_size);
                if (read_size < 0) {
                    int saved_errno = errno;
                    if (saved_errno == EINTR) {
                        should_enable_notifier = true;
                        continue;
                    }
                    if (saved_errno == 0)
                        continue;
                    dbgln("read() failed: {}", strerror(saved_errno));
                    break;
                }
                if (read_size == 0)
                    break;

                should_enable_notifier = true;
                stream.write({ buffer, (size_t)read_size });
            }

            loop.quit(NothingLeft);
        };

        auto jobs = shell->run_commands(commands);
        ScopeGuard kill_jobs_if_around { [&] {
            for (auto& job : jobs) {
                if (job.is_running_in_background() && !job.exited() && !job.signaled()) {
                    job.set_should_announce_signal(false); // We're explicitly killing it here.
                    shell->kill_job(&job, SIGTERM);
                }
            }
        } };

        auto exit_reason = loop.exec();

        notifier->on_ready_to_read = nullptr;

        if (close(pipefd[0]) < 0) {
            dbgln("close() failed: {}", strerror(errno));
        }

        if (exit_reason != Break && !stream.eof()) {
            auto action = Continue;
            do {
                action = check_and_call();
                if (action == Break)
                    return;
            } while (action == Continue);

            if (!stream.eof()) {
                auto entry_result = ByteBuffer::create_uninitialized(stream.size());
                if (entry_result.is_error()) {
                    shell->raise_error(Shell::ShellError::OutOfMemory, {}, position());
                    return;
                }
                auto entry = entry_result.release_value();
                auto rc = stream.read_or_error(entry);
                VERIFY(rc);
                callback(make_ref_counted<StringValue>(String::copy(entry)));
            }
        }

        return;
    }

    auto jobs = shell->run_commands(commands);

    if (!jobs.is_empty())
        callback(make_ref_counted<JobValue>(&jobs.last()));
}

RefPtr<Value> Execute::run(RefPtr<Shell> shell)
{
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (m_command->would_execute())
        return m_command->run(shell);

    NonnullRefPtrVector<Value> values;
    for_each_entry(shell, [&](auto value) {
        values.append(*value);
        return IterationDecision::Continue;
    });

    if (values.size() == 1 && values.first().is_job())
        return values.first();

    return make_ref_counted<ListValue>(move(values));
}

void Execute::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (m_capture_stdout)
        editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });
    metadata.is_first_in_list = true;
    m_command->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Execute::hit_test_position(size_t offset) const
{
    auto result = m_command->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    if (!result.closest_command_node)
        result.closest_command_node = m_command;
    return result;
}

Vector<Line::CompletionSuggestion> Execute::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral*>(matching_node.ptr());

    if (corrected_offset > node->text().length())
        return {};

    return shell.complete_program_name(node->text(), corrected_offset);
}

Execute::Execute(Position position, NonnullRefPtr<Node> command, bool capture_stdout)
    : Node(move(position))
    , m_command(move(command))
    , m_capture_stdout(capture_stdout)
{
    if (m_command->is_syntax_error())
        set_is_syntax_error(m_command->syntax_error_node());
}

Execute::~Execute()
{
}

void IfCond::dump(int level) const
{
    Node::dump(level);
    print_indented("Condition", ++level);
    m_condition->dump(level + 1);
    print_indented("True Branch", level);
    if (m_true_branch)
        m_true_branch->dump(level + 1);
    else
        print_indented("(empty)", level + 1);
    print_indented("False Branch", level);
    if (m_false_branch)
        m_false_branch->dump(level + 1);
    else
        print_indented("(empty)", level + 1);
}

RefPtr<Value> IfCond::run(RefPtr<Shell> shell)
{
    auto cond = m_condition->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    // The condition could be a builtin, in which case it has already run and exited.
    if (cond->is_job()) {
        auto cond_job_value = static_cast<const JobValue*>(cond.ptr());
        auto cond_job = cond_job_value->job();

        shell->block_on_job(cond_job);
    }
    if (shell->last_return_code == 0) {
        if (m_true_branch)
            return m_true_branch->run(shell);
    } else {
        if (m_false_branch)
            return m_false_branch->run(shell);
    }

    return make_ref_counted<ListValue>({});
}

void IfCond::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;

    editor.stylize({ m_position.start_offset, m_position.start_offset + 2 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (m_else_position.has_value())
        editor.stylize({ m_else_position.value().start_offset, m_else_position.value().start_offset + 4 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    m_condition->highlight_in_editor(editor, shell, metadata);
    if (m_true_branch)
        m_true_branch->highlight_in_editor(editor, shell, metadata);
    if (m_false_branch)
        m_false_branch->highlight_in_editor(editor, shell, metadata);
}

HitTestResult IfCond::hit_test_position(size_t offset) const
{
    if (auto result = m_condition->hit_test_position(offset); result.matching_node)
        return result;

    if (m_true_branch) {
        if (auto result = m_true_branch->hit_test_position(offset); result.matching_node)
            return result;
    }

    if (m_false_branch) {
        if (auto result = m_false_branch->hit_test_position(offset); result.matching_node)
            return result;
    }

    return {};
}

IfCond::IfCond(Position position, Optional<Position> else_position, NonnullRefPtr<Node> condition, RefPtr<Node> true_branch, RefPtr<Node> false_branch)
    : Node(move(position))
    , m_condition(move(condition))
    , m_true_branch(move(true_branch))
    , m_false_branch(move(false_branch))
    , m_else_position(move(else_position))
{
    if (m_condition->is_syntax_error())
        set_is_syntax_error(m_condition->syntax_error_node());
    else if (m_true_branch && m_true_branch->is_syntax_error())
        set_is_syntax_error(m_true_branch->syntax_error_node());
    else if (m_false_branch && m_false_branch->is_syntax_error())
        set_is_syntax_error(m_false_branch->syntax_error_node());

    m_condition = make_ref_counted<AST::Execute>(m_condition->position(), m_condition);

    if (m_true_branch) {
        auto true_branch = m_true_branch.release_nonnull();
        if (true_branch->is_execute())
            m_true_branch = static_ptr_cast<AST::Execute>(true_branch)->command();
        else
            m_true_branch = move(true_branch);
    }

    if (m_false_branch) {
        auto false_branch = m_false_branch.release_nonnull();
        if (false_branch->is_execute())
            m_false_branch = static_ptr_cast<AST::Execute>(false_branch)->command();
        else
            m_false_branch = move(false_branch);
    }
}

IfCond::~IfCond()
{
}

void ImmediateExpression::dump(int level) const
{
    Node::dump(level);
    print_indented("(function)", level + 1);
    print_indented(m_function.name, level + 2);
    print_indented("(arguments)", level + 1);
    for (auto& argument : arguments())
        argument.dump(level + 2);
}

RefPtr<Value> ImmediateExpression::run(RefPtr<Shell> shell)
{
    auto node = shell->run_immediate_function(m_function.name, *this, arguments());
    if (node)
        return node->run(shell);

    return make_ref_counted<ListValue>({});
}

void ImmediateExpression::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    // '${' - FIXME: This could also be '$\\\n{'
    editor.stylize({ m_position.start_offset, m_position.start_offset + 2 }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });

    // Function name
    Line::Style function_style { Line::Style::Foreground(Line::Style::XtermColor::Red) };
    if (shell.has_immediate_function(function_name()))
        function_style = { Line::Style::Foreground(Line::Style::XtermColor::Green) };
    editor.stylize({ m_function.position.start_offset, m_function.position.end_offset }, move(function_style));

    // Arguments
    for (auto& argument : m_arguments) {
        metadata.is_first_in_list = false;
        argument.highlight_in_editor(editor, shell, metadata);
    }

    // Closing brace
    if (m_closing_brace_position.has_value())
        editor.stylize({ m_closing_brace_position->start_offset, m_closing_brace_position->end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });
}

Vector<Line::CompletionSuggestion> ImmediateExpression::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || matching_node != this)
        return {};

    auto corrected_offset = offset - m_function.position.start_offset;

    if (corrected_offset > m_function.name.length())
        return {};

    return shell.complete_immediate_function_name(m_function.name, corrected_offset);
}

HitTestResult ImmediateExpression::hit_test_position(size_t offset) const
{
    if (m_function.position.contains(offset))
        return { this, this, this };

    for (auto& argument : m_arguments) {
        if (auto result = argument.hit_test_position(offset); result.matching_node)
            return result;
    }

    return {};
}

ImmediateExpression::ImmediateExpression(Position position, NameWithPosition function, NonnullRefPtrVector<AST::Node> arguments, Optional<Position> closing_brace_position)
    : Node(move(position))
    , m_arguments(move(arguments))
    , m_function(move(function))
    , m_closing_brace_position(move(closing_brace_position))
{
    if (is_syntax_error())
        return;

    for (auto& argument : m_arguments) {
        if (argument.is_syntax_error()) {
            set_is_syntax_error(argument.syntax_error_node());
            return;
        }
    }
}

ImmediateExpression::~ImmediateExpression()
{
}

void Join::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> Join::run(RefPtr<Shell> shell)
{
    auto left = m_left->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (left.last().should_wait && !left.last().next_chain.is_empty()) {
        // Join (C0s*; C1) X -> (C0s*; Join C1 X)
        auto& lhs_node = left.last().next_chain.last().node;
        lhs_node = make_ref_counted<Join>(m_position, lhs_node, m_right);
        return make_ref_counted<CommandSequenceValue>(move(left));
    }

    auto right = m_right->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    return make_ref_counted<CommandSequenceValue>(join_commands(move(left), move(right)));
}

void Join::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_left->highlight_in_editor(editor, shell, metadata);
    if (m_left->is_list() || m_left->is_command())
        metadata.is_first_in_list = false;
    m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Join::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node)
        return result;

    return m_right->hit_test_position(offset);
}

RefPtr<Node> Join::leftmost_trivial_literal() const
{
    if (auto value = m_left->leftmost_trivial_literal())
        return value;
    return m_right->leftmost_trivial_literal();
}

Join::Join(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

Join::~Join()
{
}

void MatchExpr::dump(int level) const
{
    Node::dump(level);
    print_indented(String::formatted("(expression: {})", m_expr_name.characters()), level + 1);
    m_matched_expr->dump(level + 2);
    print_indented(String::formatted("(named: {})", m_expr_name.characters()), level + 1);
    print_indented("(entries)", level + 1);
    for (auto& entry : m_entries) {
        StringBuilder builder;
        builder.append("(match");
        if (entry.match_names.has_value()) {
            builder.append(" to names (");
            bool first = true;
            for (auto& name : entry.match_names.value()) {
                if (!first)
                    builder.append(' ');
                first = false;
                builder.append(name);
            }
            builder.append("))");

        } else {
            builder.append(')');
        }
        print_indented(builder.string_view(), level + 2);
        for (auto& node : entry.options)
            node.dump(level + 3);
        print_indented("(execute)", level + 2);
        if (entry.body)
            entry.body->dump(level + 3);
        else
            print_indented("(nothing)", level + 3);
    }
}

RefPtr<Value> MatchExpr::run(RefPtr<Shell> shell)
{
    auto value = m_matched_expr->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto list = value->resolve_as_list(shell);

    auto list_matches = [&](auto&& pattern, auto& spans) {
        if (pattern.size() != list.size())
            return false;

        for (size_t i = 0; i < pattern.size(); ++i) {
            Vector<AK::MaskSpan> mask_spans;
            if (!list[i].matches(pattern[i], mask_spans))
                return false;
            for (auto& span : mask_spans)
                spans.append(list[i].substring(span.start, span.length));
        }

        return true;
    };

    auto resolve_pattern = [&](auto& option) {
        Vector<String> pattern;
        if (option.is_glob()) {
            pattern.append(static_cast<const Glob*>(&option)->text());
        } else if (option.is_bareword()) {
            pattern.append(static_cast<const BarewordLiteral*>(&option)->text());
        } else {
            auto list = option.run(shell);
            if (shell && shell->has_any_error())
                return pattern;

            option.for_each_entry(shell, [&](auto&& value) {
                pattern.extend(value->resolve_as_list(nullptr)); // Note: 'nullptr' incurs special behavior,
                                                                 //       asking the node for a 'raw' value.
                return IterationDecision::Continue;
            });
        }

        return pattern;
    };

    auto frame = shell->push_frame(String::formatted("match ({})", this));
    if (!m_expr_name.is_empty())
        shell->set_local_variable(m_expr_name, value, true);

    for (auto& entry : m_entries) {
        for (auto& option : entry.options) {
            Vector<String> spans;
            if (list_matches(resolve_pattern(option), spans)) {
                if (entry.body) {
                    if (entry.match_names.has_value()) {
                        size_t i = 0;
                        for (auto& name : entry.match_names.value()) {
                            if (spans.size() > i)
                                shell->set_local_variable(name, make_ref_counted<AST::StringValue>(spans[i]), true);
                            ++i;
                        }
                    }
                    return entry.body->run(shell);
                } else {
                    return make_ref_counted<AST::ListValue>({});
                }
            }
        }
    }

    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Non-exhaustive match rules!", position());
    return make_ref_counted<AST::ListValue>({});
}

void MatchExpr::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.start_offset + 5 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (m_as_position.has_value())
        editor.stylize({ m_as_position.value().start_offset, m_as_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    metadata.is_first_in_list = false;
    m_matched_expr->highlight_in_editor(editor, shell, metadata);

    for (auto& entry : m_entries) {
        metadata.is_first_in_list = false;
        for (auto& option : entry.options)
            option.highlight_in_editor(editor, shell, metadata);

        metadata.is_first_in_list = true;
        if (entry.body)
            entry.body->highlight_in_editor(editor, shell, metadata);

        for (auto& position : entry.pipe_positions)
            editor.stylize({ position.start_offset, position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        if (entry.match_as_position.has_value())
            editor.stylize({ entry.match_as_position.value().start_offset, entry.match_as_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    }
}

HitTestResult MatchExpr::hit_test_position(size_t offset) const
{
    auto result = m_matched_expr->hit_test_position(offset);
    if (result.matching_node)
        return result;

    for (auto& entry : m_entries) {
        if (!entry.body)
            continue;
        auto result = entry.body->hit_test_position(offset);
        if (result.matching_node)
            return result;
    }

    return {};
}

MatchExpr::MatchExpr(Position position, NonnullRefPtr<Node> expr, String name, Optional<Position> as_position, Vector<MatchEntry> entries)
    : Node(move(position))
    , m_matched_expr(move(expr))
    , m_expr_name(move(name))
    , m_as_position(move(as_position))
    , m_entries(move(entries))
{
    if (m_matched_expr->is_syntax_error()) {
        set_is_syntax_error(m_matched_expr->syntax_error_node());
    } else {
        for (auto& entry : m_entries) {
            if (!entry.body)
                continue;
            if (entry.body->is_syntax_error()) {
                set_is_syntax_error(entry.body->syntax_error_node());
                break;
            }
        }
    }
}

MatchExpr::~MatchExpr()
{
}

void Or::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> Or::run(RefPtr<Shell> shell)
{
    auto commands = m_left->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    commands.last().next_chain.empend(*m_right, NodeWithAction::Or);
    return make_ref_counted<CommandSequenceValue>(move(commands));
}

void Or::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_left->highlight_in_editor(editor, shell, metadata);
    m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Or::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node) {
        if (!result.closest_command_node)
            result.closest_command_node = m_right;
        return result;
    }

    result = m_right->hit_test_position(offset);
    if (!result.closest_command_node)
        result.closest_command_node = m_right;
    return result;
}

Or::Or(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right, Position or_position)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
    , m_or_position(or_position)
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

Or::~Or()
{
}

void Pipe::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> Pipe::run(RefPtr<Shell> shell)
{
    auto left = m_left->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right = m_right->to_lazy_evaluated_commands(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto last_in_left = left.take_last();
    auto first_in_right = right.take_first();

    auto pipe_read_end = FdRedirection::create(-1, STDIN_FILENO, Rewiring::Close::Old);
    auto pipe_write_end = FdRedirection::create(-1, STDOUT_FILENO, pipe_read_end, Rewiring::Close::RefreshOld);

    auto insert_at_start_or_after_last_pipe = [&](auto& pipe, auto& command) {
        size_t insert_index = 0;
        auto& redirections = command.redirections;
        for (ssize_t i = redirections.size() - 1; i >= 0; --i) {
            auto& redirection = redirections[i];
            if (!redirection.is_fd_redirection())
                continue;
            auto& fd_redirection = static_cast<FdRedirection&>(redirection);
            if (fd_redirection.old_fd == -1) {
                insert_index = i;
                break;
            }
        }

        redirections.insert(insert_index, pipe);
    };

    insert_at_start_or_after_last_pipe(pipe_read_end, first_in_right);
    insert_at_start_or_after_last_pipe(pipe_write_end, last_in_left);

    last_in_left.should_wait = false;
    last_in_left.is_pipe_source = true;

    if (first_in_right.pipeline) {
        last_in_left.pipeline = first_in_right.pipeline;
    } else {
        auto pipeline = make_ref_counted<Pipeline>();
        last_in_left.pipeline = pipeline;
        first_in_right.pipeline = pipeline;
    }

    Vector<Command> commands;
    commands.extend(left);
    commands.append(last_in_left);
    commands.append(first_in_right);
    commands.extend(right);

    return make_ref_counted<CommandSequenceValue>(move(commands));
}

void Pipe::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_left->highlight_in_editor(editor, shell, metadata);
    m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Pipe::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node) {
        if (!result.closest_command_node)
            result.closest_command_node = m_right;
        return result;
    }

    result = m_right->hit_test_position(offset);
    if (!result.closest_command_node)
        result.closest_command_node = m_right;
    return result;
}

Pipe::Pipe(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

Pipe::~Pipe()
{
}

PathRedirectionNode::PathRedirectionNode(Position position, int fd, NonnullRefPtr<Node> path)
    : Node(move(position))
    , m_fd(fd)
    , m_path(move(path))
{
}

void PathRedirectionNode::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
    metadata.is_first_in_list = false;
    m_path->highlight_in_editor(editor, shell, metadata);
    if (m_path->is_bareword()) {
        auto path_text = m_path->run(nullptr)->resolve_as_list(nullptr);
        VERIFY(path_text.size() == 1);
        // Apply a URL to the path.
        auto& position = m_path->position();
        auto& path = path_text[0];
        if (!path.starts_with('/'))
            path = String::formatted("{}/{}", shell.cwd, path);
        auto url = URL::create_with_file_protocol(path);
        url.set_host(shell.hostname);
        editor.stylize({ position.start_offset, position.end_offset }, { Line::Style::Hyperlink(url.to_string()) });
    }
}

HitTestResult PathRedirectionNode::hit_test_position(size_t offset) const
{
    auto result = m_path->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    return result;
}

Vector<Line::CompletionSuggestion> PathRedirectionNode::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral*>(matching_node.ptr());

    if (corrected_offset > node->text().length())
        return {};

    return shell.complete_path("", node->text(), corrected_offset, Shell::ExecutableOnly::No);
}

PathRedirectionNode::~PathRedirectionNode()
{
}

void Range::dump(int level) const
{
    Node::dump(level);
    print_indented("(From)", level + 1);
    m_start->dump(level + 2);
    print_indented("(To)", level + 1);
    m_end->dump(level + 2);
}

RefPtr<Value> Range::run(RefPtr<Shell> shell)
{
    auto interpolate = [position = position()](RefPtr<Value> start, RefPtr<Value> end, RefPtr<Shell> shell) -> NonnullRefPtrVector<Value> {
        NonnullRefPtrVector<Value> values;

        if (start->is_string() && end->is_string()) {
            auto start_str = start->resolve_as_list(shell)[0];
            auto end_str = end->resolve_as_list(shell)[0];

            Utf8View start_view { start_str }, end_view { end_str };
            if (start_view.validate() && end_view.validate()) {
                if (start_view.length() == 1 && end_view.length() == 1) {
                    // Interpolate between two code points.
                    auto start_code_point = *start_view.begin();
                    auto end_code_point = *end_view.begin();
                    auto step = start_code_point > end_code_point ? -1 : 1;
                    StringBuilder builder;
                    for (u32 code_point = start_code_point; code_point != end_code_point; code_point += step) {
                        builder.clear();
                        builder.append_code_point(code_point);
                        values.append(make_ref_counted<StringValue>(builder.to_string()));
                    }
                    // Append the ending code point too, most shells treat this as inclusive.
                    builder.clear();
                    builder.append_code_point(end_code_point);
                    values.append(make_ref_counted<StringValue>(builder.to_string()));
                } else {
                    // Could be two numbers?
                    auto start_int = start_str.to_int();
                    auto end_int = end_str.to_int();
                    if (start_int.has_value() && end_int.has_value()) {
                        auto start = start_int.value();
                        auto end = end_int.value();
                        auto step = start > end ? -1 : 1;
                        for (int value = start; value != end; value += step)
                            values.append(make_ref_counted<StringValue>(String::number(value)));
                        // Append the range end too, most shells treat this as inclusive.
                        values.append(make_ref_counted<StringValue>(String::number(end)));
                    } else {
                        goto yield_start_end;
                    }
                }
            } else {
            yield_start_end:;
                shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, String::formatted("Cannot interpolate between '{}' and '{}'!", start_str, end_str), position);
                // We can't really interpolate between the two, so just yield both.
                values.append(make_ref_counted<StringValue>(move(start_str)));
                values.append(make_ref_counted<StringValue>(move(end_str)));
            }

            return values;
        }

        warnln("Shell: Cannot apply the requested interpolation");
        return values;
    };

    auto start_value = m_start->run(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto end_value = m_end->run(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (!start_value || !end_value)
        return make_ref_counted<ListValue>({});

    return make_ref_counted<ListValue>(interpolate(*start_value, *end_value, shell));
}

void Range::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_start->highlight_in_editor(editor, shell, metadata);

    // Highlight the '..'
    editor.stylize({ m_start->position().end_offset, m_end->position().start_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    metadata.is_first_in_list = false;
    m_end->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Range::hit_test_position(size_t offset) const
{
    auto result = m_start->hit_test_position(offset);
    if (result.matching_node) {
        if (!result.closest_command_node)
            result.closest_command_node = m_start;
        return result;
    }

    result = m_end->hit_test_position(offset);
    if (!result.closest_command_node)
        result.closest_command_node = m_end;
    return result;
}

Range::Range(Position position, NonnullRefPtr<Node> start, NonnullRefPtr<Node> end)
    : Node(move(position))
    , m_start(move(start))
    , m_end(move(end))
{
    if (m_start->is_syntax_error())
        set_is_syntax_error(m_start->syntax_error_node());
    else if (m_end->is_syntax_error())
        set_is_syntax_error(m_end->syntax_error_node());
}

Range::~Range()
{
}

void ReadRedirection::dump(int level) const
{
    Node::dump(level);
    m_path->dump(level + 1);
    print_indented(String::formatted("To {}", m_fd), level + 1);
}

RefPtr<Value> ReadRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = m_path->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(" ", path_segments);

    command.redirections.append(PathRedirection::create(builder.to_string(), m_fd, PathRedirection::Read));
    return make_ref_counted<CommandValue>(move(command));
}

ReadRedirection::ReadRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

ReadRedirection::~ReadRedirection()
{
}

void ReadWriteRedirection::dump(int level) const
{
    Node::dump(level);
    m_path->dump(level + 1);
    print_indented(String::formatted("To/From {}", m_fd), level + 1);
}

RefPtr<Value> ReadWriteRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = m_path->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(" ", path_segments);

    command.redirections.append(PathRedirection::create(builder.to_string(), m_fd, PathRedirection::ReadWrite));
    return make_ref_counted<CommandValue>(move(command));
}

ReadWriteRedirection::ReadWriteRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

ReadWriteRedirection::~ReadWriteRedirection()
{
}

void Sequence::dump(int level) const
{
    Node::dump(level);
    for (auto& entry : m_entries)
        entry.dump(level + 1);
}

RefPtr<Value> Sequence::run(RefPtr<Shell> shell)
{
    Vector<Command> all_commands;
    Command* last_command_in_sequence = nullptr;
    for (auto& entry : m_entries) {
        if (shell && shell->has_any_error())
            break;
        if (!last_command_in_sequence) {
            auto commands = entry.to_lazy_evaluated_commands(shell);
            all_commands.extend(move(commands));
            last_command_in_sequence = &all_commands.last();
            continue;
        }

        if (last_command_in_sequence->should_wait) {
            last_command_in_sequence->next_chain.append(NodeWithAction { entry, NodeWithAction::Sequence });
        } else {
            all_commands.extend(entry.to_lazy_evaluated_commands(shell));
            last_command_in_sequence = &all_commands.last();
        }
    }

    return make_ref_counted<CommandSequenceValue>(move(all_commands));
}

void Sequence::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    for (auto& entry : m_entries)
        entry.highlight_in_editor(editor, shell, metadata);
}

HitTestResult Sequence::hit_test_position(size_t offset) const
{
    for (auto& entry : m_entries) {
        auto result = entry.hit_test_position(offset);
        if (result.matching_node) {
            if (!result.closest_command_node)
                result.closest_command_node = entry;
            return result;
        }
    }

    return {};
}

Sequence::Sequence(Position position, NonnullRefPtrVector<Node> entries, Vector<Position> separator_positions)
    : Node(move(position))
    , m_entries(move(entries))
    , m_separator_positions(separator_positions)
{
    for (auto& entry : m_entries) {
        if (entry.is_syntax_error()) {
            set_is_syntax_error(entry.syntax_error_node());
            break;
        }
    }
}

Sequence::~Sequence()
{
}

void Subshell::dump(int level) const
{
    Node::dump(level);
    if (m_block)
        m_block->dump(level + 1);
}

RefPtr<Value> Subshell::run(RefPtr<Shell> shell)
{
    if (!m_block)
        return make_ref_counted<ListValue>({});

    return make_ref_counted<AST::CommandSequenceValue>(m_block->to_lazy_evaluated_commands(shell));
}

void Subshell::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;
    if (m_block)
        m_block->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Subshell::hit_test_position(size_t offset) const
{
    if (m_block)
        return m_block->hit_test_position(offset);

    return {};
}

Subshell::Subshell(Position position, RefPtr<Node> block)
    : Node(move(position))
    , m_block(block)
{
    if (m_block && m_block->is_syntax_error())
        set_is_syntax_error(m_block->syntax_error_node());
}

Subshell::~Subshell()
{
}

void Slice::dump(int level) const
{
    Node::dump(level);
    m_selector->dump(level + 1);
}

RefPtr<Value> Slice::run(RefPtr<Shell> shell)
{
    return m_selector->run(shell);
}

void Slice::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_selector->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Slice::hit_test_position(size_t offset) const
{
    return m_selector->hit_test_position(offset);
}

Vector<Line::CompletionSuggestion> Slice::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    // TODO: Maybe intercept this, and suggest values in range?
    return m_selector->complete_for_editor(shell, offset, hit_test_result);
}

Slice::Slice(Position position, NonnullRefPtr<AST::Node> selector)
    : Node(move(position))
    , m_selector(move(selector))
{
    if (m_selector->is_syntax_error())
        set_is_syntax_error(m_selector->syntax_error_node());
}

Slice::~Slice()
{
}

void SimpleVariable::dump(int level) const
{
    Node::dump(level);
    print_indented("(Name)", level + 1);
    print_indented(m_name, level + 2);
    print_indented("(Slice)", level + 1);
    if (m_slice)
        m_slice->dump(level + 2);
    else
        print_indented("(None)", level + 2);
}

RefPtr<Value> SimpleVariable::run(RefPtr<Shell>)
{
    NonnullRefPtr<Value> value = make_ref_counted<SimpleVariableValue>(m_name);
    if (m_slice)
        value = value->with_slices(*m_slice);
    return value;
}

void SimpleVariable::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(214, 112, 214) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
    if (m_slice)
        m_slice->highlight_in_editor(editor, shell, metadata);
}

HitTestResult SimpleVariable::hit_test_position(size_t offset) const
{
    if (!position().contains(offset))
        return {};

    if (m_slice && m_slice->position().contains(offset))
        return m_slice->hit_test_position(offset);

    return { this, this, nullptr };
}

Vector<Line::CompletionSuggestion> SimpleVariable::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return {};

    if (matching_node != this)
        return {};

    auto corrected_offset = offset - matching_node->position().start_offset - 1;

    if (corrected_offset > m_name.length() + 1)
        return {};

    return shell.complete_variable(m_name, corrected_offset);
}

SimpleVariable::SimpleVariable(Position position, String name)
    : VariableNode(move(position))
    , m_name(move(name))
{
}

SimpleVariable::~SimpleVariable()
{
}

void SpecialVariable::dump(int level) const
{
    Node::dump(level);
    print_indented("(Name)", level + 1);
    print_indented(String { &m_name, 1 }, level + 1);
    print_indented("(Slice)", level + 1);
    if (m_slice)
        m_slice->dump(level + 2);
    else
        print_indented("(None)", level + 2);
}

RefPtr<Value> SpecialVariable::run(RefPtr<Shell>)
{
    NonnullRefPtr<Value> value = make_ref_counted<SpecialVariableValue>(m_name);
    if (m_slice)
        value = value->with_slices(*m_slice);
    return value;
}

void SpecialVariable::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(214, 112, 214) });
    if (m_slice)
        m_slice->highlight_in_editor(editor, shell, metadata);
}

Vector<Line::CompletionSuggestion> SpecialVariable::complete_for_editor(Shell&, size_t, const HitTestResult&)
{
    return {};
}

HitTestResult SpecialVariable::hit_test_position(size_t offset) const
{
    if (m_slice && m_slice->position().contains(offset))
        return m_slice->hit_test_position(offset);

    return { this, this, nullptr };
}

SpecialVariable::SpecialVariable(Position position, char name)
    : VariableNode(move(position))
    , m_name(name)
{
}

SpecialVariable::~SpecialVariable()
{
}

void Juxtaposition::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> Juxtaposition::run(RefPtr<Shell> shell)
{
    auto left_value = m_left->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right_value = m_right->run(shell)->resolve_without_cast(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto left = left_value->resolve_as_list(shell);
    auto right = right_value->resolve_as_list(shell);

    if (left_value->is_string() && right_value->is_string()) {

        VERIFY(left.size() == 1);
        VERIFY(right.size() == 1);

        StringBuilder builder;
        builder.append(left[0]);
        builder.append(right[0]);

        return make_ref_counted<StringValue>(builder.to_string());
    }

    // Otherwise, treat them as lists and create a list product.
    if (left.is_empty() || right.is_empty())
        return make_ref_counted<ListValue>({});

    Vector<String> result;
    result.ensure_capacity(left.size() * right.size());

    StringBuilder builder;
    for (auto& left_element : left) {
        for (auto& right_element : right) {
            builder.append(left_element);
            builder.append(right_element);
            result.append(builder.to_string());
            builder.clear();
        }
    }

    return make_ref_counted<ListValue>(move(result));
}

void Juxtaposition::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_left->highlight_in_editor(editor, shell, metadata);

    // '~/foo/bar' is special, we have to actually resolve the tilde
    // since that resolution is a pure operation, we can just go ahead
    // and do it to get the value :)
    if (m_right->is_bareword() && m_left->is_tilde()) {
        auto tilde_value = m_left->run(shell)->resolve_as_list(shell)[0];
        auto bareword_value = m_right->run(shell)->resolve_as_list(shell)[0];

        StringBuilder path_builder;
        path_builder.append(tilde_value);
        path_builder.append("/");
        path_builder.append(bareword_value);
        auto path = path_builder.to_string();

        if (Core::File::exists(path)) {
            auto realpath = shell.resolve_path(path);
            auto url = URL::create_with_file_protocol(realpath);
            url.set_host(shell.hostname);
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Hyperlink(url.to_string()) });
        }

    } else {
        m_right->highlight_in_editor(editor, shell, metadata);
    }
}

Vector<Line::CompletionSuggestion> Juxtaposition::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (m_left->would_execute() || m_right->would_execute()) {
        return {};
    }

    // '~/foo/bar' is special, we have to actually resolve the tilde
    // then complete the bareword with that path prefix.
    auto left_values = m_left->run(shell)->resolve_as_list(shell);

    if (left_values.is_empty())
        return m_right->complete_for_editor(shell, offset, hit_test_result);

    auto& left_value = left_values.first();

    auto right_values = m_right->run(shell)->resolve_as_list(shell);
    StringView right_value {};

    auto corrected_offset = offset - matching_node->position().start_offset;

    if (!right_values.is_empty())
        right_value = right_values.first();

    if (m_left->is_tilde() && !right_value.is_empty()) {
        right_value = right_value.substring_view(1);
        corrected_offset--;
    }

    if (corrected_offset > right_value.length())
        return {};

    return shell.complete_path(left_value, right_value, corrected_offset, Shell::ExecutableOnly::No);
}

HitTestResult Juxtaposition::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    if (result.matching_node)
        return result;

    result = m_right->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    return result;
}

Juxtaposition::Juxtaposition(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

Juxtaposition::~Juxtaposition()
{
}

void StringLiteral::dump(int level) const
{
    Node::dump(level);
    print_indented(m_text, level + 1);
}

RefPtr<Value> StringLiteral::run(RefPtr<Shell>)
{
    return make_ref_counted<StringValue>(m_text);
}

void StringLiteral::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    if (m_text.is_empty())
        return;

    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
}

StringLiteral::StringLiteral(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

StringLiteral::~StringLiteral()
{
}

void StringPartCompose::dump(int level) const
{
    Node::dump(level);
    m_left->dump(level + 1);
    m_right->dump(level + 1);
}

RefPtr<Value> StringPartCompose::run(RefPtr<Shell> shell)
{
    auto left = m_left->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right = m_right->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(" ", left);
    builder.join(" ", right);

    return make_ref_counted<StringValue>(builder.to_string());
}

void StringPartCompose::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    m_left->highlight_in_editor(editor, shell, metadata);
    m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult StringPartCompose::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node)
        return result;
    return m_right->hit_test_position(offset);
}

StringPartCompose::StringPartCompose(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

StringPartCompose::~StringPartCompose()
{
}

void SyntaxError::dump(int level) const
{
    Node::dump(level);
    print_indented("(Error text)", level + 1);
    print_indented(m_syntax_error_text, level + 2);
    print_indented("(Can be recovered from)", level + 1);
    print_indented(String::formatted("{}", m_is_continuable), level + 2);
}

RefPtr<Value> SyntaxError::run(RefPtr<Shell> shell)
{
    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, m_syntax_error_text, position());
    return make_ref_counted<StringValue>("");
}

void SyntaxError::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Bold });
}

SyntaxError::SyntaxError(Position position, String error, bool is_continuable)
    : Node(move(position))
    , m_syntax_error_text(move(error))
    , m_is_continuable(is_continuable)
{
}

const SyntaxError& SyntaxError::syntax_error_node() const
{
    return *this;
}

SyntaxError::~SyntaxError()
{
}

void SyntheticNode::dump(int level) const
{
    Node::dump(level);
}

RefPtr<Value> SyntheticNode::run(RefPtr<Shell>)
{
    return m_value;
}

void SyntheticNode::highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata)
{
}

SyntheticNode::SyntheticNode(Position position, NonnullRefPtr<Value> value)
    : Node(move(position))
    , m_value(move(value))
{
}

void Tilde::dump(int level) const
{
    Node::dump(level);
    print_indented(m_username, level + 1);
}

RefPtr<Value> Tilde::run(RefPtr<Shell>)
{
    return make_ref_counted<TildeValue>(m_username);
}

void Tilde::highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata)
{
}

HitTestResult Tilde::hit_test_position(size_t offset) const
{
    if (!position().contains(offset))
        return {};

    return { this, this, nullptr };
}

Vector<Line::CompletionSuggestion> Tilde::complete_for_editor(Shell& shell, size_t offset, const HitTestResult& hit_test_result)
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return {};

    if (matching_node != this)
        return {};

    auto corrected_offset = offset - matching_node->position().start_offset - 1;

    if (corrected_offset > m_username.length() + 1)
        return {};

    return shell.complete_user(m_username, corrected_offset);
}

String Tilde::text() const
{
    StringBuilder builder;
    builder.append('~');
    builder.append(m_username);
    return builder.to_string();
}

Tilde::Tilde(Position position, String username)
    : Node(move(position))
    , m_username(move(username))
{
}

Tilde::~Tilde()
{
}

void WriteAppendRedirection::dump(int level) const
{
    Node::dump(level);
    m_path->dump(level + 1);
    print_indented(String::formatted("From {}", m_fd), level + 1);
}

RefPtr<Value> WriteAppendRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = m_path->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(" ", path_segments);

    command.redirections.append(PathRedirection::create(builder.to_string(), m_fd, PathRedirection::WriteAppend));
    return make_ref_counted<CommandValue>(move(command));
}

WriteAppendRedirection::WriteAppendRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

WriteAppendRedirection::~WriteAppendRedirection()
{
}

void WriteRedirection::dump(int level) const
{
    Node::dump(level);
    m_path->dump(level + 1);
    print_indented(String::formatted("From {}", m_fd), level + 1);
}

RefPtr<Value> WriteRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = m_path->run(shell)->resolve_as_list(shell);
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(" ", path_segments);

    command.redirections.append(PathRedirection::create(builder.to_string(), m_fd, PathRedirection::Write));
    return make_ref_counted<CommandValue>(move(command));
}

WriteRedirection::WriteRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

WriteRedirection::~WriteRedirection()
{
}

void VariableDeclarations::dump(int level) const
{
    Node::dump(level);
    for (auto& var : m_variables) {
        print_indented("Set", level + 1);
        var.name->dump(level + 2);
        var.value->dump(level + 2);
    }
}

RefPtr<Value> VariableDeclarations::run(RefPtr<Shell> shell)
{
    for (auto& var : m_variables) {
        auto name_value = var.name->run(shell)->resolve_as_list(shell);
        if (shell && shell->has_any_error())
            break;

        VERIFY(name_value.size() == 1);
        auto name = name_value[0];
        auto value = var.value->run(shell);
        if (shell && shell->has_any_error())
            break;

        shell->set_local_variable(name, value.release_nonnull());
    }

    return make_ref_counted<ListValue>({});
}

void VariableDeclarations::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = false;
    for (auto& var : m_variables) {
        var.name->highlight_in_editor(editor, shell, metadata);
        // Highlight the '='.
        editor.stylize({ var.name->position().end_offset - 1, var.name->position().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue) });
        var.value->highlight_in_editor(editor, shell, metadata);
    }
}

HitTestResult VariableDeclarations::hit_test_position(size_t offset) const
{
    for (auto decl : m_variables) {
        auto result = decl.value->hit_test_position(offset);
        if (result.matching_node)
            return result;
    }

    return { nullptr, nullptr, nullptr };
}

VariableDeclarations::VariableDeclarations(Position position, Vector<Variable> variables)
    : Node(move(position))
    , m_variables(move(variables))
{
    for (auto& decl : m_variables) {
        if (decl.name->is_syntax_error()) {
            set_is_syntax_error(decl.name->syntax_error_node());
            break;
        }
        if (decl.value->is_syntax_error()) {
            set_is_syntax_error(decl.value->syntax_error_node());
            break;
        }
    }
}

VariableDeclarations::~VariableDeclarations()
{
}

Value::~Value()
{
}
Vector<AST::Command> Value::resolve_as_commands(RefPtr<Shell> shell)
{
    Command command;
    command.argv = resolve_as_list(shell);
    return { command };
}

ListValue::ListValue(Vector<String> values)
{
    if (values.is_empty())
        return;
    m_contained_values.ensure_capacity(values.size());
    for (auto& str : values)
        m_contained_values.append(adopt_ref(*new StringValue(move(str))));
}

NonnullRefPtr<Value> Value::with_slices(NonnullRefPtr<Slice> slice) const&
{
    auto value = clone();
    value->m_slices.append(move(slice));
    return value;
}

NonnullRefPtr<Value> Value::with_slices(NonnullRefPtrVector<Slice> slices) const&
{
    auto value = clone();
    value->m_slices.extend(move(slices));
    return value;
}

ListValue::~ListValue()
{
}

Vector<String> ListValue::resolve_as_list(RefPtr<Shell> shell)
{
    Vector<String> values;
    for (auto& value : m_contained_values)
        values.extend(value.resolve_as_list(shell));

    return resolve_slices(shell, move(values), m_slices);
}

NonnullRefPtr<Value> ListValue::resolve_without_cast(RefPtr<Shell> shell)
{
    NonnullRefPtrVector<Value> values;
    for (auto& value : m_contained_values)
        values.append(value.resolve_without_cast(shell));

    NonnullRefPtr<Value> value = make_ref_counted<ListValue>(move(values));
    if (!m_slices.is_empty())
        value = value->with_slices(m_slices);
    return value;
}

CommandValue::~CommandValue()
{
}

CommandSequenceValue::~CommandSequenceValue()
{
}

Vector<String> CommandSequenceValue::resolve_as_list(RefPtr<Shell> shell)
{
    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Unexpected cast of a command sequence to a list");
    return {};
}

Vector<Command> CommandSequenceValue::resolve_as_commands(RefPtr<Shell>)
{
    return m_contained_values;
}

Vector<String> CommandValue::resolve_as_list(RefPtr<Shell> shell)
{
    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Unexpected cast of a command to a list");
    return {};
}

Vector<Command> CommandValue::resolve_as_commands(RefPtr<Shell>)
{
    return { m_command };
}

JobValue::~JobValue()
{
}

StringValue::~StringValue()
{
}
Vector<String> StringValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (is_list()) {
        auto parts = StringView(m_string).split_view(m_split, m_keep_empty);
        Vector<String> result;
        result.ensure_capacity(parts.size());
        for (auto& part : parts)
            result.append(part);
        return resolve_slices(shell, move(result), m_slices);
    }

    return { resolve_slices(shell, String { m_string }, m_slices) };
}

NonnullRefPtr<Value> StringValue::resolve_without_cast(RefPtr<Shell> shell)
{
    if (is_list())
        return make_ref_counted<AST::ListValue>(resolve_as_list(shell)); // No need to reapply the slices.

    return *this;
}

GlobValue::~GlobValue()
{
}
Vector<String> GlobValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return { resolve_slices(shell, String { m_glob }, m_slices) };

    auto results = shell->expand_globs(m_glob, shell->cwd);
    if (results.is_empty())
        shell->raise_error(Shell::ShellError::InvalidGlobError, "Glob did not match anything!", m_generation_position);
    return resolve_slices(shell, move(results), m_slices);
}

SimpleVariableValue::~SimpleVariableValue()
{
}
Vector<String> SimpleVariableValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return resolve_slices(shell, Vector<String> {}, m_slices);

    if (auto value = resolve_without_cast(shell); value != this)
        return value->resolve_as_list(shell);

    char* env_value = getenv(m_name.characters());
    if (env_value == nullptr)
        return { resolve_slices(shell, "", m_slices) };

    return { resolve_slices(shell, String { env_value }, m_slices) };
}

NonnullRefPtr<Value> SimpleVariableValue::resolve_without_cast(RefPtr<Shell> shell)
{
    VERIFY(shell);

    if (auto value = shell->lookup_local_variable(m_name)) {
        auto result = value.release_nonnull();
        // If a slice is applied, add it.
        if (!m_slices.is_empty())
            result = result->with_slices(m_slices);

        return result;
    }

    return *this;
}

SpecialVariableValue::~SpecialVariableValue()
{
}

Vector<String> SpecialVariableValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return {};

    switch (m_name) {
    case '?':
        return { resolve_slices(shell, String::number(shell->last_return_code.value_or(0)), m_slices) };
    case '$':
        return { resolve_slices(shell, String::number(getpid()), m_slices) };
    case '*':
        if (auto argv = shell->lookup_local_variable("ARGV"))
            return resolve_slices(shell, argv->resolve_as_list(shell), m_slices);
        return resolve_slices(shell, Vector<String> {}, m_slices);
    case '#':
        if (auto argv = shell->lookup_local_variable("ARGV")) {
            if (argv->is_list()) {
                auto list_argv = static_cast<AST::ListValue*>(argv.ptr());
                return { resolve_slices(shell, String::number(list_argv->values().size()), m_slices) };
            }
            return { resolve_slices(shell, "1", m_slices) };
        }
        return { resolve_slices(shell, "0", m_slices) };
    default:
        return { resolve_slices(shell, "", m_slices) };
    }
}

TildeValue::~TildeValue()
{
}
Vector<String> TildeValue::resolve_as_list(RefPtr<Shell> shell)
{
    StringBuilder builder;
    builder.append("~");
    builder.append(m_username);

    if (!shell)
        return { resolve_slices(shell, builder.to_string(), m_slices) };

    return { resolve_slices(shell, shell->expand_tilde(builder.to_string()), m_slices) };
}

ErrorOr<NonnullRefPtr<Rewiring>> CloseRedirection::apply() const
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Rewiring(fd, fd, Rewiring::Close::ImmediatelyCloseNew));
}

CloseRedirection::~CloseRedirection()
{
}

ErrorOr<NonnullRefPtr<Rewiring>> PathRedirection::apply() const
{
    auto check_fd_and_return = [my_fd = this->fd](int fd, String const& path) -> ErrorOr<NonnullRefPtr<Rewiring>> {
        if (fd < 0) {
            auto error = Error::from_errno(errno);
            dbgln("open() failed for '{}' with {}", path, error);
            return error;
        }
        return adopt_nonnull_ref_or_enomem(new (nothrow) Rewiring(fd, my_fd, Rewiring::Close::Old));
    };
    switch (direction) {
    case AST::PathRedirection::WriteAppend:
        return check_fd_and_return(open(path.characters(), O_WRONLY | O_CREAT | O_APPEND, 0666), path);

    case AST::PathRedirection::Write:
        return check_fd_and_return(open(path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666), path);

    case AST::PathRedirection::Read:
        return check_fd_and_return(open(path.characters(), O_RDONLY), path);

    case AST::PathRedirection::ReadWrite:
        return check_fd_and_return(open(path.characters(), O_RDWR | O_CREAT, 0666), path);
    }

    VERIFY_NOT_REACHED();
}

PathRedirection::~PathRedirection()
{
}

FdRedirection::~FdRedirection()
{
}

Redirection::~Redirection()
{
}

}
