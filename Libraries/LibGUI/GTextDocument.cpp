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
    for (auto* client : m_clients)
        client->document_did_append_line();
}

void GTextDocument::insert_line(int line_index, NonnullOwnPtr<GTextDocumentLine> line)
{
    lines().insert(line_index, move(line));
    for (auto* client : m_clients)
        client->document_did_insert_line(line_index);
}

void GTextDocument::remove_line(int line_index)
{
    lines().remove(line_index);
    for (auto* client : m_clients)
        client->document_did_remove_line(line_index);
}

void GTextDocument::remove_all_lines()
{
    lines().clear();
    for (auto* client : m_clients)
        client->document_did_remove_all_lines();
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
    for (auto* client : m_clients)
        client->document_did_change();
}
