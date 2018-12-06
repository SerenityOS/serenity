#pragma once

#include "cuki.h"
#include "Line.h"
#include "Position.h"
#include "OwnPtr.h"
#include <string>

class Document {
public:
    explicit Document(const std::string& path) : m_path(path) { }
    ~Document() { }

    std::string path() const { return m_path; }

    Line& line(size_t index) { return *m_lines[index]; }
    const Line& line(size_t index) const { return *m_lines[index]; }
    size_t line_count() const { return m_lines.size(); }

    static OwnPtr<Document> create_from_file(const std::string& path);

    bool insert_at(Position, const std::string&);
    bool newline_at(Position);
    bool backspace_at(Position);
    bool erase_at(Position, int count);

    void dump();

private:
    std::deque<OwnPtr<Line>> m_lines;
    std::string m_path;
};
