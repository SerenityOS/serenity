#include <AK/StringBuilder.h>
#include <LibGUI/GTextDocument.h>
#include <ctype.h>

GTextDocument::GTextDocument(Client* client)
{
    if (client)
        m_clients.set(client);
    append_line(make<GTextDocumentLine>(*this));
}

void GTextDocument::set_text(const StringView& text)
{
    m_client_notifications_enabled = false;
    m_spans.clear();
    remove_all_lines();

    int start_of_current_line = 0;

    auto add_line = [&](int current_position) {
        int line_length = current_position - start_of_current_line;
        auto line = make<GTextDocumentLine>(*this);
        if (line_length)
            line->set_text(*this, text.substring_view(start_of_current_line, current_position - start_of_current_line));
        append_line(move(line));
        start_of_current_line = current_position + 1;
    };
    int i = 0;
    for (i = 0; i < text.length(); ++i) {
        if (text[i] == '\n')
            add_line(i);
    }
    add_line(i);
    m_client_notifications_enabled = true;

    for (auto* client : m_clients)
        client->document_did_set_text();
}

int GTextDocumentLine::first_non_whitespace_column() const
{
    for (int i = 0; i < length(); ++i) {
        if (!isspace(m_text[i]))
            return i;
    }
    return length();
}

GTextDocumentLine::GTextDocumentLine(GTextDocument& document)
{
    clear(document);
}

GTextDocumentLine::GTextDocumentLine(GTextDocument& document, const StringView& text)
{
    set_text(document, text);
}

void GTextDocumentLine::clear(GTextDocument& document)
{
    m_text.clear();
    m_text.append(0);
    document.update_views({});
}

void GTextDocumentLine::set_text(GTextDocument& document, const StringView& text)
{
    if (text.length() == length() && !memcmp(text.characters_without_null_termination(), characters(), length()))
        return;
    if (text.is_empty()) {
        clear(document);
        return;
    }
    m_text.resize(text.length() + 1);
    memcpy(m_text.data(), text.characters_without_null_termination(), text.length() + 1);
    document.update_views({});
}

void GTextDocumentLine::append(GTextDocument& document, const char* characters, int length)
{
    int old_length = m_text.size() - 1;
    m_text.resize(m_text.size() + length);
    memcpy(m_text.data() + old_length, characters, length);
    m_text.last() = 0;
    document.update_views({});
}

void GTextDocumentLine::append(GTextDocument& document, char ch)
{
    insert(document, length(), ch);
}

void GTextDocumentLine::prepend(GTextDocument& document, char ch)
{
    insert(document, 0, ch);
}

void GTextDocumentLine::insert(GTextDocument& document, int index, char ch)
{
    if (index == length()) {
        m_text.last() = ch;
        m_text.append(0);
    } else {
        m_text.insert(index, move(ch));
    }
    document.update_views({});
}

void GTextDocumentLine::remove(GTextDocument& document, int index)
{
    if (index == length()) {
        m_text.take_last();
        m_text.last() = 0;
    } else {
        m_text.remove(index);
    }
    document.update_views({});
}

void GTextDocumentLine::truncate(GTextDocument& document, int length)
{
    m_text.resize(length + 1);
    m_text.last() = 0;
    document.update_views({});
}

void GTextDocument::append_line(NonnullOwnPtr<GTextDocumentLine> line)
{
    lines().append(move(line));
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_append_line();
    }
}

void GTextDocument::insert_line(int line_index, NonnullOwnPtr<GTextDocumentLine> line)
{
    lines().insert(line_index, move(line));
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_insert_line(line_index);
    }
}

void GTextDocument::remove_line(int line_index)
{
    lines().remove(line_index);
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_remove_line(line_index);
    }
}

void GTextDocument::remove_all_lines()
{
    lines().clear();
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_remove_all_lines();
    }
}

GTextDocument::Client::~Client()
{
}

void GTextDocument::register_client(Client& client)
{
    m_clients.set(&client);
}

void GTextDocument::unregister_client(Client& client)
{
    m_clients.remove(&client);
}

