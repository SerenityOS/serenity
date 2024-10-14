/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AST.h"
#include "Highlight.h"
#include "Shell.h"
#include <AK/Find.h>
#include <AK/MemoryStream.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/EventLoop.h>
#include <LibFileSystem/FileSystem.h>
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
        TRY(builder.put_literal("(ShellInternal)"sv));
    } else {
        bool first = true;
        for (auto& arg : value.argv) {
            if (!first)
                TRY(builder.put_literal(" "sv));
            first = false;
            TRY(builder.put_literal(arg));
        }
    }

    for (auto& redir : value.redirections) {
        TRY(builder.put_padding(' ', 1));
        if (redir->is_path_redirection()) {
            auto path_redir = static_cast<Shell::AST::PathRedirection const*>(redir.ptr());
            TRY(builder.put_i64(path_redir->fd));
            switch (path_redir->direction) {
            case Shell::AST::PathRedirection::Read:
                TRY(builder.put_literal("<"sv));
                break;
            case Shell::AST::PathRedirection::Write:
                TRY(builder.put_literal(">"sv));
                break;
            case Shell::AST::PathRedirection::WriteAppend:
                TRY(builder.put_literal(">>"sv));
                break;
            case Shell::AST::PathRedirection::ReadWrite:
                TRY(builder.put_literal("<>"sv));
                break;
            }
            TRY(builder.put_literal(path_redir->path));
        } else if (redir->is_fd_redirection()) {
            auto* fdredir = static_cast<Shell::AST::FdRedirection const*>(redir.ptr());
            TRY(builder.put_i64(fdredir->new_fd));
            TRY(builder.put_literal(">"sv));
            TRY(builder.put_i64(fdredir->old_fd));
        } else if (redir->is_close_redirection()) {
            auto close_redir = static_cast<Shell::AST::CloseRedirection const*>(redir.ptr());
            TRY(builder.put_i64(close_redir->fd));
            TRY(builder.put_literal(">&-"sv));
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    if (!value.next_chain.is_empty()) {
        for (auto& command : value.next_chain) {
            switch (command.action) {
            case Shell::AST::NodeWithAction::And:
                TRY(builder.put_literal(" && "sv));
                break;
            case Shell::AST::NodeWithAction::Or:
                TRY(builder.put_literal(" || "sv));
                break;
            case Shell::AST::NodeWithAction::Sequence:
                TRY(builder.put_literal("; "sv));
                break;
            }

            TRY(builder.put_literal("("sv));
            TRY(builder.put_literal(command.node->class_name()));
            TRY(builder.put_literal("...)"sv));
        }
    }
    if (!value.should_wait)
        TRY(builder.put_literal("&"sv));
    return {};
}

namespace Shell::AST {

template<typename... Args>
static inline void print_indented(int indent, CheckedFormatString<Args...> format, Args&&... args)
{
    auto str = ByteString::formatted(format.view(), forward<Args>(args)...);
    dbgln("{: >{}}", str, str.length() + indent * 2);
}

static inline Optional<Position> merge_positions(Optional<Position> const& left, Optional<Position> const& right)
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

static ErrorOr<String> resolve_slices(RefPtr<Shell> shell, String&& input_value, Vector<NonnullRefPtr<Slice>> slices)
{
    if (slices.is_empty())
        return move(input_value);

    for (auto& slice : slices) {
        auto value = TRY(slice->run(shell));
        if (shell && shell->has_any_error())
            break;

        if (!value) {
            shell->raise_error(Shell::ShellError::InvalidSliceContentsError, "Invalid slice contents", slice->position());
            return move(input_value);
        }

        auto index_values = TRY(value->resolve_as_list(shell));
        Vector<size_t> indices;
        indices.ensure_capacity(index_values.size());

        size_t i = 0;
        for (auto& value : index_values) {
            auto maybe_index = value.bytes_as_string_view().to_number<int>();
            if (!maybe_index.has_value()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, ByteString::formatted("Invalid value in slice index {}: {} (expected a number)", i, value), slice->position());
                return move(input_value);
            }
            ++i;

            auto index = maybe_index.value();
            auto original_index = index;
            if (index < 0)
                index += input_value.bytes_as_string_view().length();

            if (index < 0 || (size_t)index >= input_value.bytes_as_string_view().length()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, ByteString::formatted("Slice index {} (evaluated as {}) out of value bounds [0-{})", index, original_index, input_value.bytes_as_string_view().length()), slice->position());
                return move(input_value);
            }
            indices.unchecked_append(index);
        }

        StringBuilder builder { indices.size() };
        for (auto& index : indices)
            builder.append(input_value.bytes_as_string_view()[index]);

        input_value = TRY(builder.to_string());
    }

    return move(input_value);
}

static ErrorOr<Vector<String>> resolve_slices(RefPtr<Shell> shell, Vector<String>&& values, Vector<NonnullRefPtr<Slice>> slices)
{
    if (slices.is_empty())
        return move(values);

    for (auto& slice : slices) {
        auto value = TRY(slice->run(shell));
        if (shell && shell->has_any_error())
            break;

        if (!value) {
            shell->raise_error(Shell::ShellError::InvalidSliceContentsError, "Invalid slice contents", slice->position());
            return move(values);
        }

        auto index_values = TRY(value->resolve_as_list(shell));
        Vector<size_t> indices;
        indices.ensure_capacity(index_values.size());

        size_t i = 0;
        for (auto& value : index_values) {
            auto maybe_index = value.to_number<int>();
            if (!maybe_index.has_value()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, ByteString::formatted("Invalid value in slice index {}: {} (expected a number)", i, value), slice->position());
                return move(values);
            }
            ++i;

            auto index = maybe_index.value();
            auto original_index = index;
            if (index < 0)
                index += values.size();

            if (index < 0 || (size_t)index >= values.size()) {
                shell->raise_error(Shell::ShellError::InvalidSliceContentsError, ByteString::formatted("Slice index {} (evaluated as {}) out of value bounds [0-{})", index, original_index, values.size()), slice->position());
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

void Node::set_is_syntax_error(SyntaxError& error_node)
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

ErrorOr<void> Node::for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback)
{
    auto value = TRY(TRY(run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return {};

    if (value->is_job()) {
        TRY(callback(value));
        return {};
    }

    if (value->is_list_without_resolution()) {
        auto list = TRY(value->resolve_without_cast(shell));
        for (auto& element : static_cast<ListValue*>(list.ptr())->values()) {
            if (TRY(callback(element)) == IterationDecision::Break)
                break;
        }
        return {};
    }

    auto list = TRY(value->resolve_as_list(shell));
    for (auto& element : list) {
        if (TRY(callback(make_ref_counted<StringValue>(move(element)))) == IterationDecision::Break)
            break;
    }

    return {};
}

ErrorOr<Vector<Command>> Node::to_lazy_evaluated_commands(RefPtr<Shell> shell)
{
    if (would_execute()) {
        // Wrap the node in a "should immediately execute next" command.
        return Vector {
            Command { {}, {}, true, false, true, true, {}, { NodeWithAction(*this, NodeWithAction::Sequence) }, position() }
        };
    }

    return TRY(TRY(run(shell))->resolve_as_commands(shell));
}

ErrorOr<void> Node::dump(int level) const
{
    print_indented(level,
        "{} at {}:{} (from {}.{} to {}.{})",
        class_name(),
        m_position.start_offset,
        m_position.end_offset,
        m_position.start_line.line_number,
        m_position.start_line.line_column,
        m_position.end_line.line_number,
        m_position.end_line.line_column);

    return {};
}

Node::Node(Position position)
    : m_position(position)
{
}

ErrorOr<Vector<Line::CompletionSuggestion>> Node::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (matching_node) {
        auto kind = matching_node->kind();
        StringLiteral::EnclosureType enclosure_type = StringLiteral::EnclosureType::None;
        if (kind == Kind::StringLiteral)
            enclosure_type = static_cast<StringLiteral const*>(matching_node.ptr())->enclosure_type();

        auto set_results_trivia = [enclosure_type](Vector<Line::CompletionSuggestion>&& suggestions) {
            if (enclosure_type != StringLiteral::EnclosureType::None) {
                for (auto& entry : suggestions)
                    entry.trailing_trivia = String::from_code_point(static_cast<u32>(enclosure_type == StringLiteral::EnclosureType::SingleQuotes ? '\'' : '"'));
            }
            return suggestions;
        };
        if (kind == Kind::BarewordLiteral || kind == Kind::StringLiteral) {
            Shell::EscapeMode escape_mode;
            StringView text;
            size_t corrected_offset;
            if (kind == Kind::BarewordLiteral) {
                auto* node = static_cast<BarewordLiteral const*>(matching_node.ptr());
                text = node->text();
                escape_mode = Shell::EscapeMode::Bareword;
                corrected_offset = find_offset_into_node(text, offset - matching_node->position().start_offset, escape_mode);
            } else {
                auto* node = static_cast<StringLiteral const*>(matching_node.ptr());
                text = node->text();
                escape_mode = enclosure_type == StringLiteral::EnclosureType::SingleQuotes ? Shell::EscapeMode::SingleQuotedString : Shell::EscapeMode::DoubleQuotedString;
                corrected_offset = find_offset_into_node(text, offset - matching_node->position().start_offset + 1, escape_mode);
            }

            if (corrected_offset > text.length())
                return Vector<Line::CompletionSuggestion> {};

            // If the literal isn't an option, treat it as a path.
            if (!(text.starts_with('-') || text == "--" || text == "-"))
                return set_results_trivia(shell.complete_path(""sv, text, corrected_offset, Shell::ExecutableOnly::No, hit_test_result.closest_command_node.ptr(), hit_test_result.matching_node, escape_mode));

            // If the literal is an option, we have to know the program name
            // should we have no way to get that, bail early.

            if (!hit_test_result.closest_command_node)
                return Vector<Line::CompletionSuggestion> {};

            auto program_name_node = hit_test_result.closest_command_node->leftmost_trivial_literal();
            if (!program_name_node)
                return Vector<Line::CompletionSuggestion> {};

            String program_name;
            if (program_name_node->is_bareword())
                program_name = static_cast<BarewordLiteral const*>(program_name_node.ptr())->text();
            else
                program_name = static_cast<StringLiteral const*>(program_name_node.ptr())->text();

            return set_results_trivia(shell.complete_option(program_name, text, corrected_offset, hit_test_result.closest_command_node.ptr(), hit_test_result.matching_node));
        }
        return Vector<Line::CompletionSuggestion> {};
    }
    auto result = hit_test_position(offset);
    if (!result.matching_node)
        return shell.complete_path(""sv, ""sv, 0, Shell::ExecutableOnly::No, result.closest_command_node.ptr(), nullptr, Shell::EscapeMode::Bareword);

    auto node = result.matching_node;
    if (node->is_bareword() || node != result.closest_node_with_semantic_meaning)
        node = result.closest_node_with_semantic_meaning;

    if (!node)
        return Vector<Line::CompletionSuggestion> {};

    return node->complete_for_editor(shell, offset, result);
}

ErrorOr<Vector<Line::CompletionSuggestion>> Node::complete_for_editor(Shell& shell, size_t offset)
{
    return Node::complete_for_editor(shell, offset, { nullptr, nullptr, nullptr });
}

ErrorOr<void> And::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> And::run(RefPtr<Shell> shell)
{
    auto commands = TRY(m_left->to_lazy_evaluated_commands(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    commands.last().next_chain.append(NodeWithAction { *m_right, NodeWithAction::And });
    return make_ref_counted<CommandSequenceValue>(move(commands));
}

ErrorOr<void> And::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;
    TRY(m_left->highlight_in_editor(editor, shell, metadata));
    TRY(m_right->highlight_in_editor(editor, shell, metadata));
    return {};
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

ErrorOr<void> ListConcatenate::dump(int level) const
{
    TRY(Node::dump(level));
    for (auto& element : m_list)
        TRY(element->dump(level + 1));

    return {};
}

ErrorOr<RefPtr<Value>> ListConcatenate::run(RefPtr<Shell> shell)
{
    RefPtr<Value> result = nullptr;

    for (auto& element : m_list) {
        if (shell && shell->has_any_error())
            break;
        if (!result) {
            result = make_ref_counted<ListValue>({ TRY(TRY(element->run(shell))->resolve_without_cast(shell)) });
            continue;
        }
        auto element_value = TRY(TRY(element->run(shell))->resolve_without_cast(shell));
        if (shell && shell->has_any_error())
            break;

        if (result->is_command() || element_value->is_command()) {
            auto joined_commands = join_commands(
                TRY(result->resolve_as_commands(shell)),
                TRY(element_value->resolve_as_commands(shell)));

            if (joined_commands.size() == 1) {
                auto& command = joined_commands[0];
                command.position = position();
                result = make_ref_counted<CommandValue>(command);
            } else {
                result = make_ref_counted<CommandSequenceValue>(move(joined_commands));
            }
        } else {
            Vector<NonnullRefPtr<Value>> values;

            if (result->is_list_without_resolution()) {
                values.extend(static_cast<ListValue*>(result.ptr())->values());
            } else {
                for (auto& result : TRY(result->resolve_as_list(shell)))
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

ErrorOr<void> ListConcatenate::for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback)
{
    for (auto& entry : m_list) {
        auto value = TRY(entry->run(shell));
        if (shell && shell->has_any_error())
            break;
        if (!value)
            continue;
        if (TRY(callback(value.release_nonnull())) == IterationDecision::Break)
            break;
    }

    return {};
}

ErrorOr<void> ListConcatenate::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    auto first = metadata.is_first_in_list;
    metadata.is_first_in_list = false;

    metadata.is_first_in_list = first;
    for (auto& element : m_list) {
        TRY(element->highlight_in_editor(editor, shell, metadata));
        metadata.is_first_in_list = false;
    }

    return {};
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

RefPtr<Node const> ListConcatenate::leftmost_trivial_literal() const
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

ErrorOr<void> Background::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_command->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Background::run(RefPtr<Shell> shell)
{
    auto commands = TRY(m_command->to_lazy_evaluated_commands(shell));
    for (auto& command : commands)
        command.should_wait = false;

    return make_ref_counted<CommandSequenceValue>(move(commands));
}

ErrorOr<void> Background::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    return m_command->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> BarewordLiteral::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_text);
    return {};
}

ErrorOr<RefPtr<Value>> BarewordLiteral::run(RefPtr<Shell>)
{
    return make_ref_counted<StringValue>(m_text);
}

ErrorOr<void> BarewordLiteral::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (metadata.is_first_in_list) {
        auto posibly_runnable = shell.runnable_path_for(m_text);
        if (posibly_runnable.has_value()) {
            Line::Style style = Line::Style::Bold;

            auto runnable = posibly_runnable.release_value();

#if defined(AK_OS_SERENITY)
            if (runnable.kind == Shell::RunnablePath::Kind::Executable || runnable.kind == Shell::RunnablePath::Kind::Alias)
                style = highlight_runnable(shell, runnable).value_or(Line::Style::Bold);
#endif

            editor.stylize({ m_position.start_offset, m_position.end_offset }, style);
        } else if (auto suggestions = shell.complete_program_name(m_text, m_text.bytes().size()); !suggestions.is_empty()) {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
        } else {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Red) });
        }

        return {};
    }

    if (m_text.starts_with('-')) {
        if (m_text == "--"sv) {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });
            return {};
        }
        if (m_text == "-"sv)
            return {};

        if (m_text.starts_with_bytes("--"sv)) {
            auto index = m_text.find_byte_offset('=').value_or(m_text.bytes_as_string_view().length() - 1) + 1;
            editor.stylize({ m_position.start_offset, m_position.start_offset + index }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan) });
        } else {
            editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan) });
        }
    }

    if (FileSystem::exists(m_text)) {
        TRY(highlight_filesystem_path(m_text, editor, shell, m_position.start_offset, m_position.end_offset));
    }

    return {};
}

