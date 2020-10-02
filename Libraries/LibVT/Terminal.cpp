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

#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibVT/Terminal.h>
#include <string.h>

//#define TERMINAL_DEBUG

namespace VT {

Terminal::Terminal(TerminalClient& client)
    : m_client(client)
{
}

Terminal::~Terminal()
{
}

void Terminal::clear()
{
    for (size_t i = 0; i < rows(); ++i)
        m_lines[i].clear(m_current_attribute);
    set_cursor(0, 0);
}

void Terminal::clear_including_history()
{
    m_history.clear();
    m_history_start = 0;

    clear();

    m_client.terminal_history_changed();
}

inline bool is_valid_parameter_character(u8 ch)
{
    return ch >= 0x30 && ch <= 0x3f;
}

inline bool is_valid_intermediate_character(u8 ch)
{
    return ch >= 0x20 && ch <= 0x2f;
}

inline bool is_valid_final_character(u8 ch)
{
    return ch >= 0x40 && ch <= 0x7e;
}

void Terminal::alter_mode(bool should_set, bool question_param, const ParamVector& params)
{
    int mode = 2;
    if (params.size() > 0) {
        mode = params[0];
    }
    if (!question_param) {
        switch (mode) {
            // FIXME: implement *something* for this
        default:
            unimplemented_escape();
            break;
        }
    } else {
        switch (mode) {
        case 25:
            // Hide cursor command, but doesn't need to be run (for now, because
            // we don't do inverse control codes anyways)
            if (should_set)
                dbgprintf("Terminal: Hide Cursor escapecode received. Not needed: ignored.\n");
            else
                dbgprintf("Terminal: Show Cursor escapecode received. Not needed: ignored.\n");
            break;
        default:
            break;
        }
    }
}

void Terminal::RM(bool question_param, const ParamVector& params)
{
    // RM – Reset Mode
    alter_mode(true, question_param, params);
}

void Terminal::SM(bool question_param, const ParamVector& params)
{
    // SM – Set Mode
    alter_mode(false, question_param, params);
}

void Terminal::SGR(const ParamVector& params)
{
    // SGR – Select Graphic Rendition
    if (params.is_empty()) {
        m_current_attribute.reset();
        return;
    }
    if (params.size() >= 3) {
        bool should_set = true;
        auto kind = params[1];
        u32 color = 0;
        switch (kind) {
        case 5: // 8-bit
            color = xterm_colors[params[2]];
            break;
        case 2: // 24-bit
            for (size_t i = 0; i < 3; ++i) {
                u8 component = 0;
                if (params.size() - 2 > i) {
                    component = params[i + 2];
                }
                color <<= 8;
                color |= component;
            }
            break;
        default:
            should_set = false;
            break;
        }

        if (should_set) {
            if (params[0] == 38) {
                m_current_attribute.foreground_color = color;
                return;
            } else if (params[0] == 48) {
                m_current_attribute.background_color = color;
                return;
            }
        }
    }
    for (auto param : params) {
        switch (param) {
        case 0:
            // Reset
            m_current_attribute.reset();
            break;
        case 1:
            m_current_attribute.flags |= Attribute::Bold;
            break;
        case 3:
            m_current_attribute.flags |= Attribute::Italic;
            break;
        case 4:
            m_current_attribute.flags |= Attribute::Underline;
            break;
        case 5:
            m_current_attribute.flags |= Attribute::Blink;
            break;
        case 7:
            m_current_attribute.flags |= Attribute::Negative;
            break;
        case 22:
            m_current_attribute.flags &= ~Attribute::Bold;
            break;
        case 23:
            m_current_attribute.flags &= ~Attribute::Italic;
            break;
        case 24:
            m_current_attribute.flags &= ~Attribute::Underline;
            break;
        case 25:
            m_current_attribute.flags &= ~Attribute::Blink;
            break;
        case 27:
            m_current_attribute.flags &= ~Attribute::Negative;
            break;
        case 30:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
            // Foreground color
            if (m_current_attribute.flags & Attribute::Bold)
                param += 8;
            m_current_attribute.foreground_color = xterm_colors[param - 30];
            break;
        case 39:
            // reset foreground
            m_current_attribute.foreground_color = Attribute::default_foreground_color;
            break;
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
            // Background color
            if (m_current_attribute.flags & Attribute::Bold)
                param += 8;
            m_current_attribute.background_color = xterm_colors[param - 40];
            break;
        case 49:
            // reset background
            m_current_attribute.background_color = Attribute::default_background_color;
            break;
        default:
            dbgprintf("FIXME: SGR: p: %u\n", param);
        }
    }
}

void Terminal::escape$s(const ParamVector&)
{
    m_saved_cursor_row = m_cursor_row;
    m_saved_cursor_column = m_cursor_column;
}

void Terminal::escape$u(const ParamVector&)
{
    set_cursor(m_saved_cursor_row, m_saved_cursor_column);
}

void Terminal::escape$t(const ParamVector& params)
{
    if (params.size() < 1)
        return;
    dbgprintf("FIXME: escape$t: Ps: %u (param count: %d)\n", params[0], params.size());
}

void Terminal::DECSTBM(const ParamVector& params)
{
    // DECSTBM – Set Top and Bottom Margins ("Scrolling Region")
    unsigned top = 1;
    unsigned bottom = m_rows;
    if (params.size() >= 1)
        top = params[0];
    if (params.size() >= 2)
        bottom = params[1];
    if ((bottom - top) < 2 || bottom > m_rows) {
        dbgprintf("Error: DECSTBM: scrolling region invalid: %u-%u\n", top, bottom);
        return;
    }
    m_scroll_region_top = top - 1;
    m_scroll_region_bottom = bottom - 1;
    set_cursor(0, 0);
}

void Terminal::CUP(const ParamVector& params)
{
    // CUP – Cursor Position
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::HVP(const ParamVector& params)
{
    // HVP – Horizontal and Vertical Position
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::CUU(const ParamVector& params)
{
    // CUU – Cursor Up
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    if (num == 0)
        num = 1;
    int new_row = (int)m_cursor_row - num;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, m_cursor_column);
}

void Terminal::CUD(const ParamVector& params)
{
    // CUD – Cursor Down
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    if (num == 0)
        num = 1;
    int new_row = (int)m_cursor_row + num;
    if (new_row >= m_rows)
        new_row = m_rows - 1;
    set_cursor(new_row, m_cursor_column);
}

void Terminal::CUF(const ParamVector& params)
{
    // CUF – Cursor Forward
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    if (num == 0)
        num = 1;
    int new_column = (int)m_cursor_column + num;
    if (new_column >= m_columns)
        new_column = m_columns - 1;
    set_cursor(m_cursor_row, new_column);
}

void Terminal::CUB(const ParamVector& params)
{
    // CUB – Cursor Backward
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    if (num == 0)
        num = 1;
    int new_column = (int)m_cursor_column - num;
    if (new_column < 0)
        new_column = 0;
    set_cursor(m_cursor_row, new_column);
}

void Terminal::escape$G(const ParamVector& params)
{
    int new_column = 1;
    if (params.size() >= 1)
        new_column = params[0] - 1;
    if (new_column < 0)
        new_column = 0;
    set_cursor(m_cursor_row, new_column);
}

void Terminal::escape$b(const ParamVector& params)
{
    if (params.size() < 1)
        return;

    for (unsigned i = 0; i < params[0]; ++i)
        put_character_at(m_cursor_row, m_cursor_column++, m_last_code_point);
}

void Terminal::escape$d(const ParamVector& params)
{
    int new_row = 1;
    if (params.size() >= 1)
        new_row = params[0] - 1;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, m_cursor_column);
}

void Terminal::escape$X(const ParamVector& params)
{
    // Erase characters (without moving cursor)
    int num = 1;
    if (params.size() >= 1)
        num = params[0];
    if (num == 0)
        num = 1;
    // Clear from cursor to end of line.
    for (int i = m_cursor_column; i < num; ++i) {
        put_character_at(m_cursor_row, i, ' ');
    }
}

void Terminal::EL(const ParamVector& params)
{
    int mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // Clear from cursor to end of line.
        for (int i = m_cursor_column; i < m_columns; ++i) {
            put_character_at(m_cursor_row, i, ' ');
        }
        break;
    case 1:
        // Clear from cursor to beginning of line.
        for (int i = 0; i <= m_cursor_column; ++i) {
            put_character_at(m_cursor_row, i, ' ');
        }
        break;
    case 2:
        // Clear the complete line
        for (int i = 0; i < m_columns; ++i) {
            put_character_at(m_cursor_row, i, ' ');
        }
        break;
    default:
        unimplemented_escape();
        break;
    }
}

