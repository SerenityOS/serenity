#pragma once

#include <unistd.h>

class Position {
public:
    Position() { }
    Position(size_t line, size_t column) : m_line(line), m_column(column) { }

    size_t line() const { return m_line; }
    size_t column() const { return m_column; }

    void set_line(size_t l) { m_line = l; }
    void set_column(size_t c) { m_column = c; }

    void move_to(size_t l, size_t c) { m_line = l; m_column = c; }

    void move_by(ssize_t l, ssize_t c) { m_line += l; m_column += c; }

    bool is_valid() const { return m_line != InvalidValue && m_column != InvalidValue; }

private:
    static const size_t InvalidValue = 0xFFFFFFFF;

    size_t m_line { InvalidValue };
    size_t m_column { InvalidValue };
};
