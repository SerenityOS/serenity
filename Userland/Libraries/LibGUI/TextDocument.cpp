/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/CharacterTypes.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibGUI/TextDocument.h>
#include <LibRegex/Regex.h>

namespace GUI {

NonnullRefPtr<TextDocument> TextDocument::create(Client* client)
{
    return adopt_ref(*new TextDocument(client));
}

TextDocument::TextDocument(Client* client)
{
    if (client)
        m_clients.set(client);
    append_line(make<TextDocumentLine>(*this));
    set_unmodified();

    m_undo_stack.on_state_change = [this] {
        if (m_client_notifications_enabled) {
            for (auto* client : m_clients)
                client->document_did_update_undo_stack();
        }
    };
}

TextDocument::~TextDocument()
{
}

bool TextDocument::set_text(StringView text, AllowCallback allow_callback)
{
    m_client_notifications_enabled = false;
    m_undo_stack.clear();
    m_spans.clear();
    remove_all_lines();

    ArmedScopeGuard clear_text_guard([this]() {
        set_text({});
    });

    size_t start_of_current_line = 0;

    auto add_line = [&](size_t current_position) -> bool {
        size_t line_length = current_position - start_of_current_line;
        auto line = make<TextDocumentLine>(*this);

        bool success = true;
        if (line_length)
            success = line->set_text(*this, text.substring_view(start_of_current_line, current_position - start_of_current_line));

        if (!success)
            return false;

        append_line(move(line));
        start_of_current_line = current_position + 1;

        return true;
    };

    size_t i = 0;
    for (i = 0; i < text.length(); ++i) {
        if (text[i] != '\n')
            continue;

        auto success = add_line(i);
        if (!success)
            return false;
    }

    auto success = add_line(i);
    if (!success)
        return false;

    // Don't show the file's trailing newline as an actual new line.
    if (line_count() > 1 && line(line_count() - 1).is_empty())
        (void)m_lines.take_last();

    m_client_notifications_enabled = true;

    for (auto* client : m_clients)
        client->document_did_set_text(allow_callback);

    clear_text_guard.disarm();

    // FIXME: Should the modified state be cleared on some of the earlier returns as well?
    set_unmodified();
    return true;
}

size_t TextDocumentLine::first_non_whitespace_column() const
{
    for (size_t i = 0; i < length(); ++i) {
        auto code_point = code_points()[i];
        if (!is_ascii_space(code_point))
            return i;
    }
    return length();
}

Optional<size_t> TextDocumentLine::last_non_whitespace_column() const
{
    for (ssize_t i = length() - 1; i >= 0; --i) {
        auto code_point = code_points()[i];
        if (!is_ascii_space(code_point))
            return i;
    }
    return {};
}

bool TextDocumentLine::ends_in_whitespace() const
{
    if (!length())
        return false;
    return is_ascii_space(code_points()[length() - 1]);
}

bool TextDocumentLine::can_select() const
{
    if (is_empty())
        return false;
    for (size_t i = 0; i < length(); ++i) {
        auto code_point = code_points()[i];
        if (code_point != '\n' && code_point != '\r' && code_point != '\f' && code_point != '\v')
            return true;
    }
    return false;
}

size_t TextDocumentLine::leading_spaces() const
{
    size_t count = 0;
    for (; count < m_text.size(); ++count) {
        if (m_text[count] != ' ') {
            break;
        }
    }
    return count;
}

String TextDocumentLine::to_utf8() const
{
    StringBuilder builder;
    builder.append(view());
    return builder.to_string();
}

TextDocumentLine::TextDocumentLine(TextDocument& document)
{
    clear(document);
}

TextDocumentLine::TextDocumentLine(TextDocument& document, StringView text)
{
    set_text(document, text);
}

void TextDocumentLine::clear(TextDocument& document)
{
    m_text.clear();
    document.update_views({});
}

void TextDocumentLine::set_text(TextDocument& document, const Vector<u32> text)
{
    m_text = move(text);
    document.update_views({});
}

bool TextDocumentLine::set_text(TextDocument& document, StringView text)
{
    if (text.is_empty()) {
        clear(document);
        return true;
    }
    m_text.clear();
    Utf8View utf8_view(text);
    if (!utf8_view.validate()) {
        return false;
    }
    for (auto code_point : utf8_view)
        m_text.append(code_point);
    document.update_views({});
    return true;
}

void TextDocumentLine::append(TextDocument& document, const u32* code_points, size_t length)
{
    if (length == 0)
        return;
    m_text.append(code_points, length);
    document.update_views({});
}

void TextDocumentLine::append(TextDocument& document, u32 code_point)
{
    insert(document, length(), code_point);
}

void TextDocumentLine::prepend(TextDocument& document, u32 code_point)
{
    insert(document, 0, code_point);
}

void TextDocumentLine::insert(TextDocument& document, size_t index, u32 code_point)
{
    if (index == length()) {
        m_text.append(code_point);
    } else {
        m_text.insert(index, code_point);
    }
    document.update_views({});
}

void TextDocumentLine::remove(TextDocument& document, size_t index)
{
    if (index == length()) {
        m_text.take_last();
    } else {
        m_text.remove(index);
    }
    document.update_views({});
}

void TextDocumentLine::remove_range(TextDocument& document, size_t start, size_t length)
{
    VERIFY(length <= m_text.size());

    Vector<u32> new_data;
    new_data.ensure_capacity(m_text.size() - length);
    for (size_t i = 0; i < start; ++i)
        new_data.append(m_text[i]);
    for (size_t i = (start + length); i < m_text.size(); ++i)
        new_data.append(m_text[i]);
    m_text = move(new_data);
    document.update_views({});
}

void TextDocumentLine::truncate(TextDocument& document, size_t length)
{
    m_text.resize(length);
    document.update_views({});
}

void TextDocument::append_line(NonnullOwnPtr<TextDocumentLine> line)
{
    lines().append(move(line));
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_append_line();
    }
}

