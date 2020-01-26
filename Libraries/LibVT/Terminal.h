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

#include <AK/String.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Vector.h>
#include <LibVT/Position.h>

namespace VT {

class TerminalClient {
public:
    virtual ~TerminalClient() {}

    virtual void beep() = 0;
    virtual void set_window_title(const StringView&) = 0;
    virtual void terminal_did_resize(u16 columns, u16 rows) = 0;
    virtual void terminal_history_changed() = 0;
    virtual void emit_char(u8) = 0;
};

struct Attribute {
    Attribute() { reset(); }

    static const u8 default_foreground_color = 7;
    static const u8 default_background_color = 0;

    void reset()
    {
        foreground_color = default_foreground_color;
        background_color = default_background_color;
        flags = Flags::NoAttributes;
    }
    u8 foreground_color;
    u8 background_color;

    enum Flags : u8 {
        NoAttributes = 0x00,
        Bold = 0x01,
        Italic = 0x02,
        Underline = 0x04,
        Negative = 0x08,
        Blink = 0x10,
        Touched = 0x20,
    };

    bool is_untouched() const { return !(flags & Touched); }

    // TODO: it would be really nice if we had a helper for enums that
    // exposed bit ops for class enums...
    u8 flags = Flags::NoAttributes;

    bool operator==(const Attribute& other) const
    {
        return foreground_color == other.foreground_color && background_color == other.background_color && flags == other.flags;
    }
    bool operator!=(const Attribute& other) const
    {
        return !(*this == other);
    }
};

class Terminal {
public:
    explicit Terminal(TerminalClient&);
    ~Terminal();

    bool m_need_full_flush { false };

    void invalidate_cursor();
    void on_char(u8);

    void clear();
    void set_size(u16 columns, u16 rows);
    u16 columns() const { return m_columns; }
    u16 rows() const { return m_rows; }

    u16 cursor_column() const { return m_cursor_column; }
    u16 cursor_row() const { return m_cursor_row; }

    struct Line {
        explicit Line(u16 columns);
        ~Line();
        void clear(Attribute);
        bool has_only_one_background_color() const;
        void set_length(u16);
        StringView text() const { return { characters, m_length }; }

        u8* characters { nullptr };
        Attribute* attributes { nullptr };
        bool dirty { false };
        u16 m_length { 0 };
    };

    Line& line(size_t index)
    {
        ASSERT(index < m_rows);
        return m_lines[index];
    }
    const Line& line(size_t index) const
    {
        ASSERT(index < m_rows);
        return m_lines[index];
    }

    int max_history_size() const { return 500; }
    const NonnullOwnPtrVector<Line>& history() const { return m_history; }

    void inject_string(const StringView&);

private:
    typedef Vector<unsigned, 4> ParamVector;

    void scroll_up();
    void scroll_down();
    void newline();
    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u8 ch);
    void set_window_title(const String&);

    void unimplemented_escape();
    void unimplemented_xterm_escape();

    void emit_string(const StringView&);

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
    void escape$r(const ParamVector&);
    void escape$S(const ParamVector&);
    void escape$T(const ParamVector&);
    void escape$L(const ParamVector&);
    void escape$h_l(bool, bool, const ParamVector&);
    void escape$c(const ParamVector&);
    void escape$f(const ParamVector&);
    void NEL();
    void IND();
    void RI();

    TerminalClient& m_client;

    NonnullOwnPtrVector<Line> m_history;
    NonnullOwnPtrVector<Line> m_lines;

    int m_scroll_region_top { 0 };
    int m_scroll_region_bottom { 0 };

    u16 m_columns { 1 };
    u16 m_rows { 1 };

    u16 m_cursor_row { 0 };
    u16 m_cursor_column { 0 };
    u16 m_saved_cursor_row { 0 };
    u16 m_saved_cursor_column { 0 };
    bool m_swallow_current { false };
    bool m_stomp { false };

    Attribute m_current_attribute;

    void execute_escape_sequence(u8 final);
    void execute_xterm_command();
    void execute_hashtag(u8);

    enum EscapeState {
        Normal,
        GotEscape,
        ExpectParameter,
        ExpectIntermediate,
        ExpectFinal,
        ExpectHashtagDigit,
        ExpectXtermParameter1,
        ExpectXtermParameter2,
        ExpectXtermFinal,
    };
    EscapeState m_escape_state { Normal };
    Vector<u8> m_parameters;
    Vector<u8> m_intermediates;
    Vector<u8> m_xterm_param1;
    Vector<u8> m_xterm_param2;
    Vector<bool> m_horizontal_tabs;
    u8 m_final { 0 };

    u8 m_last_char { 0 };
};

}
