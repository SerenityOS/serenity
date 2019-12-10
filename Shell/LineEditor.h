#pragma once

#include <AK/BinarySearch.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/CDirIterator.h>
#include <sys/stat.h>

class LineEditor {
public:
    LineEditor();
    ~LineEditor();

    String get_line(const String& prompt);

    void add_to_history(const String&);
    const Vector<String>& history() const { return m_history; }

    void cache_path();

private:
    void clear_line();
    void insert(const String&);
    void insert(const char);
    void cut_mismatching_chars(String& completion, const String& program, size_t token_length);
    void tab_complete_first_token();
    void vt_save_cursor();
    void vt_restore_cursor();
    void vt_clear_to_end_of_line();

    Vector<char, 1024> m_buffer;
    size_t m_cursor { 0 };

    // FIXME: This should be something more take_first()-friendly.
    Vector<String> m_history;
    int m_history_cursor { 0 };
    int m_history_capacity { 100 };

    Vector<String, 256> m_path;

    enum class InputState {
        Free,
        ExpectBracket,
        ExpectFinal,
        ExpectTerminator,
    };
    InputState m_state { InputState::Free };
};