void TextDocument::insert_line(size_t line_index, NonnullOwnPtr<TextDocumentLine> line)
{
    lines().insert((int)line_index, move(line));
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_insert_line(line_index);
    }
}

void TextDocument::remove_line(size_t line_index)
{
    lines().remove((int)line_index);
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_remove_line(line_index);
    }
}

void TextDocument::remove_all_lines()
{
    lines().clear();
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_remove_all_lines();
    }
}

TextDocument::Client::~Client()
{
}

void TextDocument::register_client(Client& client)
{
    m_clients.set(&client);
}

void TextDocument::unregister_client(Client& client)
{
    m_clients.remove(&client);
}

void TextDocument::update_views(Badge<TextDocumentLine>)
{
    notify_did_change();
}

void TextDocument::notify_did_change()
{
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_change();
    }

    m_regex_needs_update = true;
}

void TextDocument::set_all_cursors(const TextPosition& position)
{
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_set_cursor(position);
    }
}

String TextDocument::text() const
{
    StringBuilder builder;
    for (size_t i = 0; i < line_count(); ++i) {
        auto& line = this->line(i);
        builder.append(line.view());
        if (i != line_count() - 1)
            builder.append('\n');
    }
    return builder.to_string();
}

String TextDocument::text_in_range(const TextRange& a_range) const
{
    auto range = a_range.normalized();
    if (is_empty() || line_count() < range.end().line() - range.start().line())
        return String("");

    StringBuilder builder;
    for (size_t i = range.start().line(); i <= range.end().line(); ++i) {
        auto& line = this->line(i);
        size_t selection_start_column_on_line = range.start().line() == i ? range.start().column() : 0;
        size_t selection_end_column_on_line = range.end().line() == i ? range.end().column() : line.length();

        if (!line.is_empty()) {
            builder.append(
                Utf32View(
                    line.code_points() + selection_start_column_on_line,
                    selection_end_column_on_line - selection_start_column_on_line));
        }

        if (i != range.end().line())
            builder.append('\n');
    }

    return builder.to_string();
}