void Terminal::ED(const ParamVector& params)
{
    // ED - Erase in Display
    int mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // Clear from cursor to end of screen.
        for (int i = m_cursor_column; i < m_columns; ++i)
            put_character_at(m_cursor_row, i, ' ');
        for (int row = m_cursor_row + 1; row < m_rows; ++row) {
            for (int column = 0; column < m_columns; ++column) {
                put_character_at(row, column, ' ');
            }
        }
        break;
    case 1:
        // Clear from cursor to beginning of screen.
        for (int i = m_cursor_column; i >= 0; --i)
            put_character_at(m_cursor_row, i, ' ');
        for (int row = m_cursor_row - 1; row >= 0; --row) {
            for (int column = 0; column < m_columns; ++column) {
                put_character_at(row, column, ' ');
            }
        }
        break;
    case 2:
        clear();
        break;
    case 3:
        // FIXME: <esc>[3J should also clear the scrollback buffer.
        clear();
        break;
    default:
        unimplemented_escape();
        break;
    }
}

void Terminal::escape$S(const ParamVector& params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];

    for (u16 i = 0; i < count; i++)
        scroll_up();
}

void Terminal::escape$T(const ParamVector& params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];

    for (u16 i = 0; i < count; i++)
        scroll_down();
}

void Terminal::escape$L(const ParamVector& params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];
    invalidate_cursor();
    for (; count > 0; --count) {
        m_lines.insert(m_cursor_row + m_scroll_region_top, make<Line>(m_columns));
        if (m_scroll_region_bottom + 1 < m_lines.size())
            m_lines.remove(m_scroll_region_bottom + 1);
        else
            m_lines.remove(m_lines.size() - 1);
    }

    m_need_full_flush = true;
}