BarewordLiteral::BarewordLiteral(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

ErrorOr<void> BraceExpansion::dump(int level) const
{
    TRY(Node::dump(level));
    for (auto& entry : m_entries)
        TRY(entry->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> BraceExpansion::run(RefPtr<Shell> shell)
{
    Vector<NonnullRefPtr<Value>> values;
    for (auto& entry : m_entries) {
        if (shell && shell->has_any_error())
            break;
        auto value = TRY(entry->run(shell));
        if (value)
            values.append(value.release_nonnull());
    }

    return make_ref_counted<ListValue>(move(values));
}

HitTestResult BraceExpansion::hit_test_position(size_t offset) const
{
    for (auto& entry : m_entries) {
        auto result = entry->hit_test_position(offset);
        if (result.matching_node) {
            if (!result.closest_command_node)
                result.closest_command_node = entry;
            return result;
        }
    }

    return {};
}

ErrorOr<void> BraceExpansion::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    for (auto& entry : m_entries) {
        TRY(entry->highlight_in_editor(editor, shell, metadata));
        metadata.is_first_in_list = false;
    }

    return {};
}

BraceExpansion::BraceExpansion(Position position, Vector<NonnullRefPtr<Node>> entries)
    : Node(move(position))
    , m_entries(move(entries))
{
    for (auto& entry : m_entries) {
        if (entry->is_syntax_error()) {
            set_is_syntax_error(entry->syntax_error_node());
            break;
        }
    }
}

ErrorOr<void> CastToCommand::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_inner->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> CastToCommand::run(RefPtr<Shell> shell)
{
    if (m_inner->is_command())
        return m_inner->run(shell);

    auto value = TRY(TRY(m_inner->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (value->is_command())
        return value;

    auto argv = TRY(value->resolve_as_list(shell));
    return make_ref_counted<CommandValue>(move(argv), position());
}

ErrorOr<void> CastToCommand::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    return m_inner->highlight_in_editor(editor, shell, metadata);
}

HitTestResult CastToCommand::hit_test_position(size_t offset) const
{
    auto result = m_inner->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    if (!result.closest_command_node && position().contains(offset))
        result.closest_command_node = this;
    return result;
}

ErrorOr<Vector<Line::CompletionSuggestion>> CastToCommand::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral const*>(matching_node.ptr());

    if (corrected_offset > node->text().bytes_as_string_view().length())
        return Vector<Line::CompletionSuggestion> {};

    return shell.complete_program_name(node->text(), corrected_offset);
}

RefPtr<Node const> CastToCommand::leftmost_trivial_literal() const
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

ErrorOr<void> CastToList::dump(int level) const
{
    TRY(Node::dump(level));
    if (m_inner)
        TRY(m_inner->dump(level + 1));
    else
        print_indented(level + 1, "(empty)");
    return {};
}

ErrorOr<RefPtr<Value>> CastToList::run(RefPtr<Shell> shell)
{
    if (!m_inner)
        return make_ref_counted<ListValue>({});

    auto inner_value = TRY(TRY(m_inner->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (inner_value->is_command() || inner_value->is_list())
        return inner_value;

    auto values = TRY(inner_value->resolve_as_list(shell));
    Vector<NonnullRefPtr<Value>> cast_values;
    for (auto& value : values)
        cast_values.append(make_ref_counted<StringValue>(value));

    return make_ref_counted<ListValue>(cast_values);
}

ErrorOr<void> CastToList::for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback)
{
    if (m_inner)
        TRY(m_inner->for_each_entry(shell, move(callback)));
    return {};
}

ErrorOr<void> CastToList::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (m_inner)
        TRY(m_inner->highlight_in_editor(editor, shell, metadata));
    return {};
}

HitTestResult CastToList::hit_test_position(size_t offset) const
{
    if (!m_inner)
        return {};

    return m_inner->hit_test_position(offset);
}

RefPtr<Node const> CastToList::leftmost_trivial_literal() const
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

ErrorOr<void> CloseFdRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level, "{} -> Close", m_fd);
    return {};
}

ErrorOr<RefPtr<Value>> CloseFdRedirection::run(RefPtr<Shell>)
{
    Command command;
    command.position = position();
    command.redirections.append(adopt_ref(*new CloseRedirection(m_fd)));
    return make_ref_counted<CommandValue>(move(command));
}

ErrorOr<void> CloseFdRedirection::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset - 1 }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
    editor.stylize({ m_position.end_offset - 1, m_position.end_offset }, { Line::Style::Foreground(0xff, 0x7e, 0x00) });   // Amber
    return {};
}

CloseFdRedirection::CloseFdRedirection(Position position, int fd)
    : Node(move(position))
    , m_fd(fd)
{
}

CloseFdRedirection::~CloseFdRedirection()
{
}

ErrorOr<void> CommandLiteral::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(Generated command literal: {})", m_command);
    return {};
}

ErrorOr<RefPtr<Value>> CommandLiteral::run(RefPtr<Shell>)
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

ErrorOr<void> Comment::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_text);
    return {};
}

ErrorOr<RefPtr<Value>> Comment::run(RefPtr<Shell>)
{
    return make_ref_counted<ListValue>({});
}

ErrorOr<void> Comment::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(150, 150, 150) }); // Light gray
    return {};
}