u32 TextDocument::code_point_at(const TextPosition& position) const
{
    VERIFY(position.line() < line_count());
    auto& line = this->line(position.line());
    if (position.column() == line.length())
        return '\n';
    return line.code_points()[position.column()];
}

TextPosition TextDocument::next_position_after(const TextPosition& position, SearchShouldWrap should_wrap) const
{
    auto& line = this->line(position.line());
    if (position.column() == line.length()) {
        if (position.line() == line_count() - 1) {
            if (should_wrap == SearchShouldWrap::Yes)
                return { 0, 0 };
            return {};
        }
        return { position.line() + 1, 0 };
    }
    return { position.line(), position.column() + 1 };
}

TextPosition TextDocument::previous_position_before(const TextPosition& position, SearchShouldWrap should_wrap) const
{
    if (position.column() == 0) {
        if (position.line() == 0) {
            if (should_wrap == SearchShouldWrap::Yes) {
                auto& last_line = this->line(line_count() - 1);
                return { line_count() - 1, last_line.length() };
            }
            return {};
        }
        auto& prev_line = this->line(position.line() - 1);
        return { position.line() - 1, prev_line.length() };
    }
    return { position.line(), position.column() - 1 };
}

void TextDocument::update_regex_matches(StringView needle)
{
    if (m_regex_needs_update || needle != m_regex_needle) {
        Regex<PosixExtended> re(needle);

        Vector<RegexStringView> views;

        for (size_t line = 0; line < m_lines.size(); ++line) {
            views.append(m_lines.at(line).view());
        }
        re.search(views, m_regex_result);
        m_regex_needs_update = false;
        m_regex_needle = String { needle };
        m_regex_result_match_index = -1;
        m_regex_result_match_capture_group_index = -1;
    }
}

TextRange TextDocument::find_next(StringView needle, const TextPosition& start, SearchShouldWrap should_wrap, bool regmatch, bool match_case)
{
    if (needle.is_empty())
        return {};

    if (regmatch) {
        if (!m_regex_result.matches.size())
            return {};

        regex::Match match;
        bool use_whole_match { false };

        auto next_match = [&] {
            m_regex_result_match_capture_group_index = 0;
            if (m_regex_result_match_index == m_regex_result.matches.size() - 1) {
                if (should_wrap == SearchShouldWrap::Yes)
                    m_regex_result_match_index = 0;
                else
                    ++m_regex_result_match_index;
            } else
                ++m_regex_result_match_index;
        };

        if (m_regex_result.n_capture_groups) {
            if (m_regex_result_match_index >= m_regex_result.capture_group_matches.size())
                next_match();
            else {
                // check if last capture group has been reached
                if (m_regex_result_match_capture_group_index >= m_regex_result.capture_group_matches.at(m_regex_result_match_index).size()) {
                    next_match();
                } else {
                    // get to the next capture group item
                    ++m_regex_result_match_capture_group_index;
                }
            }

            // use whole match, if there is no capture group for current index
            if (m_regex_result_match_index >= m_regex_result.capture_group_matches.size())
                use_whole_match = true;
            else if (m_regex_result_match_capture_group_index >= m_regex_result.capture_group_matches.at(m_regex_result_match_index).size())
                next_match();

        } else {
            next_match();
        }

        if (use_whole_match || !m_regex_result.capture_group_matches.at(m_regex_result_match_index).size())
            match = m_regex_result.matches.at(m_regex_result_match_index);
        else
            match = m_regex_result.capture_group_matches.at(m_regex_result_match_index).at(m_regex_result_match_capture_group_index);

        return TextRange { { match.line, match.column }, { match.line, match.column + match.view.length() } };
    }

    TextPosition position = start.is_valid() ? start : TextPosition(0, 0);
    TextPosition original_position = position;

    TextPosition start_of_potential_match;
    size_t needle_index = 0;

    do {
        auto ch = code_point_at(position);
        // FIXME: This is not the right way to use a Unicode needle!
        if (match_case ? ch == (u32)needle[needle_index] : tolower(ch) == tolower((u32)needle[needle_index])) {
            if (needle_index == 0)
                start_of_potential_match = position;
            ++needle_index;
            if (needle_index >= needle.length())
                return { start_of_potential_match, next_position_after(position, should_wrap) };
        } else {
            if (needle_index > 0)
                position = start_of_potential_match;
            needle_index = 0;
        }
        position = next_position_after(position, should_wrap);
    } while (position.is_valid() && position != original_position);

    return {};
}