void Terminal::DA(const ParamVector&)
{
    // DA - Device Attributes
    emit_string("\033[?1;0c");
}

void Terminal::escape$M(const ParamVector& params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];

    if (count == 1 && m_cursor_row == 0) {
        scroll_up();
        return;
    }

    int max_count = m_rows - (m_scroll_region_top + m_cursor_row);
    count = min(count, max_count);

    for (int c = count; c > 0; --c) {
        m_lines.remove(m_cursor_row + m_scroll_region_top);
        if (m_scroll_region_bottom < m_lines.size())
            m_lines.insert(m_scroll_region_bottom, make<Line>(m_columns));
        else
            m_lines.append(make<Line>(m_columns));
    }
}

void Terminal::escape$P(const ParamVector& params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];

    if (num == 0)
        num = 1;

    auto& line = m_lines[m_cursor_row];

    // Move n characters of line to the left
    for (int i = m_cursor_column; i < line.length() - num; i++)
        line.set_code_point(i, line.code_point(i + num));

    // Fill remainder of line with blanks
    for (int i = line.length() - num; i < line.length(); i++)
        line.set_code_point(i, ' ');

    line.set_dirty(true);
}

void Terminal::execute_xterm_command()
{
    ParamVector numeric_params;
    auto param_string = String::copy(m_xterm_parameters);
    auto params = param_string.split(';', true);
    m_xterm_parameters.clear_with_capacity();
    for (auto& parampart : params)
        numeric_params.append(parampart.to_uint().value_or(0));

    while (params.size() < 3) {
        params.append(String::empty());
        numeric_params.append(0);
    }

    m_final = '@';

    if (numeric_params.is_empty()) {
        dbg() << "Empty Xterm params?";
        return;
    }

    switch (numeric_params[0]) {
    case 0:
    case 1:
    case 2:
        m_client.set_window_title(params[1]);
        break;
    case 8:
        if (params[2].is_empty()) {
            m_current_attribute.href = String();
            m_current_attribute.href_id = String();
        } else {
            m_current_attribute.href = params[2];
            // FIXME: Respect the provided ID
            m_current_attribute.href_id = String::format("%u", m_next_href_id++);
        }
        break;
    case 9:
        m_client.set_window_progress(numeric_params[1], numeric_params[2]);
        break;
    default:
        unimplemented_xterm_escape();
        break;
    }
}