Comment::Comment(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

Comment::~Comment()
{
}

ErrorOr<void> ContinuationControl::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_kind == Continue ? "(Continue)"sv : "(Break)"sv);
    return {};
}

ErrorOr<RefPtr<Value>> ContinuationControl::run(RefPtr<Shell> shell)
{
    if (m_kind == Break)
        shell->raise_error(Shell::ShellError::InternalControlFlowBreak, {}, position());
    else if (m_kind == Continue)
        shell->raise_error(Shell::ShellError::InternalControlFlowContinue, {}, position());
    else
        VERIFY_NOT_REACHED();
    return make_ref_counted<ListValue>({});
}

ErrorOr<void> ContinuationControl::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    return {};
}

ErrorOr<void> DoubleQuotedString::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_inner->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> DoubleQuotedString::run(RefPtr<Shell> shell)
{
    StringBuilder builder;
    auto values = TRY(TRY(m_inner->run(shell))->resolve_as_list(shell));

    builder.join(""sv, values);

    return make_ref_counted<StringValue>(TRY(builder.to_string()));
}

ErrorOr<void> DoubleQuotedString::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });

    editor.stylize({ m_position.start_offset, m_position.end_offset }, style);
    metadata.is_first_in_list = false;
    return m_inner->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> DynamicEvaluate::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_inner->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> DynamicEvaluate::run(RefPtr<Shell> shell)
{
    auto result = TRY(TRY(m_inner->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    // Dynamic Evaluation behaves differently between strings and lists.
    // Strings are treated as variables, and Lists are treated as commands.
    if (result->is_string()) {
        auto name_part = TRY(result->resolve_as_list(shell));
        VERIFY(name_part.size() == 1);
        return make_ref_counted<SimpleVariableValue>(name_part[0]);
    }

    // If it's anything else, we're just gonna cast it to a list.
    auto list = TRY(result->resolve_as_list(shell));
    return make_ref_counted<CommandValue>(move(list), position());
}

ErrorOr<void> DynamicEvaluate::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    return m_inner->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> Fd2FdRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level, "{} -> {}", m_old_fd, m_new_fd);
    return {};
}

ErrorOr<RefPtr<Value>> Fd2FdRedirection::run(RefPtr<Shell>)
{
    Command command;
    command.position = position();
    command.redirections.append(FdRedirection::create(m_new_fd, m_old_fd, Rewiring::Close::None));
    return make_ref_counted<CommandValue>(move(command));
}

ErrorOr<void> Fd2FdRedirection::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
    return {};
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

ErrorOr<void> FunctionDeclaration::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(name: {})\n", m_name.name);
    print_indented(level + 1, "(argument names)");
    for (auto& arg : m_arguments)
        print_indented(level + 2, "(name: {})\n", arg.name);

    print_indented(level + 1, "(body)");
    if (m_block)
        TRY(m_block->dump(level + 2));
    else
        print_indented(level + 2, "(null)");
    return {};
}

ErrorOr<RefPtr<Value>> FunctionDeclaration::run(RefPtr<Shell> shell)
{
    Vector<ByteString> args;
    for (auto& arg : m_arguments)
        args.append(arg.name.to_byte_string());

    shell->define_function(m_name.name.to_byte_string(), move(args), m_block);

    return make_ref_counted<ListValue>({});
}

ErrorOr<void> FunctionDeclaration::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_name.position.start_offset, m_name.position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue) });

    for (auto& arg : m_arguments)
        editor.stylize({ arg.position.start_offset, arg.position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    metadata.is_first_in_list = true;
    if (m_block)
        TRY(m_block->highlight_in_editor(editor, shell, metadata));
    return {};
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

ErrorOr<Vector<Line::CompletionSuggestion>> FunctionDeclaration::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return Vector<Line::CompletionSuggestion> {};

    if (!matching_node->is_simple_variable())
        return matching_node->complete_for_editor(shell, offset, hit_test_result);

    auto corrected_offset = offset - matching_node->position().start_offset - 1; // Skip the first '$'
    auto* node = static_cast<SimpleVariable const*>(matching_node.ptr());

    auto name = node->name().bytes_as_string_view().substring_view(0, corrected_offset);

    Vector<Line::CompletionSuggestion> results;
    for (auto& arg : m_arguments) {
        if (arg.name.starts_with_bytes(name))
            results.append(arg.name.to_byte_string());
    }

    results.extend(TRY(matching_node->complete_for_editor(shell, offset, hit_test_result)));

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

ErrorOr<void> ForLoop::dump(int level) const
{
    TRY(Node::dump(level));
    if (m_variable.has_value())
        print_indented(level + 1, "iterating with {} in", m_variable->name);
    if (m_index_variable.has_value())
        print_indented(level + 1, "with index name {} in", m_index_variable->name);
    if (m_iterated_expression)
        TRY(m_iterated_expression->dump(level + 2));
    else
        print_indented(level + 2, "(ever)");
    print_indented(level + 1, "Running");
    if (m_block)
        TRY(m_block->dump(level + 2));
    else
        print_indented(level + 2, "(null)");
    return {};
}

ErrorOr<RefPtr<Value>> ForLoop::run(RefPtr<Shell> shell)
{
    if (!m_block)
        return make_ref_counted<ListValue>({});

    size_t consecutive_interruptions = 0;
    auto run = [&](auto& block_value) {
        if (shell->has_error(Shell::ShellError::InternalControlFlowBreak) || shell->has_error(Shell::ShellError::InternalControlFlowReturn)) {
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
        auto variable_name = m_variable.has_value() ? m_variable->name : "it"_string;
        Optional<StringView> index_name = m_index_variable.has_value() ? Optional<StringView>(m_index_variable->name) : Optional<StringView>();
        size_t i = 0;
        TRY(m_iterated_expression->for_each_entry(shell, [&](auto value) -> ErrorOr<IterationDecision> {
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
                auto frame = shell->push_frame(ByteString::formatted("for ({})", this));
                shell->set_local_variable(variable_name.bytes_as_string_view(), value, true);

                if (index_name.has_value())
                    shell->set_local_variable(index_name.value(), make_ref_counted<AST::StringValue>(String::number(i)), true);

                ++i;

                block_value = TRY(m_block->run(shell));
            }

            return run(block_value);
        }));
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

            RefPtr<Value> block_value = TRY(m_block->run(shell));
            if (run(block_value) == IterationDecision::Break)
                break;
        }
    }

    return make_ref_counted<ListValue>({});
}

ErrorOr<void> ForLoop::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    auto is_loop = m_iterated_expression.is_null();
    editor.stylize({ m_position.start_offset, m_position.start_offset + (is_loop ? 4 : 3) }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (!is_loop) {
        if (m_in_kw_position.has_value())
            editor.stylize({ m_in_kw_position.value().start_offset, m_in_kw_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        if (m_index_kw_position.has_value())
            editor.stylize({ m_index_kw_position.value().start_offset, m_index_kw_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        metadata.is_first_in_list = false;
        TRY(m_iterated_expression->highlight_in_editor(editor, shell, metadata));
    }

    if (m_index_variable.has_value())
        editor.stylize({ m_index_variable->position.start_offset, m_index_variable->position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    if (m_variable.has_value())
        editor.stylize({ m_variable->position.start_offset, m_variable->position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Italic });

    metadata.is_first_in_list = true;
    if (m_block)
        TRY(m_block->highlight_in_editor(editor, shell, metadata));
    return {};
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

ErrorOr<void> Glob::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_text);
    return {};
}

ErrorOr<RefPtr<Value>> Glob::run(RefPtr<Shell>)
{
    return make_ref_counted<GlobValue>(m_text, position());
}

ErrorOr<void> Glob::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Cyan) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
    return {};
}

Glob::Glob(Position position, String text)
    : Node(move(position))
    , m_text(move(text))
{
}

Glob::~Glob()
{
}

ErrorOr<void> Heredoc::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(End Key)");
    print_indented(level + 2, "{}", m_end);
    print_indented(level + 1, "(Allows Interpolation)");
    print_indented(level + 2, "{}", m_allows_interpolation);
    if (!evaluates_to_string()) {
        print_indented(level + 1, "(Target FD)");
        print_indented(level + 2, "{}", *m_target_fd);
    }
    print_indented(level + 1, "(Contents)");
    if (m_contents)
        TRY(m_contents->dump(level + 2));
    else
        print_indented(level + 2, "(null)");
    return {};
}

ErrorOr<RefPtr<Value>> Heredoc::run(RefPtr<Shell> shell)
{
    if (shell && shell->posix_mode() && !m_contents) {
        m_contents = make_ref_counted<StringLiteral>(position(), ""_string, StringLiteral::EnclosureType::None);
    }

    if (!m_contents) {
        if (shell)
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Attempt to evaluate an unresolved heredoc"sv, position());
        return nullptr;
    }

    auto value = TRY([&]() -> ErrorOr<RefPtr<Value>> {
        if (!m_deindent)
            return TRY(m_contents->run(shell));

        // To deindent, first split to lines...
        auto value = TRY(m_contents->run(shell));
        if (shell && shell->has_any_error())
            return make_ref_counted<ListValue>({});

        if (!value)
            return value;
        auto list = TRY(value->resolve_as_list(shell));
        // The list better have one entry, otherwise we've put the wrong kind of node inside this heredoc
        VERIFY(list.size() == 1);
        auto lines = list.first().bytes_as_string_view().split_view('\n');

        // Now just trim each line and put them back in a string
        StringBuilder builder { list.first().bytes_as_string_view().length() };
        for (auto& line : lines) {
            builder.append(line.trim_whitespace(TrimMode::Left));
            builder.append('\n');
        }

        return make_ref_counted<StringValue>(TRY(builder.to_string()));
    }());

    if (evaluates_to_string())
        return value;

    int fds[2];
    auto rc = pipe(fds);
    if (rc != 0) {
        // pipe() failed for {}
        if (shell)
            shell->raise_error(Shell::ShellError::PipeFailure, ByteString::formatted("heredoc: {}", strerror(errno)), position());
        return nullptr;
    }

    auto read_end = fds[0];
    auto write_end = fds[1];

    // Dump all of 'value' into the pipe.
    auto* file = fdopen(write_end, "wb");
    if (!file) {
        if (shell)
            shell->raise_error(Shell::ShellError::OpenFailure, "heredoc"sv, position());
        return nullptr;
    }

    auto text = TRY(value->resolve_as_string(shell));
    auto bytes = text.bytes();

    auto written = fwrite(bytes.data(), 1, bytes.size(), file);
    fflush(file);
    if (written != bytes.size()) {
        if (shell)
            shell->raise_error(Shell::ShellError::WriteFailure, "heredoc"sv, position());
    }
    fclose(file);

    Command command;
    command.position = position();
    command.redirections.append(FdRedirection::create(read_end, *target_fd(), Rewiring::Close::None));
    return make_ref_counted<CommandValue>(move(command));
}

ErrorOr<void> Heredoc::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style content_style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        content_style.unify_with({ Line::Style::Bold });

    if (!m_contents)
        content_style.unify_with({ Line::Style::Foreground(Line::Style::XtermColor::Red) }, true);

    editor.stylize({ m_position.start_offset, m_position.end_offset }, content_style);
    if (m_contents)
        TRY(m_contents->highlight_in_editor(editor, shell, metadata));
    return {};
}

HitTestResult Heredoc::hit_test_position(size_t offset) const
{
    if (!m_contents)
        return {};

    return m_contents->hit_test_position(offset);
}

Heredoc::Heredoc(Position position, String end, bool allow_interpolation, bool deindent, Optional<int> target_fd)
    : Node(move(position))
    , m_end(move(end))
    , m_allows_interpolation(allow_interpolation)
    , m_deindent(deindent)
    , m_target_fd(target_fd)
{
}

Heredoc::~Heredoc()
{
}

ErrorOr<void> HistoryEvent::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "Event Selector");
    switch (m_selector.event.kind) {
    case HistorySelector::EventKind::IndexFromStart:
        print_indented(level + 2, "IndexFromStart");
        break;
    case HistorySelector::EventKind::IndexFromEnd:
        print_indented(level + 2, "IndexFromEnd");
        break;
    case HistorySelector::EventKind::ContainingStringLookup:
        print_indented(level + 2, "ContainingStringLookup");
        break;
    case HistorySelector::EventKind::StartingStringLookup:
        print_indented(level + 2, "StartingStringLookup");
        break;
    }
    print_indented(level + 3, "{}({})", m_selector.event.index, m_selector.event.text);

    print_indented(level + 1, "Word Selector");
    auto print_word_selector = [&](HistorySelector::WordSelector const& selector) {
        switch (selector.kind) {
        case HistorySelector::WordSelectorKind::Index:
            print_indented(level + 3, "Index {}", selector.selector);
            break;
        case HistorySelector::WordSelectorKind::Last:
            print_indented(level + 3, "Last");
            break;
        }
    };

    if (m_selector.word_selector_range.end.has_value()) {
        print_indented(level + 2, "Range Start");
        print_word_selector(m_selector.word_selector_range.start);
        print_indented(level + 2, "Range End");
        print_word_selector(m_selector.word_selector_range.end.value());
    } else {
        print_indented(level + 2, "Direct Address");
        print_word_selector(m_selector.word_selector_range.start);
    }

    return {};
}