TextRange TextDocument::find_previous(StringView needle, const TextPosition& start, SearchShouldWrap should_wrap, bool regmatch, bool match_case)
{
    if (needle.is_empty())
        return {};

    if (regmatch) {
        if (!m_regex_result.matches.size())
            return {};

        regex::Match match;
        bool use_whole_match { false };

        auto next_match = [&] {
            if (m_regex_result_match_index == 0) {
                if (should_wrap == SearchShouldWrap::Yes)
                    m_regex_result_match_index = m_regex_result.matches.size() - 1;
                else
                    --m_regex_result_match_index;
            } else
                --m_regex_result_match_index;

            m_regex_result_match_capture_group_index = m_regex_result.capture_group_matches.at(m_regex_result_match_index).size() - 1;
        };

        if (m_regex_result.n_capture_groups) {
            if (m_regex_result_match_index >= m_regex_result.capture_group_matches.size())
                next_match();
            else {
                // check if last capture group has been reached
                if (m_regex_result_match_capture_group_index >= m_regex_result.capture_group_matches.at(m_regex_result_match_index).size()) {
                    next_match();
                } else {
                    // get to the next capture group item
                    --m_regex_result_match_capture_group_index;
                }
            }

            // use whole match, if there is no capture group for current index
            if (m_regex_result_match_index >= m_regex_result.capture_group_matches.size())
                use_whole_match = true;
            else if (m_regex_result_match_capture_group_index >= m_regex_result.capture_group_matches.at(m_regex_result_match_index).size())
                next_match();

        } else {
            next_match();
        }

        if (use_whole_match || !m_regex_result.capture_group_matches.at(m_regex_result_match_index).size())
            match = m_regex_result.matches.at(m_regex_result_match_index);
        else
            match = m_regex_result.capture_group_matches.at(m_regex_result_match_index).at(m_regex_result_match_capture_group_index);

        return TextRange { { match.line, match.column }, { match.line, match.column + match.view.length() } };
    }

    TextPosition position = start.is_valid() ? start : TextPosition(0, 0);
    position = previous_position_before(position, should_wrap);
    if (position.line() >= line_count())
        return {};
    TextPosition original_position = position;

    TextPosition end_of_potential_match;
    size_t needle_index = needle.length() - 1;

    do {
        auto ch = code_point_at(position);
        // FIXME: This is not the right way to use a Unicode needle!
        if (match_case ? ch == (u32)needle[needle_index] : tolower(ch) == tolower((u32)needle[needle_index])) {
            if (needle_index == needle.length() - 1)
                end_of_potential_match = position;
            if (needle_index == 0)
                return { position, next_position_after(end_of_potential_match, should_wrap) };
            --needle_index;
        } else {
            if (needle_index < needle.length() - 1)
                position = end_of_potential_match;
            needle_index = needle.length() - 1;
        }
        position = previous_position_before(position, should_wrap);
    } while (position.is_valid() && position != original_position);

    return {};
}

Vector<TextRange> TextDocument::find_all(StringView needle, bool regmatch)
{
    Vector<TextRange> ranges;

    TextPosition position;
    for (;;) {
        auto range = find_next(needle, position, SearchShouldWrap::No, regmatch);
        if (!range.is_valid())
            break;
        ranges.append(range);
        position = range.end();
    }
    return ranges;
}