void Terminal::execute_escape_sequence(u8 final)
{
    bool question_param = false;
    m_final = final;
    ParamVector params;

    if (m_parameters.size() > 0 && m_parameters[0] == '?') {
        question_param = true;
        m_parameters.remove(0);
    }
    auto paramparts = String::copy(m_parameters).split(';');
    for (auto& parampart : paramparts) {
        auto value = parampart.to_uint();
        if (!value.has_value()) {
            // FIXME: Should we do something else?
            m_parameters.clear_with_capacity();
            m_intermediates.clear_with_capacity();
            return;
        }
        params.append(value.value());
    }

#if defined(TERMINAL_DEBUG)
    dbgprintf("Terminal::execute_escape_sequence: Handled final '%c'\n", final);
    dbgprintf("Params: ");
    for (auto& p : params) {
        dbgprintf("%d ", p);
    }
    dbgprintf("\b\n");
#endif

    switch (final) {
    case 'A':
        CUU(params);
        break;
    case 'B':
        CUD(params);
        break;
    case 'C':
        CUF(params);
        break;
    case 'D':
        CUB(params);
        break;
    case 'H':
        CUP(params);
        break;
    case 'J':
        ED(params);
        break;
    case 'K':
        EL(params);
        break;
    case 'M':
        escape$M(params);
        break;
    case 'P':
        escape$P(params);
        break;
    case 'S':
        escape$S(params);
        break;
    case 'T':
        escape$T(params);
        break;
    case 'L':
        escape$L(params);
        break;
    case 'G':
        escape$G(params);
        break;
    case 'X':
        escape$X(params);
        break;
    case 'b':
        escape$b(params);
        break;
    case 'd':
        escape$d(params);
        break;
    case 'm':
        SGR(params);
        break;
    case 's':
        escape$s(params);
        break;
    case 'u':
        escape$u(params);
        break;
    case 't':
        escape$t(params);
        break;
    case 'r':
        DECSTBM(params);
        break;
    case 'l':
        RM(question_param, params);
        break;
    case 'h':
        SM(question_param, params);
        break;
    case 'c':
        DA(params);
        break;
    case 'f':
        HVP(params);
        break;
    case 'n':
        DSR(params);
        break;
    default:
        dbgprintf("Terminal::execute_escape_sequence: Unhandled final '%c'\n", final);
        break;
    }

#if defined(TERMINAL_DEBUG)
    dbgprintf("\n");
    for (auto& line : m_lines) {
        dbgprintf("Terminal: Line: ");
        for (int i = 0; i < line.length(); i++) {
            u32 codepoint = line.code_point(i);
            if (codepoint < 128)
                dbgprintf("%c", (char)codepoint);
            else
                dbgprintf("<U+%04x>", codepoint);
        }
        dbgprintf("\n");
    }
#endif

    m_parameters.clear_with_capacity();
    m_intermediates.clear_with_capacity();
}

void Terminal::newline()
{
    u16 new_row = m_cursor_row;
    if (m_cursor_row == m_scroll_region_bottom) {
        scroll_up();
    } else {
        ++new_row;
    }
    set_cursor(new_row, 0);
}

void Terminal::scroll_up()
{
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();
    if (m_scroll_region_top == 0) {
        auto line = move(m_lines.ptr_at(m_scroll_region_top));
        add_line_to_history(move(line));
        m_client.terminal_history_changed();
    }
    m_lines.remove(m_scroll_region_top);
    m_lines.insert(m_scroll_region_bottom, make<Line>(m_columns));
    m_need_full_flush = true;
}

void Terminal::scroll_down()
{
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();
    m_lines.remove(m_scroll_region_bottom);
    m_lines.insert(m_scroll_region_top, make<Line>(m_columns));
    m_need_full_flush = true;
}

