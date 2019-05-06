#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>

class LineEditor {
public:
    LineEditor();
    ~LineEditor();

    String get_line();

    void add_to_history(const String&);
    const Vector<String>& history() const { return m_history; }

private:
    Vector<char, 1024> m_buffer;
    int m_cursor { 0 };

    // FIXME: This should be something more take_first()-friendly.
    Vector<String> m_history;
    int m_history_capacity { 100 };
};
