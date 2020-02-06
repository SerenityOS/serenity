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
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
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
    void cut_mismatching_chars(String& completion, const String& other, size_t start_compare);
    Vector<String> tab_complete_first_token(const String&);
    Vector<String> tab_complete_other_token(String&);
    void vt_save_cursor();
    void vt_restore_cursor();
    void vt_clear_to_end_of_line();

    Vector<char, 1024> m_buffer;
    size_t m_cursor { 0 };
    int m_times_tab_pressed { 0 };
    int m_num_columns { 0 };

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