void Terminal::set_cursor(unsigned a_row, unsigned a_column)
{
    unsigned row = min(a_row, m_rows - 1u);
    unsigned column = min(a_column, m_columns - 1u);
    if (row == m_cursor_row && column == m_cursor_column)
        return;
    ASSERT(row < rows());
    ASSERT(column < columns());
    invalidate_cursor();
    m_cursor_row = row;
    m_cursor_column = column;
    m_stomp = false;
    invalidate_cursor();
}

void Terminal::put_character_at(unsigned row, unsigned column, u32 code_point)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    auto& line = m_lines[row];
    line.set_code_point(column, code_point);
    line.attributes()[column] = m_current_attribute;
    line.attributes()[column].flags |= Attribute::Touched;
    line.set_dirty(true);

    m_last_code_point = code_point;
}

void Terminal::NEL()
{
    // NEL - Next Line
    newline();
}

void Terminal::IND()
{
    // IND - Index (move down)
    CUD({});
}

void Terminal::RI()
{
    // RI - Reverse Index (move up)
    CUU({});
}

void Terminal::DSR(const ParamVector& params)
{
    if (params.size() == 1 && params[0] == 5) {
        // Device status
        emit_string("\033[0n"); // Terminal status OK!
    } else if (params.size() == 1 && params[0] == 6) {
        // Cursor position query
        emit_string(String::format("\033[%d;%dR", m_cursor_row + 1, m_cursor_column + 1));
    } else {
        dbg() << "Unknown DSR";
    }
}

void Terminal::on_input(u8 ch)
{
#ifdef TERMINAL_DEBUG
    dbgprintf("Terminal::on_char: %b (%c), fg=%u, bg=%u\n", ch, ch, m_current_attribute.foreground_color, m_current_attribute.background_color);
#endif

    auto fail_utf8_parse = [this] {
        m_parser_state = Normal;
        on_code_point('%');
    };

    auto advance_utf8_parse = [this, ch] {
        m_parser_code_point <<= 6;
        m_parser_code_point |= ch & 0x3f;
        if (m_parser_state == UTF8Needs1Byte) {
            on_code_point(m_parser_code_point);
            m_parser_state = Normal;
        } else {
            m_parser_state = (ParserState)(m_parser_state + 1);
        }
    };

    switch (m_parser_state) {
    case GotEscape:
        if (ch == '[') {
            m_parser_state = ExpectParameter;
        } else if (ch == '(') {
            m_swallow_current = true;
            m_parser_state = ExpectParameter;
        } else if (ch == ']') {
            m_parser_state = ExpectXtermParameter;
            m_xterm_parameters.clear_with_capacity();
        } else if (ch == '#') {
            m_parser_state = ExpectHashtagDigit;
        } else if (ch == 'D') {
            IND();
            m_parser_state = Normal;
            return;
        } else if (ch == 'M') {
            RI();
            m_parser_state = Normal;
            return;
        } else if (ch == 'E') {
            NEL();
            m_parser_state = Normal;
            return;
        } else {
            dbg() << "Unexpected character in GotEscape '" << (char)ch << "'";
            m_parser_state = Normal;
        }
        return;
    case ExpectHashtagDigit:
        if (ch >= '0' && ch <= '9') {
            execute_hashtag(ch);
            m_parser_state = Normal;
        }
        return;
    case ExpectXtermParameter:
        if (ch == 27) {
            m_parser_state = ExpectStringTerminator;
            return;
        }
        if (ch == 7) {
            execute_xterm_command();
            m_parser_state = Normal;
            return;
        }
        m_xterm_parameters.append(ch);
        return;
    case ExpectStringTerminator:
        if (ch == '\\')
            execute_xterm_command();
        else
            dbg() << "Unexpected string terminator: " << String::format("%02x", ch);
        m_parser_state = Normal;
        return;
    case ExpectParameter:
        if (is_valid_parameter_character(ch)) {
            m_parameters.append(ch);
            return;
        }
        m_parser_state = ExpectIntermediate;
        [[fallthrough]];
    case ExpectIntermediate:
        if (is_valid_intermediate_character(ch)) {
            m_intermediates.append(ch);
            return;
        }
        m_parser_state = ExpectFinal;
        [[fallthrough]];
    case ExpectFinal:
        if (is_valid_final_character(ch)) {
            m_parser_state = Normal;
            if (!m_swallow_current)
                execute_escape_sequence(ch);
            m_swallow_current = false;
            return;
        }
        m_parser_state = Normal;
        m_swallow_current = false;
        return;
    case UTF8Needs1Byte:
    case UTF8Needs2Bytes:
    case UTF8Needs3Bytes:
        if ((ch & 0xc0) != 0x80) {
            fail_utf8_parse();
        } else {
            advance_utf8_parse();
        }
        return;

    case Normal:
        if (!(ch & 0x80))
            break;
        if ((ch & 0xe0) == 0xc0) {
            m_parser_state = UTF8Needs1Byte;
            m_parser_code_point = ch & 0x1f;
            return;
        }
        if ((ch & 0xf0) == 0xe0) {
            m_parser_state = UTF8Needs2Bytes;
            m_parser_code_point = ch & 0x0f;
            return;
        }
        if ((ch & 0xf8) == 0xf0) {
            m_parser_state = UTF8Needs3Bytes;
            m_parser_code_point = ch & 0x07;
            return;
        }
        fail_utf8_parse();
        return;
    }

    switch (ch) {
    case '\0':
        return;
    case '\033':
        m_parser_state = GotEscape;
        m_swallow_current = false;
        return;
    case 8: // Backspace
        if (m_cursor_column) {
            set_cursor(m_cursor_row, m_cursor_column - 1);
            return;
        }
        return;
    case '\a':
        m_client.beep();
        return;
    case '\t': {
        for (unsigned i = m_cursor_column + 1; i < columns(); ++i) {
            if (m_horizontal_tabs[i]) {
                set_cursor(m_cursor_row, i);
                return;
            }
        }
        return;
    }
    case '\r':
        set_cursor(m_cursor_row, 0);
        return;
    case '\n':
        newline();
        return;
    }

    on_code_point(ch);
}

