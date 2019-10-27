#include <LibGUI/GTextDocument.h>
#include <ctype.h>

GTextDocument::GTextDocument(GTextEditor& editor)
    : m_editor(editor)
{
    m_lines.append(make<GTextDocumentLine>(m_editor));
}

void GTextDocument::set_text(const StringView& text)
{
    m_spans.clear();
    m_lines.clear();
    int start_of_current_line = 0;

    auto add_line = [&](int current_position) {
        int line_length = current_position - start_of_current_line;
        auto line = make<GTextDocumentLine>(m_editor);
        if (line_length)
            line->set_text(text.substring_view(start_of_current_line, current_position - start_of_current_line));
        m_lines.append(move(line));
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

GTextDocumentLine::GTextDocumentLine(GTextEditor& editor)
    : m_editor(editor)
{
    clear();
}

GTextDocumentLine::GTextDocumentLine(GTextEditor& editor, const StringView& text)
    : m_editor(editor)
{
    set_text(text);
}

void GTextDocumentLine::clear()
{
    m_text.clear();
    m_text.append(0);
}

void GTextDocumentLine::set_text(const StringView& text)
{
    if (text.length() == length() && !memcmp(text.characters_without_null_termination(), characters(), length()))
        return;
    if (text.is_empty()) {
        clear();
        return;
    }
    m_text.resize(text.length() + 1);
    memcpy(m_text.data(), text.characters_without_null_termination(), text.length() + 1);
}

void GTextDocumentLine::append(const char* characters, int length)
{
    int old_length = m_text.size() - 1;
    m_text.resize(m_text.size() + length);
    memcpy(m_text.data() + old_length, characters, length);
    m_text.last() = 0;
}

void GTextDocumentLine::append(char ch)
{
    insert(length(), ch);
}

void GTextDocumentLine::prepend(char ch)
{
    insert(0, ch);
}

void GTextDocumentLine::insert(int index, char ch)
{
    if (index == length()) {
        m_text.last() = ch;
        m_text.append(0);
    } else {
        m_text.insert(index, move(ch));
    }
}

void GTextDocumentLine::remove(int index)
{
    if (index == length()) {
        m_text.take_last();
        m_text.last() = 0;
    } else {
        m_text.remove(index);
    }
}

void GTextDocumentLine::truncate(int length)
{
    m_text.resize(length + 1);
    m_text.last() = 0;
}