ErrorOr<RefPtr<Value>> HistoryEvent::run(RefPtr<Shell> shell)
{
    if (!shell)
        return make_ref_counted<AST::ListValue>({});

    auto editor = shell->editor();
    if (!editor) {
        shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "No history available!", position());
        return make_ref_counted<AST::ListValue>({});
    }
    auto& history = editor->history();

    // First, resolve the event itself.
    ByteString resolved_history;
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
        auto it = find_if(history.rbegin(), history.rend(), [&](auto& entry) { return entry.entry.contains(m_selector.event.text); });
        if (it.is_end()) {
            shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "History event did not match any entry", m_selector.event.text_position);
            return make_ref_counted<AST::ListValue>({});
        }
        resolved_history = it->entry;
        break;
    }
    case HistorySelector::EventKind::StartingStringLookup: {
        auto it = find_if(history.rbegin(), history.rend(), [&](auto& entry) { return entry.entry.starts_with(m_selector.event.text); });
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
    return nodes[index]->run(shell);
}

ErrorOr<void> HistoryEvent::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Green) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
    return {};
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

ErrorOr<void> Execute::dump(int level) const
{
    TRY(Node::dump(level));
    if (m_capture_stdout)
        print_indented(level + 1, "(Capturing stdout)");
    TRY(m_command->dump(level + 1));

    return {};
}

ErrorOr<void> Execute::for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback)
{
    if (m_command->would_execute())
        return m_command->for_each_entry(shell, move(callback));

    auto unexpanded_commands = TRY(TRY(m_command->run(shell))->resolve_as_commands(shell));
    if (shell && shell->has_any_error())
        return {};

    if (!shell)
        return {};

    auto commands = TRY(shell->expand_aliases(move(unexpanded_commands)));

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
            return {};
        }
        int pipefd[2];
        int rc = pipe(pipefd);
        if (rc < 0) {
            dbgln("Error: cannot pipe(): {}", strerror(errno));
            return {};
        }
        auto& last_in_commands = commands.last();

        last_in_commands.redirections.prepend(FdRedirection::create(pipefd[1], STDOUT_FILENO, Rewiring::Close::Old));
        last_in_commands.should_wait = false;
        last_in_commands.should_notify_if_in_background = false;
        last_in_commands.is_pipe_source = false;

        Core::EventLoop loop;

        auto notifier = Core::Notifier::construct(pipefd[0], Core::Notifier::Type::Read);
        AllocatingMemoryStream stream;

        enum CheckResult {
            Continue,
            Break,
            NothingLeft,
        };
        auto check_and_call = [&]() -> ErrorOr<CheckResult> {
            auto ifs = TRY(shell->local_variable_or("IFS"sv, "\n"sv));

            if (auto offset = TRY(stream.offset_of(ifs.bytes())); offset.has_value()) {
                auto line_end = offset.value();
                if (line_end == 0) {
                    TRY(stream.discard(ifs.length()));

                    if (shell->options.inline_exec_keep_empty_segments)
                        if (TRY(callback(make_ref_counted<StringValue>(String {}))) == IterationDecision::Break) {
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
                    TRY(stream.read_until_filled(entry));

                    auto str = TRY(String::from_utf8(StringView(entry.data(), entry.size() - ifs.length())));
                    if (TRY(callback(make_ref_counted<StringValue>(move(str)))) == IterationDecision::Break) {
                        loop.quit(Break);
                        notifier->set_enabled(false);
                        return Break;
                    }
                }

                return Continue;
            }

            return NothingLeft;
        };

        notifier->on_activation = [&]() -> void {
            constexpr static auto buffer_size = 16;
            u8 buffer[buffer_size];
            size_t remaining_size = buffer_size;

            for (;;) {
                notifier->set_type(Core::Notifier::Type::None);
                bool should_enable_notifier = false;

                ScopeGuard notifier_enabler { [&] {
                    if (should_enable_notifier)
                        notifier->set_type(Core::Notifier::Type::Read);
                } };

                if (check_and_call().release_value_but_fixme_should_propagate_errors() == Break) {
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
                stream.write_until_depleted({ buffer, (size_t)read_size }).release_value_but_fixme_should_propagate_errors();
            }

            loop.quit(NothingLeft);
        };

        auto jobs = shell->run_commands(commands);
        ScopeGuard kill_jobs_if_around { [&] {
            for (auto& job : jobs) {
                if (job->is_running_in_background() && !job->exited() && !job->signaled()) {
                    job->set_should_announce_signal(false); // We're explicitly killing it here.
                    shell->kill_job(job, SIGTERM);
                }
            }
        } };

        auto exit_reason = loop.exec();

        notifier->on_activation = nullptr;

        if (close(pipefd[0]) < 0) {
            dbgln("close() failed: {}", strerror(errno));
        }

        if (exit_reason != Break && !stream.is_eof()) {
            auto action = Continue;
            do {
                action = TRY(check_and_call());
                if (action == Break)
                    return {};
            } while (action == Continue);

            if (!stream.is_eof()) {
                auto entry_result = ByteBuffer::create_uninitialized(stream.used_buffer_size());
                if (entry_result.is_error()) {
                    shell->raise_error(Shell::ShellError::OutOfMemory, {}, position());
                    return {};
                }
                auto entry = entry_result.release_value();
                TRY(stream.read_until_filled(entry));
                TRY(callback(make_ref_counted<StringValue>(TRY(String::from_utf8(entry)))));
            }
        }

        return {};
    }

    auto jobs = shell->run_commands(commands);

    if (!jobs.is_empty())
        TRY(callback(make_ref_counted<JobValue>(jobs.last())));

    return {};
}

ErrorOr<RefPtr<Value>> Execute::run(RefPtr<Shell> shell)
{
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (m_command->would_execute())
        return m_command->run(shell);

    Vector<NonnullRefPtr<Value>> values;
    TRY(for_each_entry(shell, [&](auto value) {
        values.append(*value);
        return IterationDecision::Continue;
    }));

    if (values.size() == 1 && values.first()->is_job())
        return values.first();

    return make_ref_counted<ListValue>(move(values));
}

ErrorOr<void> Execute::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    if (m_capture_stdout)
        editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });
    metadata.is_first_in_list = true;
    return m_command->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<Vector<Line::CompletionSuggestion>> Execute::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral const*>(matching_node.ptr());

    if (corrected_offset > node->text().bytes_as_string_view().length())
        return Vector<Line::CompletionSuggestion> {};

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

ErrorOr<void> IfCond::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(++level, "Condition");
    TRY(m_condition->dump(level + 1));
    print_indented(level, "True Branch");
    if (m_true_branch)
        TRY(m_true_branch->dump(level + 1));
    else
        print_indented(level + 1, "(empty)");
    print_indented(level, "False Branch");
    if (m_false_branch)
        TRY(m_false_branch->dump(level + 1));
    else
        print_indented(level + 1, "(empty)");

    return {};
}

