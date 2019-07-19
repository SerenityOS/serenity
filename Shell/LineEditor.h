#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>

class LineEditor {
public:
    LineEditor();
    ~LineEditor();

    String get_line(const String& prompt);

    void add_to_history(const String&);
    const Vector<String>& history() const { return m_history; }

private:
    void clear_line();
    void append(const String&);
    void vt_save_cursor();
    void vt_restore_cursor();
    void vt_clear_to_end_of_line();

    Vector<char, 1024> m_buffer;
    int m_cursor { 0 };

    // FIXME: This should be something more take_first()-friendly.
    Vector<String> m_history;
    int m_history_cursor { 0 };
    int m_history_capacity { 100 };

    enum class InputState {
        Free,
        ExpectBracket,
        ExpectFinal,
    };
    InputState m_state { InputState::Free };
};