Optional<TextDocumentSpan> TextDocument::first_non_skippable_span_before(const TextPosition& position) const
{
    for (int i = m_spans.size() - 1; i >= 0; --i) {
        if (!m_spans[i].range.contains(position))
            continue;
        while ((i - 1) >= 0 && m_spans[i - 1].is_skippable)
            --i;
        if (i <= 0)
            return {};
        return m_spans[i - 1];
    }
    return {};
}

Optional<TextDocumentSpan> TextDocument::first_non_skippable_span_after(const TextPosition& position) const
{
    size_t i = 0;
    // Find the first span containing the cursor
    for (; i < m_spans.size(); ++i) {
        if (m_spans[i].range.contains(position))
            break;
    }
    // Find the first span *after* the cursor
    // TODO: For a large number of spans, binary search would be faster.
    for (; i < m_spans.size(); ++i) {
        if (!m_spans[i].range.contains(position))
            break;
    }
    // Skip skippable spans
    for (; i < m_spans.size(); ++i) {
        if (!m_spans[i].is_skippable)
            break;
    }
    if (i < m_spans.size())
        return m_spans[i];
    return {};
}

TextPosition TextDocument::first_word_break_before(const TextPosition& position, bool start_at_column_before) const
{
    if (position.column() == 0) {
        if (position.line() == 0) {
            return TextPosition(0, 0);
        }
        auto previous_line = this->line(position.line() - 1);
        return TextPosition(position.line() - 1, previous_line.length());
    }

    auto target = position;
    auto line = this->line(target.line());
    auto modifier = start_at_column_before ? 1 : 0;
    if (target.column() == line.length())
        modifier = 1;

    while (target.column() > 0 && is_ascii_blank(line.code_points()[target.column() - modifier]))
        target.set_column(target.column() - 1);
    auto is_start_alphanumeric = is_ascii_alphanumeric(line.code_points()[target.column() - modifier]);

    while (target.column() > 0) {
        auto prev_code_point = line.code_points()[target.column() - 1];
        if ((is_start_alphanumeric && !is_ascii_alphanumeric(prev_code_point)) || (!is_start_alphanumeric && is_ascii_alphanumeric(prev_code_point)))
            break;
        target.set_column(target.column() - 1);
    }

    return target;
}

TextPosition TextDocument::first_word_break_after(const TextPosition& position) const
{
    auto target = position;
    auto line = this->line(target.line());

    if (position.column() >= line.length()) {
        if (position.line() >= this->line_count() - 1) {
            return position;
        }
        return TextPosition(position.line() + 1, 0);
    }

    while (target.column() < line.length() && is_ascii_space(line.code_points()[target.column()]))
        target.set_column(target.column() + 1);
    auto is_start_alphanumeric = is_ascii_alphanumeric(line.code_points()[target.column()]);

    while (target.column() < line.length()) {
        auto next_code_point = line.code_points()[target.column()];
        if ((is_start_alphanumeric && !is_ascii_alphanumeric(next_code_point)) || (!is_start_alphanumeric && is_ascii_alphanumeric(next_code_point)))
            break;
        target.set_column(target.column() + 1);
    }

    return target;
}

TextPosition TextDocument::first_word_before(const TextPosition& position, bool start_at_column_before) const
{
    if (position.column() == 0) {
        if (position.line() == 0) {
            return TextPosition(0, 0);
        }
        auto previous_line = this->line(position.line() - 1);
        return TextPosition(position.line() - 1, previous_line.length());
    }

    auto target = position;
    auto line = this->line(target.line());
    if (target.column() == line.length())
        start_at_column_before = 1;

    auto nonblank_passed = !is_ascii_blank(line.code_points()[target.column() - start_at_column_before]);
    while (target.column() > 0) {
        auto prev_code_point = line.code_points()[target.column() - 1];
        nonblank_passed |= !is_ascii_blank(prev_code_point);

        if (nonblank_passed && is_ascii_blank(prev_code_point)) {
            break;
        } else if (is_ascii_punctuation(prev_code_point)) {
            target.set_column(target.column() - 1);
            break;
        }

        target.set_column(target.column() - 1);
    }

    return target;
}