void Terminal::on_code_point(u32 code_point)
{
    auto new_column = m_cursor_column + 1;
    if (new_column < columns()) {
        put_character_at(m_cursor_row, m_cursor_column, code_point);
        set_cursor(m_cursor_row, new_column);
        return;
    }
    if (m_stomp) {
        m_stomp = false;
        newline();
        put_character_at(m_cursor_row, m_cursor_column, code_point);
        set_cursor(m_cursor_row, 1);
    } else {
        // Curious: We wait once on the right-hand side
        m_stomp = true;
        put_character_at(m_cursor_row, m_cursor_column, code_point);
    }
}

void Terminal::inject_string(const StringView& str)
{
    for (size_t i = 0; i < str.length(); ++i)
        on_input(str[i]);
}

void Terminal::emit_string(const StringView& string)
{
    m_client.emit((const u8*)string.characters_without_null_termination(), string.length());
}

void Terminal::handle_key_press(KeyCode key, u32 code_point, u8 flags)
{
    bool ctrl = flags & Mod_Ctrl;
    bool alt = flags & Mod_Alt;
    bool shift = flags & Mod_Shift;
    unsigned modifier_mask = int(shift) + (int(alt) << 1) + (int(ctrl) << 2);

    auto emit_final_with_modifier = [this, modifier_mask](char final) {
        if (modifier_mask)
            emit_string(String::format("\e[1;%d%c", modifier_mask + 1, final));
        else
            emit_string(String::format("\e[%c", final));
    };
    auto emit_tilde_with_modifier = [this, modifier_mask](unsigned num) {
        if (modifier_mask)
            emit_string(String::format("\e[%d;%d~", num, modifier_mask + 1));
        else
            emit_string(String::format("\e[%d~", num));
    };

    switch (key) {
    case KeyCode::Key_Up:
        emit_final_with_modifier('A');
        return;
    case KeyCode::Key_Down:
        emit_final_with_modifier('B');
        return;
    case KeyCode::Key_Right:
        emit_final_with_modifier('C');
        return;
    case KeyCode::Key_Left:
        emit_final_with_modifier('D');
        return;
    case KeyCode::Key_Insert:
        emit_tilde_with_modifier(2);
        return;
    case KeyCode::Key_Delete:
        emit_tilde_with_modifier(3);
        return;
    case KeyCode::Key_Home:
        emit_final_with_modifier('H');
        return;
    case KeyCode::Key_End:
        emit_final_with_modifier('F');
        return;
    case KeyCode::Key_PageUp:
        emit_tilde_with_modifier(5);
        return;
    case KeyCode::Key_PageDown:
        emit_tilde_with_modifier(6);
        return;
    default:
        break;
    }

    if (!code_point) {
        // Probably a modifier being pressed.
        return;
    }

    if (shift && key == KeyCode::Key_Tab) {
        emit_string("\033[Z");
        return;
    }

    // Key event was not one of the above special cases,
    // attempt to treat it as a character...
    if (ctrl) {
        if (code_point >= 'a' && code_point <= 'z') {
            code_point = code_point - 'a' + 1;
        } else if (code_point == '\\') {
            code_point = 0x1c;
        }
    }

    // Alt modifier sends escape prefix.
    if (alt)
        emit_string("\033");

    StringBuilder sb;
    sb.append_code_point(code_point);

    emit_string(sb.to_string());
}