ErrorOr<RefPtr<Value>> IfCond::run(RefPtr<Shell> shell)
{
    auto cond = TRY(TRY(m_condition->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    // The condition could be a builtin, in which case it has already run and exited.
    if (cond->is_job()) {
        auto cond_job_value = static_cast<JobValue const*>(cond.ptr());
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

ErrorOr<void> IfCond::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;

    editor.stylize({ m_position.start_offset, m_position.start_offset + 2 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (m_else_position.has_value())
        editor.stylize({ m_else_position.value().start_offset, m_else_position.value().start_offset + 4 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    TRY(m_condition->highlight_in_editor(editor, shell, metadata));
    if (m_true_branch)
        TRY(m_true_branch->highlight_in_editor(editor, shell, metadata));
    if (m_false_branch)
        TRY(m_false_branch->highlight_in_editor(editor, shell, metadata));
    return {};
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

ErrorOr<void> ImmediateExpression::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(function)"sv);
    print_indented(level + 2, "{}", m_function.name);
    print_indented(level + 1, "(arguments)");
    for (auto& argument : arguments())
        TRY(argument->dump(level + 2));

    return {};
}

ErrorOr<RefPtr<Value>> ImmediateExpression::run(RefPtr<Shell> shell)
{
    auto node = TRY(shell->run_immediate_function(m_function.name, *this, arguments()));
    if (node)
        return node->run(shell);

    return make_ref_counted<ListValue>({});
}

ErrorOr<void> ImmediateExpression::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
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
        TRY(argument->highlight_in_editor(editor, shell, metadata));
    }

    // Closing brace
    if (m_closing_brace_position.has_value())
        editor.stylize({ m_closing_brace_position->start_offset, m_closing_brace_position->end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Green) });

    return {};
}

ErrorOr<Vector<Line::CompletionSuggestion>> ImmediateExpression::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || matching_node != this)
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - m_function.position.start_offset;

    if (corrected_offset > m_function.name.bytes_as_string_view().length())
        return Vector<Line::CompletionSuggestion> {};

    return shell.complete_immediate_function_name(m_function.name, corrected_offset);
}

HitTestResult ImmediateExpression::hit_test_position(size_t offset) const
{
    if (m_function.position.contains(offset))
        return { this, this, this };

    for (auto& argument : m_arguments) {
        if (auto result = argument->hit_test_position(offset); result.matching_node)
            return result;
    }

    return {};
}

ImmediateExpression::ImmediateExpression(Position position, NameWithPosition function, Vector<NonnullRefPtr<AST::Node>> arguments, Optional<Position> closing_brace_position)
    : Node(move(position))
    , m_arguments(move(arguments))
    , m_function(move(function))
    , m_closing_brace_position(move(closing_brace_position))
{
    if (is_syntax_error())
        return;

    for (auto& argument : m_arguments) {
        if (argument->is_syntax_error()) {
            set_is_syntax_error(argument->syntax_error_node());
            return;
        }
    }
}

ImmediateExpression::~ImmediateExpression()
{
}

ErrorOr<void> Join::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Join::run(RefPtr<Shell> shell)
{
    auto left = TRY(m_left->to_lazy_evaluated_commands(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (left.last().should_wait && !left.last().next_chain.is_empty()) {
        // Join (C0s*; C1) X -> (C0s*; Join C1 X)
        auto& lhs_node = left.last().next_chain.last().node;
        lhs_node = make_ref_counted<Join>(m_position, lhs_node, m_right);
        return make_ref_counted<CommandSequenceValue>(move(left));
    }

    auto right = TRY(m_right->to_lazy_evaluated_commands(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    return make_ref_counted<CommandSequenceValue>(join_commands(move(left), move(right)));
}

ErrorOr<void> Join::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_left->highlight_in_editor(editor, shell, metadata));
    if (m_left->is_list() || m_left->is_command())
        metadata.is_first_in_list = false;
    return m_right->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Join::hit_test_position(size_t offset) const
{
    auto result = m_left->hit_test_position(offset);
    if (result.matching_node)
        return result;

    return m_right->hit_test_position(offset);
}

RefPtr<Node const> Join::leftmost_trivial_literal() const
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

ErrorOr<void> MatchExpr::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(expression: {})", m_expr_name);
    TRY(m_matched_expr->dump(level + 2));
    print_indented(level + 1, "(named: {})", m_expr_name);
    print_indented(level + 1, "(entries)");
    for (auto& entry : m_entries) {
        StringBuilder builder;
        builder.append("(match"sv);
        if (entry.match_names.has_value()) {
            builder.append(" to names ("sv);
            bool first = true;
            for (auto& name : entry.match_names.value()) {
                if (!first)
                    builder.append(' ');
                first = false;
                builder.append(name);
            }
            builder.append("))"sv);

        } else {
            builder.append(')');
        }
        print_indented(level + 2, "{}", builder.string_view());
        TRY(entry.options.visit(
            [&](Vector<NonnullRefPtr<Node>> const& options) -> ErrorOr<void> {
                for (auto& option : options)
                    TRY(option->dump(level + 3));
                return {};
            },
            [&](Vector<Regex<ECMA262>> const& options) -> ErrorOr<void> {
                for (auto& option : options)
                    print_indented(level + 3, "(regex: {})", option.pattern_value);
                return {};
            }));
        print_indented(level + 2, "(execute)");
        if (entry.body)
            TRY(entry.body->dump(level + 3));
        else
            print_indented(level + 3, "(nothing)"sv);
    }
    return {};
}

ErrorOr<RefPtr<Value>> MatchExpr::run(RefPtr<Shell> shell)
{
    auto value = TRY(TRY(m_matched_expr->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto list = TRY(value->resolve_as_list(shell));

    auto list_matches = [&](auto&& pattern, auto& spans) -> ErrorOr<bool> {
        if constexpr (IsSame<RemoveCVReference<decltype(pattern)>, Regex<ECMA262>>) {
            if (list.size() != 1)
                return false;
            auto& subject = list.first();
            auto match = pattern.match(subject);
            if (!match.success)
                return false;

            spans.ensure_capacity(match.n_capture_groups);
            for (size_t i = 0; i < match.n_capture_groups; ++i) {
                auto& capture = match.capture_group_matches[0][i];
                spans.append(TRY(capture.view.to_string()));
            }
            return true;
        } else {
            if (pattern.size() != list.size())
                return false;

            for (size_t i = 0; i < pattern.size(); ++i) {
                Vector<AK::MaskSpan> mask_spans;
                if (!list[i].bytes_as_string_view().matches(pattern[i], mask_spans))
                    return false;
                for (auto& span : mask_spans)
                    spans.append(TRY(list[i].substring_from_byte_offset(span.start, span.length)));
            }

            return true;
        }
    };

    auto resolve_pattern = [&](auto& option) -> decltype(auto) {
        if constexpr (IsSame<RemoveCVReference<decltype(option)>, Regex<ECMA262>>) {
            return ErrorOr<Regex<ECMA262>>(move(option));
        } else {
            Vector<String> pattern;
            if (option->is_glob()) {
                pattern.append(static_cast<Glob const*>(option.ptr())->text());
            } else if (option->is_bareword()) {
                pattern.append(static_cast<BarewordLiteral const*>(option.ptr())->text());
            } else {
                auto list_or_error = option->run(shell);
                if (list_or_error.is_error() || (shell && shell->has_any_error()))
                    return ErrorOr<Vector<String>>(move(pattern));

                auto list = list_or_error.release_value();
                auto result = option->for_each_entry(shell, [&](auto&& value) -> ErrorOr<IterationDecision> {
                    pattern.extend(TRY(value->resolve_as_list(nullptr))); // Note: 'nullptr' incurs special behavior,
                                                                          //       asking the node for a 'raw' value.
                    return IterationDecision::Continue;
                });

                if (result.is_error())
                    return ErrorOr<Vector<String>>(result.release_error());
            }

            return ErrorOr<Vector<String>>(move(pattern));
        }
    };

    auto frame = shell->push_frame(ByteString::formatted("match ({})", this));
    if (!m_expr_name.is_empty())
        shell->set_local_variable(m_expr_name.to_byte_string(), value, true);

    for (auto& entry : m_entries) {
        auto result = TRY(entry.options.visit([&](auto& options) -> ErrorOr<Variant<IterationDecision, RefPtr<Value>>> {
            for (auto& option : options) {
                Vector<String> spans;
                if (TRY(list_matches(TRY(resolve_pattern(option)), spans))) {
                    if (entry.body) {
                        if (entry.match_names.has_value()) {
                            size_t i = 0;
                            for (auto& name : entry.match_names.value()) {
                                if (spans.size() > i)
                                    shell->set_local_variable(name.to_byte_string(), make_ref_counted<AST::StringValue>(spans[i]), true);
                                ++i;
                            }
                        }
                        return TRY(entry.body->run(shell));
                    }
                    return RefPtr<Value>(make_ref_counted<AST::ListValue>({}));
                }
            }
            return IterationDecision::Continue;
        }));
        if (result.has<IterationDecision>() && result.get<IterationDecision>() == IterationDecision::Break)
            break;

        if (result.has<RefPtr<Value>>())
            return move(result).get<RefPtr<Value>>();
    }

    // Non-exhaustive 'case' statements are valid in POSIX.
    if (!shell || !shell->posix_mode())
        shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Non-exhaustive match rules!", position());
    return make_ref_counted<AST::ListValue>({});
}

ErrorOr<void> MatchExpr::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.start_offset + 5 }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    if (m_as_position.has_value())
        editor.stylize({ m_as_position.value().start_offset, m_as_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    metadata.is_first_in_list = false;
    TRY(m_matched_expr->highlight_in_editor(editor, shell, metadata));

    for (auto& entry : m_entries) {
        metadata.is_first_in_list = false;
        TRY(entry.options.visit(
            [&](Vector<NonnullRefPtr<Node>>& node_options) -> ErrorOr<void> {
                for (auto& option : node_options)
                    TRY(option->highlight_in_editor(editor, shell, metadata));
                return {};
            },
            [](auto&) -> ErrorOr<void> { return {}; }));

        metadata.is_first_in_list = true;
        if (entry.body)
            TRY(entry.body->highlight_in_editor(editor, shell, metadata));

        for (auto& position : entry.pipe_positions)
            editor.stylize({ position.start_offset, position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

        if (entry.match_as_position.has_value())
            editor.stylize({ entry.match_as_position.value().start_offset, entry.match_as_position.value().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });
    }

    return {};
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

ErrorOr<void> Or::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Or::run(RefPtr<Shell> shell)
{
    auto commands = TRY(m_left->to_lazy_evaluated_commands(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    commands.last().next_chain.empend(*m_right, NodeWithAction::Or);
    return make_ref_counted<CommandSequenceValue>(move(commands));
}

ErrorOr<void> Or::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_left->highlight_in_editor(editor, shell, metadata));
    return m_right->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> Pipe::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Pipe::run(RefPtr<Shell> shell)
{
    auto left = TRY(m_left->to_lazy_evaluated_commands(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right = TRY(m_right->to_lazy_evaluated_commands(shell));
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
            if (!redirection->is_fd_redirection())
                continue;
            auto& fd_redirection = static_cast<FdRedirection&>(*redirection);
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

ErrorOr<void> Pipe::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_left->highlight_in_editor(editor, shell, metadata));
    return m_right->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> PathRedirectionNode::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(0x87, 0x9b, 0xcd) }); // 25% Darkened Periwinkle
    metadata.is_first_in_list = false;
    TRY(m_path->highlight_in_editor(editor, shell, metadata));

    if (m_path->is_bareword()) {
        auto path_text = TRY(TRY(m_path->run(nullptr))->resolve_as_list(nullptr));
        VERIFY(path_text.size() == 1);
        // Apply a URL to the path.
        auto& position = m_path->position();
        auto& path = path_text[0];
        if (!path.starts_with('/'))
            path = TRY(String::formatted("{}/{}", shell.cwd, path));
        TRY(highlight_filesystem_path_without_resolving(path, editor, shell, position.start_offset, position.end_offset));
    }

    return {};
}

HitTestResult PathRedirectionNode::hit_test_position(size_t offset) const
{
    auto result = m_path->hit_test_position(offset);
    if (!result.closest_node_with_semantic_meaning)
        result.closest_node_with_semantic_meaning = this;
    return result;
}

ErrorOr<Vector<Line::CompletionSuggestion>> PathRedirectionNode::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node || !matching_node->is_bareword())
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - matching_node->position().start_offset;
    auto* node = static_cast<BarewordLiteral const*>(matching_node.ptr());

    if (corrected_offset > node->text().bytes_as_string_view().length())
        return Vector<Line::CompletionSuggestion> {};

    return shell.complete_path(""sv, node->text(), corrected_offset, Shell::ExecutableOnly::No, nullptr, nullptr);
}

PathRedirectionNode::~PathRedirectionNode()
{
}

ErrorOr<void> Range::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(From)");
    TRY(m_start->dump(level + 2));
    print_indented(level + 1, "(To)");
    TRY(m_end->dump(level + 2));
    return {};
}

ErrorOr<RefPtr<Value>> Range::run(RefPtr<Shell> shell)
{
    auto interpolate = [position = position()](RefPtr<Value> start, RefPtr<Value> end, RefPtr<Shell> shell) -> ErrorOr<Vector<NonnullRefPtr<Value>>> {
        Vector<NonnullRefPtr<Value>> values;

        if (start->is_string() && end->is_string()) {
            auto start_str = TRY(start->resolve_as_list(shell))[0];
            auto end_str = TRY(end->resolve_as_list(shell))[0];

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
                        values.append(make_ref_counted<StringValue>(TRY(builder.to_string())));
                    }
                    // Append the ending code point too, most shells treat this as inclusive.
                    builder.clear();
                    builder.append_code_point(end_code_point);
                    values.append(make_ref_counted<StringValue>(TRY(builder.to_string())));
                } else {
                    // Could be two numbers?
                    auto start_int = start_str.to_number<int>();
                    auto end_int = end_str.to_number<int>();
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
                shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, ByteString::formatted("Cannot interpolate between '{}' and '{}'!", start_str, end_str), position);
                // We can't really interpolate between the two, so just yield both.
                values.append(make_ref_counted<StringValue>(move(start_str)));
                values.append(make_ref_counted<StringValue>(move(end_str)));
            }

            return values;
        }

        warnln("Shell: Cannot apply the requested interpolation");
        return values;
    };

    auto start_value = TRY(m_start->run(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto end_value = TRY(m_end->run(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    if (!start_value || !end_value)
        return make_ref_counted<ListValue>({});

    return make_ref_counted<ListValue>(TRY(interpolate(*start_value, *end_value, shell)));
}

ErrorOr<void> Range::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_start->highlight_in_editor(editor, shell, metadata));

    // Highlight the '..'
    editor.stylize({ m_start->position().end_offset, m_end->position().start_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow) });

    metadata.is_first_in_list = false;
    return m_end->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> ReadRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_path->dump(level + 1));
    print_indented(level + 1, "To {}", m_fd);
    return {};
}

ErrorOr<RefPtr<Value>> ReadRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = TRY(TRY(m_path->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(' ', path_segments);

    command.redirections.append(PathRedirection::create(TRY(builder.to_string()), m_fd, PathRedirection::Read));
    return make_ref_counted<CommandValue>(move(command));
}

ReadRedirection::ReadRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

ReadRedirection::~ReadRedirection()
{
}

ErrorOr<void> ReadWriteRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_path->dump(level + 1));
    print_indented(level + 1, "To/From {}", m_fd);
    return {};
}

ErrorOr<RefPtr<Value>> ReadWriteRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = TRY(TRY(m_path->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(' ', path_segments);

    command.redirections.append(PathRedirection::create(TRY(builder.to_string()), m_fd, PathRedirection::ReadWrite));
    return make_ref_counted<CommandValue>(move(command));
}

ReadWriteRedirection::ReadWriteRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

ReadWriteRedirection::~ReadWriteRedirection()
{
}

ErrorOr<void> Sequence::dump(int level) const
{
    TRY(Node::dump(level));
    for (auto& entry : m_entries)
        TRY(entry->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Sequence::run(RefPtr<Shell> shell)
{
    Vector<Command> all_commands;
    Command* last_command_in_sequence = nullptr;
    for (auto& entry : m_entries) {
        if (shell && shell->has_any_error())
            break;
        if (!last_command_in_sequence) {
            auto commands = TRY(entry->to_lazy_evaluated_commands(shell));
            all_commands.extend(move(commands));
            last_command_in_sequence = &all_commands.last();
            continue;
        }

        if (last_command_in_sequence->should_wait) {
            last_command_in_sequence->next_chain.append(NodeWithAction { entry, NodeWithAction::Sequence });
        } else {
            all_commands.extend(TRY(entry->to_lazy_evaluated_commands(shell)));
            last_command_in_sequence = &all_commands.last();
        }
    }

    return make_ref_counted<CommandSequenceValue>(move(all_commands));
}

ErrorOr<void> Sequence::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    for (auto& entry : m_entries)
        TRY(entry->highlight_in_editor(editor, shell, metadata));
    return {};
}

HitTestResult Sequence::hit_test_position(size_t offset) const
{
    for (auto& entry : m_entries) {
        auto result = entry->hit_test_position(offset);
        if (result.matching_node) {
            if (!result.closest_command_node)
                result.closest_command_node = entry;
            return result;
        }
    }

    return {};
}

RefPtr<Node const> Sequence::leftmost_trivial_literal() const
{
    for (auto& entry : m_entries) {
        if (auto node = entry->leftmost_trivial_literal())
            return node;
    }
    return nullptr;
}

Sequence::Sequence(Position position, Vector<NonnullRefPtr<Node>> entries, Vector<Position> separator_positions)
    : Node(move(position))
    , m_entries(move(entries))
    , m_separator_positions(separator_positions)
{
    for (auto& entry : m_entries) {
        if (entry->is_syntax_error()) {
            set_is_syntax_error(entry->syntax_error_node());
            break;
        }
    }
}

Sequence::~Sequence()
{
}

ErrorOr<void> Subshell::dump(int level) const
{
    TRY(Node::dump(level));
    if (m_block)
        TRY(m_block->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Subshell::run(RefPtr<Shell> shell)
{
    if (!m_block)
        return make_ref_counted<ListValue>({});

    return make_ref_counted<AST::CommandSequenceValue>(TRY(m_block->to_lazy_evaluated_commands(shell)));
}

ErrorOr<void> Subshell::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = true;
    if (m_block)
        TRY(m_block->highlight_in_editor(editor, shell, metadata));
    return {};
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

ErrorOr<void> Slice::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_selector->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Slice::run(RefPtr<Shell> shell)
{
    return m_selector->run(shell);
}

ErrorOr<void> Slice::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    return m_selector->highlight_in_editor(editor, shell, metadata);
}

HitTestResult Slice::hit_test_position(size_t offset) const
{
    return m_selector->hit_test_position(offset);
}

ErrorOr<Vector<Line::CompletionSuggestion>> Slice::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
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

ErrorOr<void> SimpleVariable::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(Name)");
    print_indented(level + 2, "{}", m_name);
    print_indented(level + 1, "(Slice)");
    if (m_slice)
        TRY(m_slice->dump(level + 2));
    else
        print_indented(level + 2, "(None)");
    return {};
}

ErrorOr<RefPtr<Value>> SimpleVariable::run(RefPtr<Shell>)
{
    NonnullRefPtr<Value> value = make_ref_counted<SimpleVariableValue>(m_name);
    if (m_slice)
        value = TRY(value->with_slices(*m_slice));
    return value;
}

ErrorOr<void> SimpleVariable::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    Line::Style style { Line::Style::Foreground(214, 112, 214) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));
    if (m_slice)
        TRY(m_slice->highlight_in_editor(editor, shell, metadata));
    return {};
}

HitTestResult SimpleVariable::hit_test_position(size_t offset) const
{
    if (!position().contains(offset))
        return {};

    if (m_slice && m_slice->position().contains(offset))
        return m_slice->hit_test_position(offset);

    return { this, this, nullptr };
}

ErrorOr<Vector<Line::CompletionSuggestion>> SimpleVariable::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return Vector<Line::CompletionSuggestion> {};

    if (matching_node != this)
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - matching_node->position().start_offset - 1;

    if (corrected_offset > m_name.bytes_as_string_view().length() + 1)
        return Vector<Line::CompletionSuggestion> {};

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

ErrorOr<void> SpecialVariable::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(Name)");
    print_indented(level + 1, "{:c}", m_name);
    print_indented(level + 1, "(Slice)");
    if (m_slice)
        TRY(m_slice->dump(level + 2));
    else
        print_indented(level + 2, "(None)");
    return {};
}

ErrorOr<RefPtr<Value>> SpecialVariable::run(RefPtr<Shell>)
{
    NonnullRefPtr<Value> value = make_ref_counted<SpecialVariableValue>(m_name);
    if (m_slice)
        value = TRY(value->with_slices(*m_slice));
    return value;
}

ErrorOr<void> SpecialVariable::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(214, 112, 214) });
    if (m_slice)
        TRY(m_slice->highlight_in_editor(editor, shell, metadata));
    return {};
}

