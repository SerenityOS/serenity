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

#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibVT/Line.h>
#include <LibVT/Position.h>

namespace VT {

class TerminalClient {
public:
    virtual ~TerminalClient() { }

    virtual void beep() = 0;
    virtual void set_window_title(const StringView&) = 0;
    virtual void set_window_progress(int value, int max) = 0;
    virtual void terminal_did_resize(u16 columns, u16 rows) = 0;
    virtual void terminal_history_changed() = 0;
    virtual void emit(const u8*, size_t) = 0;
};

class Terminal {
public:
    explicit Terminal(TerminalClient&);
    ~Terminal();

    bool m_need_full_flush { false };

    void invalidate_cursor();
    void on_input(u8);

    void clear();
    void clear_including_history();

    void set_size(u16 columns, u16 rows);
    u16 columns() const { return m_columns; }
    u16 rows() const { return m_rows; }

    u16 cursor_column() const { return m_cursor_column; }
    u16 cursor_row() const { return m_cursor_row; }

    size_t line_count() const
    {
        return m_history.size() + m_lines.size();
    }

    Line& line(size_t index)
    {
        if (index < m_history.size())
            return m_history[(m_history_start + index) % m_history.size()];
        return m_lines[index - m_history.size()];
    }
    const Line& line(size_t index) const
    {
        if (index < m_history.size())
            return m_history[index];
        return m_lines[index - m_history.size()];
    }

    Line& visible_line(size_t index)
    {
        return m_lines[index];
    }
    const Line& visible_line(size_t index) const
    {
        return m_lines[index];
    }

    size_t max_history_size() const { return 500; }
    size_t history_size() const { return m_history.size(); }

    void inject_string(const StringView&);
    void handle_key_press(KeyCode, u32, u8 flags);

    Attribute attribute_at(const Position&) const;

private:
    typedef Vector<unsigned, 4> ParamVector;

    void on_code_point(u32);

    void scroll_up();
    void scroll_down();
    void newline();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u32 ch);
    void set_window_title(const String&);

    void unimplemented_escape();
    void unimplemented_xterm_escape();

    void emit_string(const StringView&);

    void alter_mode(bool should_set, bool question_param, const ParamVector&);

    void CUU(const ParamVector&);
    void CUD(const ParamVector&);
    void CUF(const ParamVector&);
    void CUB(const ParamVector&);
    void CUP(const ParamVector&);
    void ED(const ParamVector&);
    void EL(const ParamVector&);
    void escape$M(const ParamVector&);
    void escape$P(const ParamVector&);
    void escape$G(const ParamVector&);
    void escape$X(const ParamVector&);
    void escape$b(const ParamVector&);
    void escape$d(const ParamVector&);
    void SGR(const ParamVector&);
    void escape$s(const ParamVector&);
    void escape$u(const ParamVector&);
    void escape$t(const ParamVector&);
    void DECSTBM(const ParamVector&);
    void escape$S(const ParamVector&);
    void escape$T(const ParamVector&);
    void escape$L(const ParamVector&);
    void RM(bool question_param, const ParamVector&);
    void SM(bool question_param, const ParamVector&);
    void DA(const ParamVector&);
    void HVP(const ParamVector&);
    void NEL();
    void IND();
    void RI();
    void DSR(const ParamVector&);

    TerminalClient& m_client;

    size_t m_history_start = 0;
    NonnullOwnPtrVector<Line> m_history;
    void add_line_to_history(NonnullOwnPtr<Line>&& line)
    {
        if (m_history.size() < max_history_size()) {
            ASSERT(m_history_start == 0);
            m_history.append(move(line));
            return;
        }
        m_history.ptr_at(m_history_start) = move(line);
        m_history_start = (m_history_start + 1) % m_history.size();
    }

    NonnullOwnPtrVector<Line> m_lines;

    size_t m_scroll_region_top { 0 };
    size_t m_scroll_region_bottom { 0 };

    u16 m_columns { 1 };
    u16 m_rows { 1 };

    u16 m_cursor_row { 0 };
    u16 m_cursor_column { 0 };
    u16 m_saved_cursor_row { 0 };
    u16 m_saved_cursor_column { 0 };
    bool m_swallow_current { false };
    bool m_stomp { false };

    Attribute m_current_attribute;

    u32 m_next_href_id { 0 };

    void execute_escape_sequence(u8 final);
    void execute_xterm_command();
    void execute_hashtag(u8);

    enum ParserState {
        Normal,
        GotEscape,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
        ExpectHashtagDigit,
        ExpectXtermParameter,
        ExpectStringTerminator,
        UTF8Needs3Bytes,
        UTF8Needs2Bytes,
        UTF8Needs1Byte,
    };

    ParserState m_parser_state { Normal };
    u32 m_parser_code_point { 0 };
    Vector<u8> m_parameters;
    Vector<u8> m_intermediates;
    Vector<u8> m_xterm_parameters;
    Vector<bool> m_horizontal_tabs;
    u8 m_final { 0 };
    u32 m_last_code_point { 0 };
};

}
