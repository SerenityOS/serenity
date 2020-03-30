/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/BinarySearch.h>
#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <sys/stat.h>
#include <termios.h>

class LineEditor;

struct KeyCallback {
    KeyCallback(Function<bool(LineEditor&)> cb)
        : callback(move(cb))
    {
    }
    Function<bool(LineEditor&)> callback;
};

class LineEditor {
public:
    LineEditor(struct termios);
    LineEditor();
    ~LineEditor();

    void initialize(struct termios termios)
    {
        ASSERT(!m_initialized);
        m_termios = termios;
        m_initialized = true;
    }

    String get_line(const String& prompt);

    void add_to_history(const String&);
    const Vector<String>& history() const { return m_history; }

    void on_char_input(char ch, Function<bool(LineEditor&)> callback);

    Function<Vector<String>(const String&)> on_tab_complete_first_token = nullptr;
    Function<Vector<String>(const String&)> on_tab_complete_other_token = nullptr;

    // FIXME: we will have to kindly ask our instantiators to set our signal handlers
    // since we can not do this cleanly ourselves (signal() limitation: cannot give member functions)
    void interrupted() { m_was_interrupted = true; }
    void resized() { m_was_resized = true; }

    size_t cursor() const { return m_cursor; }
    const Vector<char, 1024>& buffer() const { return m_buffer; }
    char buffer_at(size_t pos) const { return m_buffer.at(pos); }

    void clear_line();
    void insert(const String&);
    void insert(const char);
    void cut_mismatching_chars(String& completion, const String& other, size_t start_compare);

private:
    void vt_save_cursor();
    void vt_restore_cursor();
    void vt_clear_to_end_of_line();

    Vector<char, 1024> m_buffer;
    size_t m_cursor { 0 };
    size_t m_times_tab_pressed { 0 };
    size_t m_num_columns { 0 };

    HashMap<char, NonnullOwnPtr<KeyCallback>> m_key_callbacks;

    // TODO: handle signals internally
    struct termios m_termios;
    bool m_was_interrupted = false;
    bool m_was_resized = false;

    // FIXME: This should be something more take_first()-friendly.
    Vector<String> m_history;
    size_t m_history_cursor { 0 };
    size_t m_history_capacity { 100 };

    enum class InputState {
        Free,
        ExpectBracket,
        ExpectFinal,
        ExpectTerminator,
    };
    InputState m_state { InputState::Free };

    bool m_initialized = false;
};