ErrorOr<Vector<Line::CompletionSuggestion>> SpecialVariable::complete_for_editor(Shell&, size_t, HitTestResult const&) const
{
    return Vector<Line::CompletionSuggestion> {};
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

ErrorOr<void> Juxtaposition::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> Juxtaposition::run(RefPtr<Shell> shell)
{
    auto left_value = TRY(TRY(m_left->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right_value = TRY(TRY(m_right->run(shell))->resolve_without_cast(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto left = TRY(left_value->resolve_as_list(shell));
    auto right = TRY(right_value->resolve_as_list(shell));

    if (m_mode == Mode::StringExpand) {
        Vector<String> result;
        result.ensure_capacity(left.size() + right.size());

        for (auto& left_item : left)
            result.append(left_item);

        if (!result.is_empty() && !right.is_empty()) {
            auto& last = result.last();
            last = TRY(String::formatted("{}{}", last, right.first()));
            right.take_first();
        }
        for (auto& right_item : right)
            result.append(right_item);

        return make_ref_counted<ListValue>(move(result));
    }

    if (left_value->is_string() && right_value->is_string()) {

        VERIFY(left.size() == 1);
        VERIFY(right.size() == 1);

        StringBuilder builder;
        builder.append(left[0]);
        builder.append(right[0]);

        return make_ref_counted<StringValue>(TRY(builder.to_string()));
    }

    // Otherwise, treat them as lists and create a list product (or just append).
    if (left.is_empty() || right.is_empty())
        return make_ref_counted<ListValue>({});

    Vector<String> result;
    result.ensure_capacity(left.size() * right.size());

    StringBuilder builder;
    for (auto& left_element : left) {
        for (auto& right_element : right) {
            builder.append(left_element);
            builder.append(right_element);
            result.append(TRY(builder.to_string()));
            builder.clear();
        }
    }

    return make_ref_counted<ListValue>(move(result));
}

ErrorOr<void> Juxtaposition::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_left->highlight_in_editor(editor, shell, metadata));

    // '~/foo/bar' is special, we have to actually resolve the tilde
    // since that resolution is a pure operation, we can just go ahead
    // and do it to get the value :)
    if (m_right->is_bareword() && m_left->is_tilde()) {
        auto tilde_value = TRY(TRY(m_left->run(shell))->resolve_as_list(shell))[0];
        auto bareword_value = TRY(TRY(m_right->run(shell))->resolve_as_list(shell))[0];

        StringBuilder path_builder;
        path_builder.append(tilde_value);
        path_builder.append('/');
        path_builder.append(bareword_value);
        auto path = path_builder.to_byte_string();

        if (FileSystem::exists(path)) {
            TRY(highlight_filesystem_path(path, editor, shell, m_position.start_offset, m_position.end_offset));
        }

    } else {
        TRY(m_right->highlight_in_editor(editor, shell, metadata));
    }

    return {};
}

ErrorOr<Vector<Line::CompletionSuggestion>> Juxtaposition::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (m_left->would_execute() || m_right->would_execute()) {
        return Vector<Line::CompletionSuggestion> {};
    }

    // '~/foo/bar' is special, we have to actually resolve the tilde
    // then complete the bareword with that path prefix.
    auto left_values = TRY(TRY(m_left->run(shell))->resolve_as_list(shell));

    if (left_values.is_empty())
        return m_right->complete_for_editor(shell, offset, hit_test_result);

    auto& left_value = left_values.first();

    auto right_values = TRY(TRY(m_right->run(shell))->resolve_as_list(shell));
    StringView right_value {};

    auto corrected_offset = offset - matching_node->position().start_offset;

    if (!right_values.is_empty())
        right_value = right_values.first();

    if (m_left->is_tilde() && !right_value.is_empty()) {
        right_value = right_value.substring_view(1);
        corrected_offset--;
    }

    if (corrected_offset > right_value.length())
        return Vector<Line::CompletionSuggestion> {};

    return shell.complete_path(left_value, right_value, corrected_offset, Shell::ExecutableOnly::No, hit_test_result.closest_command_node.ptr(), hit_test_result.matching_node);
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

Juxtaposition::Juxtaposition(Position position, NonnullRefPtr<Node> left, NonnullRefPtr<Node> right, Juxtaposition::Mode mode)
    : Node(move(position))
    , m_left(move(left))
    , m_right(move(right))
    , m_mode(mode)
{
    if (m_left->is_syntax_error())
        set_is_syntax_error(m_left->syntax_error_node());
    else if (m_right->is_syntax_error())
        set_is_syntax_error(m_right->syntax_error_node());
}

Juxtaposition::~Juxtaposition()
{
}

ErrorOr<void> StringLiteral::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_text);
    return {};
}

ErrorOr<RefPtr<Value>> StringLiteral::run(RefPtr<Shell>)
{
    return make_ref_counted<StringValue>(m_text);
}

ErrorOr<void> StringLiteral::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata metadata)
{
    if (m_text.is_empty())
        return {};

    Line::Style style { Line::Style::Foreground(Line::Style::XtermColor::Yellow) };
    if (metadata.is_first_in_list)
        style.unify_with({ Line::Style::Bold });
    editor.stylize({ m_position.start_offset, m_position.end_offset }, move(style));

    return {};
}