void TextDocument::undo()
{
    if (!can_undo())
        return;
    m_undo_stack.undo();
    notify_did_change();
}

void TextDocument::redo()
{
    if (!can_redo())
        return;
    m_undo_stack.redo();
    notify_did_change();
}

void TextDocument::add_to_undo_stack(NonnullOwnPtr<TextDocumentUndoCommand> undo_command)
{
    m_undo_stack.push(move(undo_command));
}

TextDocumentUndoCommand::TextDocumentUndoCommand(TextDocument& document)
    : m_document(document)
{
}

TextDocumentUndoCommand::~TextDocumentUndoCommand()
{
}

InsertTextCommand::InsertTextCommand(TextDocument& document, const String& text, const TextPosition& position)
    : TextDocumentUndoCommand(document)
    , m_text(text)
    , m_range({ position, position })
{
}

String InsertTextCommand::action_text() const
{
    return "Insert Text";
}

bool InsertTextCommand::merge_with(GUI::Command const& other)
{
    if (!is<InsertTextCommand>(other))
        return false;
    auto& typed_other = static_cast<InsertTextCommand const&>(other);
    if (m_range.end() != typed_other.m_range.start())
        return false;
    if (m_range.start().line() != m_range.end().line())
        return false;
    StringBuilder builder(m_text.length() + typed_other.m_text.length());
    builder.append(m_text);
    builder.append(typed_other.m_text);
    m_text = builder.to_string();
    m_range.set_end(typed_other.m_range.end());
    return true;
}

void InsertTextCommand::perform_formatting(const TextDocument::Client& client)
{
    const size_t tab_width = client.soft_tab_width();
    const auto& dest_line = m_document.line(m_range.start().line());
    const bool should_auto_indent = client.is_automatic_indentation_enabled();

    StringBuilder builder;
    size_t column = m_range.start().column();
    size_t line_indentation = dest_line.leading_spaces();
    bool at_start_of_line = line_indentation == column;

    for (auto input_char : m_text) {
        if (input_char == '\n') {
            size_t spaces_at_end = 0;
            if (column < line_indentation)
                spaces_at_end = line_indentation - column;
            line_indentation -= spaces_at_end;
            builder.append('\n');
            column = 0;
            if (should_auto_indent) {
                for (; column < line_indentation; ++column) {
                    builder.append(' ');
                }
            }
            at_start_of_line = true;
        } else if (input_char == '\t') {
            size_t next_soft_tab_stop = ((column + tab_width) / tab_width) * tab_width;
            size_t spaces_to_insert = next_soft_tab_stop - column;
            for (size_t i = 0; i < spaces_to_insert; ++i) {
                builder.append(' ');
            }
            column = next_soft_tab_stop;
            if (at_start_of_line) {
                line_indentation = column;
            }
        } else {
            if (input_char == ' ') {
                if (at_start_of_line) {
                    ++line_indentation;
                }
            } else {
                at_start_of_line = false;
            }
            builder.append(input_char);
            ++column;
        }
    }
    m_text = builder.build();
}

void InsertTextCommand::redo()
{
    auto new_cursor = m_document.insert_at(m_range.start(), m_text, m_client);
    // NOTE: We don't know where the range ends until after doing redo().
    //       This is okay since we always do redo() after adding this to the undo stack.
    m_range.set_end(new_cursor);
    m_document.set_all_cursors(new_cursor);
}

void InsertTextCommand::undo()
{
    m_document.remove(m_range);
    m_document.set_all_cursors(m_range.start());
}

RemoveTextCommand::RemoveTextCommand(TextDocument& document, const String& text, const TextRange& range)
    : TextDocumentUndoCommand(document)
    , m_text(text)
    , m_range(range)
{
}