void Terminal::unimplemented_escape()
{
    StringBuilder builder;
    builder.appendf("((Unimplemented escape: %c", m_final);
    if (!m_parameters.is_empty()) {
        builder.append(" parameters:");
        for (size_t i = 0; i < m_parameters.size(); ++i)
            builder.append((char)m_parameters[i]);
    }
    if (!m_intermediates.is_empty()) {
        builder.append(" intermediates:");
        for (size_t i = 0; i < m_intermediates.size(); ++i)
            builder.append((char)m_intermediates[i]);
    }
    builder.append("))");
    inject_string(builder.to_string());
}

void Terminal::unimplemented_xterm_escape()
{
    auto message = String::format("((Unimplemented xterm escape: %c))\n", m_final);
    inject_string(message);
}

void Terminal::set_size(u16 columns, u16 rows)
{
    if (!columns)
        columns = 1;
    if (!rows)
        rows = 1;

    if (columns == m_columns && rows == m_rows)
        return;

#if defined(TERMINAL_DEBUG)
    dbgprintf("Terminal: RESIZE to: %d rows\n", rows);
#endif

    if (rows > m_rows) {
        while (m_lines.size() < rows)
            m_lines.append(make<Line>(columns));
    } else {
        m_lines.shrink(rows);
    }

    for (int i = 0; i < rows; ++i)
        m_lines[i].set_length(columns);

    m_columns = columns;
    m_rows = rows;

    m_scroll_region_top = 0;
    m_scroll_region_bottom = rows - 1;

    m_cursor_row = min((int)m_cursor_row, m_rows - 1);
    m_cursor_column = min((int)m_cursor_column, m_columns - 1);
    m_saved_cursor_row = min((int)m_saved_cursor_row, m_rows - 1);
    m_saved_cursor_column = min((int)m_saved_cursor_column, m_columns - 1);

    m_horizontal_tabs.resize(columns);
    for (unsigned i = 0; i < columns; ++i)
        m_horizontal_tabs[i] = (i % 8) == 0;
    // Rightmost column is always last tab on line.
    m_horizontal_tabs[columns - 1] = 1;

    m_client.terminal_did_resize(m_columns, m_rows);
}

void Terminal::invalidate_cursor()
{
    m_lines[m_cursor_row].set_dirty(true);
}

void Terminal::execute_hashtag(u8 hashtag)
{
    switch (hashtag) {
    case '8':
        // Confidence Test - Fill screen with E's
        for (size_t row = 0; row < m_rows; ++row) {
            for (size_t column = 0; column < m_columns; ++column) {
                put_character_at(row, column, 'E');
            }
        }
        break;
    default:
        dbg() << "Unknown hashtag: '" << hashtag << "'";
    }
}

Attribute Terminal::attribute_at(const Position& position) const
{
    if (!position.is_valid())
        return {};
    if (position.row() >= static_cast<int>(line_count()))
        return {};
    auto& line = this->line(position.row());
    if (position.column() >= line.length())
        return {};
    return line.attributes()[position.column()];
}

}