StringLiteral::StringLiteral(Position position, String text, EnclosureType enclosure_type)
    : Node(move(position))
    , m_text(move(text))
    , m_enclosure_type(enclosure_type)
{
}

StringLiteral::~StringLiteral()
{
}

ErrorOr<void> StringPartCompose::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_left->dump(level + 1));
    TRY(m_right->dump(level + 1));
    return {};
}

ErrorOr<RefPtr<Value>> StringPartCompose::run(RefPtr<Shell> shell)
{
    auto left = TRY(TRY(m_left->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    auto right = TRY(TRY(m_right->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(' ', left);
    builder.join(' ', right);

    return make_ref_counted<StringValue>(TRY(builder.to_string()));
}

ErrorOr<void> StringPartCompose::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    TRY(m_left->highlight_in_editor(editor, shell, metadata));
    return m_right->highlight_in_editor(editor, shell, metadata);
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

ErrorOr<void> SyntaxError::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "(Error text)");
    print_indented(level + 2, "{}", m_syntax_error_text);
    print_indented(level + 1, "(Can be recovered from)");
    print_indented(level + 2, "{}", m_is_continuable);
    return {};
}

ErrorOr<RefPtr<Value>> SyntaxError::run(RefPtr<Shell> shell)
{
    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, m_syntax_error_text.to_byte_string(), position());
    return make_ref_counted<StringValue>(String {});
}

ErrorOr<void> SyntaxError::highlight_in_editor(Line::Editor& editor, Shell&, HighlightMetadata)
{
    editor.stylize({ m_position.start_offset, m_position.end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Bold });
    return {};
}

SyntaxError::SyntaxError(Position position, String error, bool is_continuable)
    : Node(move(position))
    , m_syntax_error_text(move(error))
    , m_is_continuable(is_continuable)
{
}

SyntaxError& SyntaxError::syntax_error_node()
{
    return *this;
}

SyntaxError::~SyntaxError()
{
}

ErrorOr<void> SyntheticNode::dump(int level) const
{
    TRY(Node::dump(level));
    return {};
}

ErrorOr<RefPtr<Value>> SyntheticNode::run(RefPtr<Shell>)
{
    return m_value;
}

ErrorOr<void> SyntheticNode::highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata)
{
    return {};
}

SyntheticNode::SyntheticNode(Position position, NonnullRefPtr<Value> value)
    : Node(move(position))
    , m_value(move(value))
{
}

ErrorOr<void> Tilde::dump(int level) const
{
    TRY(Node::dump(level));
    print_indented(level + 1, "{}", m_username);
    return {};
}

ErrorOr<RefPtr<Value>> Tilde::run(RefPtr<Shell>)
{
    return make_ref_counted<TildeValue>(m_username);
}

ErrorOr<void> Tilde::highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata)
{
    return {};
}

HitTestResult Tilde::hit_test_position(size_t offset) const
{
    if (!position().contains(offset))
        return {};

    return { this, this, nullptr };
}

ErrorOr<Vector<Line::CompletionSuggestion>> Tilde::complete_for_editor(Shell& shell, size_t offset, HitTestResult const& hit_test_result) const
{
    auto matching_node = hit_test_result.matching_node;
    if (!matching_node)
        return Vector<Line::CompletionSuggestion> {};

    if (matching_node != this)
        return Vector<Line::CompletionSuggestion> {};

    auto corrected_offset = offset - matching_node->position().start_offset - 1;

    if (corrected_offset > m_username.bytes_as_string_view().length() + 1)
        return Vector<Line::CompletionSuggestion> {};

    return shell.complete_user(m_username, corrected_offset);
}

String Tilde::text() const
{
    StringBuilder builder;
    builder.append('~');
    builder.append(m_username);
    return builder.to_string().release_value_but_fixme_should_propagate_errors();
}

Tilde::Tilde(Position position, String username)
    : Node(move(position))
    , m_username(move(username))
{
}

Tilde::~Tilde()
{
}

ErrorOr<void> WriteAppendRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_path->dump(level + 1));
    print_indented(level + 1, "From {}", m_fd);
    return {};
}