String RemoveTextCommand::action_text() const
{
    return "Remove Text";
}

bool RemoveTextCommand::merge_with(GUI::Command const& other)
{
    if (!is<RemoveTextCommand>(other))
        return false;
    auto& typed_other = static_cast<RemoveTextCommand const&>(other);
    if (m_range.start() != typed_other.m_range.end())
        return false;
    if (m_range.start().line() != m_range.end().line())
        return false;
    // Merge backspaces
    StringBuilder builder(m_text.length() + typed_other.m_text.length());
    builder.append(typed_other.m_text);
    builder.append(m_text);
    m_text = builder.to_string();
    m_range.set_start(typed_other.m_range.start());
    return true;
}

void RemoveTextCommand::redo()
{
    m_document.remove(m_range);
    m_document.set_all_cursors(m_range.start());
}

void RemoveTextCommand::undo()
{
    auto new_cursor = m_document.insert_at(m_range.start(), m_text);
    m_document.set_all_cursors(new_cursor);
}

TextPosition TextDocument::insert_at(const TextPosition& position, StringView text, const Client* client)
{
    TextPosition cursor = position;
    Utf8View utf8_view(text);
    for (auto code_point : utf8_view)
        cursor = insert_at(cursor, code_point, client);
    return cursor;
}

TextPosition TextDocument::insert_at(const TextPosition& position, u32 code_point, const Client*)
{
    if (code_point == '\n') {
        auto new_line = make<TextDocumentLine>(*this);
        new_line->append(*this, line(position.line()).code_points() + position.column(), line(position.line()).length() - position.column());
        line(position.line()).truncate(*this, position.column());
        insert_line(position.line() + 1, move(new_line));
        notify_did_change();
        return { position.line() + 1, 0 };
    } else {
        line(position.line()).insert(*this, position.column(), code_point);
        notify_did_change();
        return { position.line(), position.column() + 1 };
    }
}

void TextDocument::remove(const TextRange& unnormalized_range)
{
    if (!unnormalized_range.is_valid())
        return;

    auto range = unnormalized_range.normalized();

    // First delete all the lines in between the first and last one.
    for (size_t i = range.start().line() + 1; i < range.end().line();) {
        remove_line(i);
        range.end().set_line(range.end().line() - 1);
    }

    if (range.start().line() == range.end().line()) {
        // Delete within same line.
        auto& line = this->line(range.start().line());
        if (line.length() == 0)
            return;

        bool whole_line_is_selected = range.start().column() == 0 && range.end().column() == line.length();

        if (whole_line_is_selected) {
            line.clear(*this);
        } else {
            line.remove_range(*this, range.start().column(), range.end().column() - range.start().column());
        }
    } else {
        // Delete across a newline, merging lines.
        VERIFY(range.start().line() == range.end().line() - 1);

        auto& first_line = line(range.start().line());
        auto& second_line = line(range.end().line());

        Vector<u32> code_points;
        code_points.append(first_line.code_points(), range.start().column());
        if (!second_line.is_empty())
            code_points.append(second_line.code_points() + range.end().column(), second_line.length() - range.end().column());
        first_line.set_text(*this, move(code_points));

        remove_line(range.end().line());
    }

    if (lines().is_empty()) {
        append_line(make<TextDocumentLine>(*this));
    }

    notify_did_change();
}

bool TextDocument::is_empty() const
{
    return line_count() == 1 && line(0).is_empty();
}

TextRange TextDocument::range_for_entire_line(size_t line_index) const
{
    if (line_index >= line_count())
        return {};
    return { { line_index, 0 }, { line_index, line(line_index).length() } };
}

const TextDocumentSpan* TextDocument::span_at(const TextPosition& position) const
{
    for (auto& span : m_spans) {
        if (span.range.contains(position))
            return &span;
    }
    return nullptr;
}

void TextDocument::set_unmodified()
{
    m_undo_stack.set_current_unmodified();
}

}
