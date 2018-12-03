#pragma once

#include "cuki.h"
#include "Line.h"
#include "Position.h"
#include "OwnPtr.h"
#include <string>

class Document {
public:
    Document() { }
    ~Document() { }

    const std::deque<Line>& lines() const { return m_lines; }
    std::deque<Line>& lines() { return m_lines; }
    size_t line_count() const { return m_lines.size(); }

    static OwnPtr<Document> create_from_file(const std::string& path);

    bool insert_at(Position, const std::string&);
    bool backspace_at(Position);

    void dump();

private:
    std::deque<Line> m_lines;
};