ErrorOr<RefPtr<Value>> WriteAppendRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = TRY(TRY(m_path->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(' ', path_segments);

    command.redirections.append(PathRedirection::create(TRY(builder.to_string()), m_fd, PathRedirection::WriteAppend));
    return make_ref_counted<CommandValue>(move(command));
}

WriteAppendRedirection::WriteAppendRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

WriteAppendRedirection::~WriteAppendRedirection()
{
}

ErrorOr<void> WriteRedirection::dump(int level) const
{
    TRY(Node::dump(level));
    TRY(m_path->dump(level + 1));
    print_indented(level + 1, "From {}", m_fd);
    return {};
}

ErrorOr<RefPtr<Value>> WriteRedirection::run(RefPtr<Shell> shell)
{
    Command command;
    auto path_segments = TRY(TRY(m_path->run(shell))->resolve_as_list(shell));
    if (shell && shell->has_any_error())
        return make_ref_counted<ListValue>({});

    StringBuilder builder;
    builder.join(' ', path_segments);

    command.redirections.append(PathRedirection::create(TRY(builder.to_string()), m_fd, PathRedirection::Write));
    return make_ref_counted<CommandValue>(move(command));
}

WriteRedirection::WriteRedirection(Position position, int fd, NonnullRefPtr<Node> path)
    : PathRedirectionNode(move(position), fd, move(path))
{
}

WriteRedirection::~WriteRedirection()
{
}

ErrorOr<void> VariableDeclarations::dump(int level) const
{
    TRY(Node::dump(level));
    for (auto& var : m_variables) {
        print_indented(level + 1, "Set");
        TRY(var.name->dump(level + 2));
        TRY(var.value->dump(level + 2));
    }
    return {};
}

ErrorOr<RefPtr<Value>> VariableDeclarations::run(RefPtr<Shell> shell)
{
    for (auto& var : m_variables) {
        auto name_value = TRY(TRY(var.name->run(shell))->resolve_as_list(shell));
        if (shell && shell->has_any_error())
            break;

        VERIFY(name_value.size() == 1);
        auto name = name_value[0];
        auto value = TRY(var.value->run(shell));
        if (shell && shell->has_any_error())
            break;
        value = TRY(value->resolve_without_cast(shell));

        shell->set_local_variable(name.to_byte_string(), value.release_nonnull());
    }

    return make_ref_counted<ListValue>({});
}

ErrorOr<void> VariableDeclarations::highlight_in_editor(Line::Editor& editor, Shell& shell, HighlightMetadata metadata)
{
    metadata.is_first_in_list = false;
    for (auto& var : m_variables) {
        TRY(var.name->highlight_in_editor(editor, shell, metadata));
        // Highlight the '='.
        editor.stylize({ var.name->position().end_offset - 1, var.name->position().end_offset }, { Line::Style::Foreground(Line::Style::XtermColor::Blue) });
        TRY(var.value->highlight_in_editor(editor, shell, metadata));
    }

    return {};
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

ErrorOr<String> Value::resolve_as_string(RefPtr<Shell> shell)
{
    if (shell)
        shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Conversion to string not allowed");
    return String {};
}

ErrorOr<Vector<AST::Command>> Value::resolve_as_commands(RefPtr<Shell> shell)
{
    Command command;
    command.argv = TRY(resolve_as_list(shell));
    return Vector { move(command) };
}

ListValue::ListValue(Vector<String> values)
{
    if (values.is_empty())
        return;
    m_contained_values.ensure_capacity(values.size());
    for (auto& str : values)
        m_contained_values.append(adopt_ref(*new StringValue(move(str))));
}

ErrorOr<NonnullRefPtr<Value>> Value::with_slices(NonnullRefPtr<Slice> slice) const&
{
    auto value = TRY(clone());
    value->m_slices.append(move(slice));
    return value;
}

ErrorOr<NonnullRefPtr<Value>> Value::with_slices(Vector<NonnullRefPtr<Slice>> slices) const&
{
    auto value = TRY(clone());
    value->m_slices.extend(move(slices));
    return value;
}

ListValue::~ListValue()
{
}

ErrorOr<Vector<String>> ListValue::resolve_as_list(RefPtr<Shell> shell)
{
    Vector<String> values;
    for (auto& value : m_contained_values)
        values.extend(TRY(value->resolve_as_list(shell)));

    return resolve_slices(shell, move(values), m_slices);
}

ErrorOr<String> ListValue::resolve_as_string(RefPtr<Shell> shell)
{
    if (!shell || !shell->posix_mode())
        return Value::resolve_as_string(shell);

    if (m_contained_values.is_empty())
        return resolve_slices(shell, String {}, m_slices);

    return resolve_slices(shell, TRY(m_contained_values[0]->resolve_as_string(shell)), m_slices);
}

ErrorOr<NonnullRefPtr<Value>> ListValue::resolve_without_cast(RefPtr<Shell> shell)
{
    Vector<NonnullRefPtr<Value>> values;
    for (auto& value : m_contained_values)
        values.append(TRY(value->resolve_without_cast(shell)));

    NonnullRefPtr<Value> value = make_ref_counted<ListValue>(move(values));
    if (!m_slices.is_empty())
        value = TRY(value->with_slices(m_slices));
    return value;
}

CommandValue::~CommandValue()
{
}

CommandSequenceValue::~CommandSequenceValue()
{
}

ErrorOr<Vector<String>> CommandSequenceValue::resolve_as_list(RefPtr<Shell> shell)
{
    shell->raise_error(Shell::ShellError::EvaluatedSyntaxError, "Unexpected cast of a command sequence to a list");
    return Vector<String> {};
}

ErrorOr<Vector<Command>> CommandSequenceValue::resolve_as_commands(RefPtr<Shell>)
{
    return m_contained_values;
}

ErrorOr<Vector<String>> CommandValue::resolve_as_list(RefPtr<Shell>)
{
    return m_command.argv;
}

ErrorOr<Vector<Command>> CommandValue::resolve_as_commands(RefPtr<Shell>)
{
    return Vector { m_command };
}

JobValue::~JobValue()
{
}

StringValue::~StringValue()
{
}

ErrorOr<String> StringValue::resolve_as_string(RefPtr<Shell> shell)
{
    if (m_split.is_empty())
        return TRY(resolve_slices(shell, String { m_string }, m_slices));
    return Value::resolve_as_string(shell);
}

ErrorOr<Vector<String>> StringValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (is_list()) {
        auto parts = StringView(m_string).split_view(m_split, m_keep_empty ? SplitBehavior::KeepEmpty : SplitBehavior::Nothing);
        Vector<String> result;
        result.ensure_capacity(parts.size());
        for (auto& part : parts)
            result.append(TRY(String::from_utf8(part)));
        return resolve_slices(shell, move(result), m_slices);
    }

    return Vector<String> { TRY(resolve_slices(shell, String { m_string }, m_slices)) };
}

ErrorOr<NonnullRefPtr<Value>> StringValue::resolve_without_cast(RefPtr<Shell> shell)
{
    if (is_list())
        return try_make_ref_counted<AST::ListValue>(TRY(resolve_as_list(shell))); // No need to reapply the slices.

    return *this;
}

GlobValue::~GlobValue()
{
}

ErrorOr<Vector<String>> GlobValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return resolve_slices(shell, Vector { m_glob }, m_slices);

    auto results = TRY(shell->expand_globs(m_glob, shell->cwd));
    if (results.is_empty())
        shell->raise_error(Shell::ShellError::InvalidGlobError, "Glob did not match anything!", m_generation_position);

    Vector<String> strings;
    TRY(strings.try_ensure_capacity(results.size()));
    for (auto& entry : results) {
        TRY(strings.try_append(TRY(String::from_byte_string(entry))));
    }

    return resolve_slices(shell, move(strings), m_slices);
}

SimpleVariableValue::~SimpleVariableValue()
{
}

ErrorOr<String> SimpleVariableValue::resolve_as_string(RefPtr<Shell> shell)
{
    if (!shell)
        return resolve_slices(shell, String {}, m_slices);

    if (auto value = TRY(resolve_without_cast(shell)); value != this)
        return resolve_slices(shell, TRY(value->resolve_as_string(shell)), m_slices);

    auto name = m_name.to_byte_string();
    char const* env_value = getenv(name.characters());
    if (!env_value)
        env_value = "";

    return resolve_slices(shell, TRY(String::from_utf8(StringView { env_value, strlen(env_value) })), m_slices);
}

ErrorOr<Vector<String>> SimpleVariableValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return resolve_slices(shell, Vector<String> {}, m_slices);

    if (auto value = TRY(resolve_without_cast(shell)); value != this)
        return value->resolve_as_list(shell);

    auto name = m_name.to_byte_string();
    char* env_value = getenv(name.characters());
    if (env_value == nullptr)
        return { resolve_slices(shell, Vector { String {} }, m_slices) };

    return Vector<String> { TRY(resolve_slices(shell, TRY(String::from_utf8(StringView { env_value, strlen(env_value) })), m_slices)) };
}

ErrorOr<NonnullRefPtr<Value>> SimpleVariableValue::resolve_without_cast(RefPtr<Shell> shell)
{
    VERIFY(shell);

    if (auto value = TRY(shell->look_up_local_variable(m_name))) {
        auto result = value.release_nonnull();
        // If a slice is applied, add it.
        if (!m_slices.is_empty())
            result = TRY(result->with_slices(m_slices));

        return const_cast<Value&>(*result);
    }

    return *this;
}

SpecialVariableValue::~SpecialVariableValue()
{
}

ErrorOr<String> SpecialVariableValue::resolve_as_string(RefPtr<Shell> shell)
{
    if (!shell)
        return String {};

    auto result = TRY(resolve_as_list(shell));
    if (result.size() == 1)
        return result[0];

    if (result.is_empty())
        return String {};

    return Value::resolve_as_string(shell);
}

ErrorOr<Vector<String>> SpecialVariableValue::resolve_as_list(RefPtr<Shell> shell)
{
    if (!shell)
        return Vector<String> {};

    switch (m_name) {
    case '?':
        return { resolve_slices(shell, Vector { String::number(shell->last_return_code.value_or(0)) }, m_slices) };
    case '$':
        return { resolve_slices(shell, Vector { String::number(getpid()) }, m_slices) };
    case '*':
        if (auto argv = TRY(shell->look_up_local_variable("ARGV"sv)))
            return resolve_slices(shell, TRY(const_cast<Value&>(*argv).resolve_as_list(shell)), m_slices);
        return resolve_slices(shell, Vector<String> {}, m_slices);
    case '#':
        if (auto argv = TRY(shell->look_up_local_variable("ARGV"sv))) {
            if (argv->is_list()) {
                auto list_argv = static_cast<AST::ListValue const*>(argv.ptr());
                return { resolve_slices(shell, Vector { String::number(list_argv->values().size()) }, m_slices) };
            }
            return { resolve_slices(shell, Vector { "1"_string }, m_slices) };
        }
        return { resolve_slices(shell, Vector { "0"_string }, m_slices) };
    default:
        return { resolve_slices(shell, Vector { String {} }, m_slices) };
    }
}

ErrorOr<NonnullRefPtr<Value>> SpecialVariableValue::resolve_without_cast(RefPtr<Shell> shell)
{
    if (!shell)
        return *this;

    return try_make_ref_counted<ListValue>(TRY(resolve_as_list(shell)));
}

TildeValue::~TildeValue()
{
}

ErrorOr<String> TildeValue::resolve_as_string(RefPtr<Shell> shell)
{
    return TRY(resolve_as_list(shell)).first();
}

ErrorOr<Vector<String>> TildeValue::resolve_as_list(RefPtr<Shell> shell)
{
    StringBuilder builder;
    builder.append('~');
    builder.append(m_username);

    if (!shell)
        return { resolve_slices(shell, Vector { TRY(builder.to_string()) }, m_slices) };

    return { resolve_slices(shell, Vector { TRY(String::from_byte_string(shell->expand_tilde(builder.to_byte_string()))) }, m_slices) };
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

    auto path_string = path.to_byte_string();
    switch (direction) {
    case AST::PathRedirection::WriteAppend:
        return check_fd_and_return(open(path_string.characters(), O_WRONLY | O_CREAT | O_APPEND, 0666), path);

    case AST::PathRedirection::Write:
        return check_fd_and_return(open(path_string.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666), path);

    case AST::PathRedirection::Read:
        return check_fd_and_return(open(path_string.characters(), O_RDONLY), path);

    case AST::PathRedirection::ReadWrite:
        return check_fd_and_return(open(path_string.characters(), O_RDWR | O_CREAT, 0666), path);
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