void GTextDocument::update_views(Badge<GTextDocumentLine>)
{
    if (m_client_notifications_enabled) {
        for (auto* client : m_clients)
            client->document_did_change();
    }
}

String GTextDocument::text_in_range(const GTextRange& a_range) const
{
    auto range = a_range.normalized();

    StringBuilder builder;
    for (int i = range.start().line(); i <= range.end().line(); ++i) {
        auto& line = lines()[i];
        int selection_start_column_on_line = range.start().line() == i ? range.start().column() : 0;
        int selection_end_column_on_line = range.end().line() == i ? range.end().column() : line.length();
        builder.append(line.characters() + selection_start_column_on_line, selection_end_column_on_line - selection_start_column_on_line);
        if (i != range.end().line())
            builder.append('\n');
    }

    return builder.to_string();
}

char GTextDocument::character_at(const GTextPosition& position) const
{
    ASSERT(position.line() < line_count());
    auto& line = lines()[position.line()];
    if (position.column() == line.length())
        return '\n';
    return line.characters()[position.column()];
}

GTextPosition GTextDocument::next_position_after(const GTextPosition& position, SearchShouldWrap should_wrap) const
{
    auto& line = lines()[position.line()];
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

GTextPosition GTextDocument::previous_position_before(const GTextPosition& position, SearchShouldWrap should_wrap) const
{
    if (position.column() == 0) {
        if (position.line() == 0) {
            if (should_wrap == SearchShouldWrap::Yes) {
                auto& last_line = lines()[line_count() - 1];
                return { line_count() - 1, last_line.length() };
            }
            return {};
        }
        auto& prev_line = lines()[position.line() - 1];
        return { position.line() - 1, prev_line.length() };
    }
    return { position.line(), position.column() - 1 };
}

GTextRange GTextDocument::find_next(const StringView& needle, const GTextPosition& start, SearchShouldWrap should_wrap) const
{
    if (needle.is_empty())
        return {};

    GTextPosition position = start.is_valid() ? start : GTextPosition(0, 0);
    GTextPosition original_position = position;

    GTextPosition start_of_potential_match;
    int needle_index = 0;

    do {
        auto ch = character_at(position);
        if (ch == needle[needle_index]) {
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

GTextRange GTextDocument::find_previous(const StringView& needle, const GTextPosition& start, SearchShouldWrap should_wrap) const
{
    if (needle.is_empty())
        return {};

    GTextPosition position = start.is_valid() ? start : GTextPosition(0, 0);
    position = previous_position_before(position, should_wrap);
    GTextPosition original_position = position;

    GTextPosition end_of_potential_match;
    int needle_index = needle.length() - 1;

    do {
        auto ch = character_at(position);
        if (ch == needle[needle_index]) {
            if (needle_index == needle.length() - 1)
                end_of_potential_match = position;
            --needle_index;
            if (needle_index < 0)
                return { position, next_position_after(end_of_potential_match, should_wrap) };
        } else {
            if (needle_index < needle.length() - 1)
                position = end_of_potential_match;
            needle_index = needle.length() - 1;
        }
        position = previous_position_before(position, should_wrap);
    } while (position.is_valid() && position != original_position);

    return {};
}

Vector<GTextRange> GTextDocument::find_all(const StringView& needle) const
{
    Vector<GTextRange> ranges;

    GTextPosition position;
    for (;;) {
        auto range = find_next(needle, position, SearchShouldWrap::No);
        if (!range.is_valid())
            break;
        ranges.append(range);
        position = range.end();
    }
    return ranges;
}

Optional<GTextDocumentSpan> GTextDocument::first_non_skippable_span_before(const GTextPosition& position) const
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

Optional<GTextDocumentSpan> GTextDocument::first_non_skippable_span_after(const GTextPosition& position) const
{
    for (int i = 0; i < m_spans.size(); ++i) {
        if (!m_spans[i].range.contains(position))
            continue;
        while ((i + 1) < m_spans.size() && m_spans[i + 1].is_skippable)
            ++i;
        if (i >= (m_spans.size() - 1))
            return {};
        return m_spans[i + 1];
    }
    return {};
}
