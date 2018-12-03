#include "Document.h"
#include "FileReader.h"

OwnPtr<Document> Document::create_from_file(const std::string& path)
{
    auto document = make<Document>();

    FileReader reader(path);
    while (reader.can_read()) {
        auto line = reader.read_line();
        document->m_lines.push_back(Line(line));
    }

    return document;
}

void Document::dump()
{
    fprintf(stderr, "Document{%p}\n", this);
    for (size_t i = 0; i < m_lines.size(); ++i) {
        fprintf(stderr, "[%02zu] %s\n", i, m_lines[i].data().c_str());
    }
}

bool Document::backspace_at(Position position)
{
    return false;
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
    Line& line = m_lines[position.line()];
    if (position.column() > line.length())
        return false;
    line.insert(position.column(), text);
    return true;
}
