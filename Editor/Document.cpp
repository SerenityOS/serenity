#include "Document.h"
#include "FileReader.h"

OwnPtr<Document> Document::create_from_file(const std::string& path)
{
    auto document = make<Document>(path);

    FileReader reader(path);
    while (reader.can_read()) {
        auto line = reader.read_line();
        document->m_lines.push_back(make<Line>(line));
    }

    return document;
}

void Document::dump()
{
    fprintf(stderr, "Document{%p}\n", this);
    for (size_t i = 0; i < line_count(); ++i) {
        fprintf(stderr, "[%02zu] %s\n", i, line(i).data().c_str());
    }
}

bool Document::backspace_at(Position)
{
    return false;
}

bool Document::newline_at(Position position)
{
    ASSERT(position.is_valid());
    ASSERT(position.line() < line_count());
    auto& line = this->line(position.line());
    if (position.column() > line.length())
        return false;
    if (position.column() == line.length()) {
        m_lines.insert(m_lines.begin() + position.line() + 1, make<Line>(""));
        return true;
    }
    auto chop = line.truncate(position.column());
    m_lines.insert(m_lines.begin() + position.line() + 1, make<Line>(chop));
    return true;
}

bool Document::insert_at(Position position, const std::string& text)
{
    static FILE* f = fopen("log", "a");
    fprintf(f, "@%zu,%zu: +%s\n", position.line(), position.column(), text.c_str());
    fflush(f);
    ASSERT(position.is_valid());
    if (!position.is_valid())
        return false;
    ASSERT(position.line() < line_count());
    if (position.line() >= line_count())
        return false;
    auto& line = this->line(position.line());
    if (position.column() > line.length())
        return false;
    line.insert(position.column(), text);
    return true;
}
