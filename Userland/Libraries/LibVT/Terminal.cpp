/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Terminal.h"
#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibVT/Terminal.h>
#ifdef KERNEL
#    include <Kernel/TTY/VirtualConsole.h>
#endif

namespace VT {

#ifndef KERNEL
Terminal::Terminal(TerminalClient& client)
#else
Terminal::Terminal(Kernel::VirtualConsole& client)
#endif
    : m_client(client)
    , m_parser(*this)
{
}

#ifndef KERNEL
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
#endif

void Terminal::alter_mode(bool should_set, Parameters params, Intermediates intermediates)
{
    auto steady_cursor_to_blinking = [](CursorStyle style) {
        switch (style) {
        case SteadyBar:
            return BlinkingBar;
        case SteadyBlock:
            return BlinkingBlock;
        case SteadyUnderline:
            return BlinkingUnderline;
        default:
            return style;
        }
    };

    auto blinking_cursor_to_steady = [](CursorStyle style) {
        switch (style) {
        case BlinkingBar:
            return SteadyBar;
        case BlinkingBlock:
            return SteadyBlock;
        case BlinkingUnderline:
            return SteadyUnderline;
        default:
            return style;
        }
    };

    if (intermediates.size() > 0 && intermediates[0] == '?') {
        for (auto mode : params) {
            switch (mode) {
            case 3: {
                // 80/132-column mode (DECCOLM)
                unsigned new_columns = should_set ? 132 : 80;
                dbgln_if(TERMINAL_DEBUG, "Setting {}-column mode", new_columns);
                set_size(new_columns, rows());
                clear();
                break;
            }
            case 12:
                if (should_set) {
                    // Start blinking cursor
                    m_cursor_style = steady_cursor_to_blinking(m_cursor_style);
                } else {
                    // Stop blinking cursor
                    m_cursor_style = blinking_cursor_to_steady(m_cursor_style);
                }
                m_client.set_cursor_style(m_cursor_style);
                break;
            case 25:
                if (should_set) {
                    // Show cursor
                    m_cursor_style = m_saved_cursor_style;
                    m_client.set_cursor_style(m_cursor_style);
                } else {
                    // Hide cursor
                    m_saved_cursor_style = m_cursor_style;
                    m_cursor_style = None;
                    m_client.set_cursor_style(None);
                }
                break;
            default:
                dbgln("Terminal::alter_mode: Unimplemented private mode {} (should_set={})", mode, should_set);
                break;
            }
        }
    } else {
        for (auto mode : params) {
            switch (mode) {
            // FIXME: implement *something* for this
            default:
                dbgln("Terminal::alter_mode: Unimplemented mode {} (should_set={})", mode, should_set);
                break;
            }
        }
    }
}

void Terminal::RM(Parameters params, Intermediates intermediates)
{
    alter_mode(false, params, intermediates);
}

void Terminal::SM(Parameters params, Intermediates intermediates)
{
    alter_mode(true, params, intermediates);
}

void Terminal::SGR(Parameters params)
{
    if (params.is_empty()) {
        m_current_attribute.reset();
        return;
    }

    auto parse_color = [&]() -> Optional<u32> {
        if (params.size() < 2) {
            dbgln("Color code has no type");
            return {};
        }
        u32 color = 0;
        switch (params[1]) {
        case 5: // 8-bit
            if (params.size() < 3) {
                dbgln("8-bit color code has too few parameters");
                return {};
            }
            return xterm_colors[params[2]];
        case 2: // 24-bit
            if (params.size() < 5) {
                dbgln("24-bit color code has too few parameters");
                return {};
            }
            for (size_t i = 0; i < 3; ++i) {
                color <<= 8;
                color |= params[i + 2];
            }
            return color;
        default:
            dbgln("Unknown color type {}", params[1]);
            return {};
        }
    };

    if (params[0] == 38) {
        m_current_attribute.foreground_color = parse_color().value_or(m_current_attribute.foreground_color);
    } else if (params[0] == 48) {
        m_current_attribute.background_color = parse_color().value_or(m_current_attribute.background_color);
    } else {
        // A single escape sequence may set multiple parameters.
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
                dbgln("FIXME: SGR: p: {}", param);
            }
        }
    }
}

void Terminal::SCOSC()
{
    m_saved_cursor_row = m_cursor_row;
    m_saved_cursor_column = m_cursor_column;
    m_saved_attribute = m_current_attribute;
}

void Terminal::SCORC(Parameters)
{
    set_cursor(m_saved_cursor_row, m_saved_cursor_column);
}

void Terminal::XTERM_WM(Parameters params)
{
    if (params.size() < 1)
        return;
    dbgln("FIXME: XTERM_WM: Ps: {} (param count: {})", params[0], params.size());
}

void Terminal::DECSTBM(Parameters params)
{
    unsigned top = 1;
    unsigned bottom = m_rows;
    if (params.size() >= 1)
        top = params[0];
    if (params.size() >= 2)
        bottom = params[1];
    if ((bottom - top) < 2 || bottom > m_rows) {
        dbgln("Error: DECSTBM: scrolling region invalid: {}-{}", top, bottom);
        return;
    }
    m_scroll_region_top = top - 1;
    m_scroll_region_bottom = bottom - 1;
    set_cursor(0, 0);
}

void Terminal::CUP(Parameters params)
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

void Terminal::HVP(Parameters params)
{
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::CUU(Parameters params)
{
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

void Terminal::CUD(Parameters params)
{
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

void Terminal::CUF(Parameters params)
{
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

void Terminal::CUB(Parameters params)
{
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

void Terminal::CHA(Parameters params)
{
    int new_column = 1;
    if (params.size() >= 1)
        new_column = params[0] - 1;
    if (new_column < 0)
        new_column = 0;
    set_cursor(m_cursor_row, new_column);
}

void Terminal::REP(Parameters params)
{
    if (params.size() < 1)
        return;

    for (unsigned i = 0; i < params[0]; ++i)
        put_character_at(m_cursor_row, m_cursor_column++, m_last_code_point);
}

void Terminal::VPA(Parameters params)
{
    int new_row = 1;
    if (params.size() >= 1)
        new_row = params[0] - 1;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, m_cursor_column);
}

void Terminal::ECH(Parameters params)
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

void Terminal::EL(Parameters params)
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
        unimplemented_csi_sequence(params, {}, 'K');
        break;
    }
}

void Terminal::ED(Parameters params)
{
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
        unimplemented_csi_sequence(params, {}, 'J');
        break;
    }
}

void Terminal::SU(Parameters params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];

    for (u16 i = 0; i < count; i++)
        scroll_up();
}

void Terminal::SD(Parameters params)
{
    int count = 1;
    if (params.size() >= 1)
        count = params[0];

    for (u16 i = 0; i < count; i++)
        scroll_down();
}

void Terminal::DECSCUSR(Parameters params)
{
    unsigned style = 1;
    if (params.size() >= 1 && params[0] != 0)
        style = params[0];
    switch (style) {
    case 1:
        m_client.set_cursor_style(BlinkingBlock);
        break;
    case 2:
        m_client.set_cursor_style(SteadyBlock);
        break;
    case 3:
        m_client.set_cursor_style(BlinkingUnderline);
        break;
    case 4:
        m_client.set_cursor_style(SteadyUnderline);
        break;
    case 5:
        m_client.set_cursor_style(BlinkingBar);
        break;
    case 6:
        m_client.set_cursor_style(SteadyBar);
        break;
    default:
        dbgln("Unknown cursor style {}", style);
    }
}

#ifndef KERNEL
void Terminal::IL(Parameters params)
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
#endif

void Terminal::DA(Parameters)
{
    emit_string("\033[?1;0c");
}

#ifndef KERNEL
void Terminal::DL(Parameters params)
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

void Terminal::DCH(Parameters params)
{
    int num = 1;
    if (params.size() >= 1)
        num = params[0];

    if (num == 0)
        num = 1;

    auto& line = m_lines[m_cursor_row];

    // Move n characters of line to the left
    for (size_t i = m_cursor_column; i < line.length() - num; i++)
        line.set_code_point(i, line.code_point(i + num));

    // Fill remainder of line with blanks
    for (size_t i = line.length() - num; i < line.length(); i++)
        line.set_code_point(i, ' ');

    line.set_dirty(true);
}
#endif

void Terminal::linefeed()
{
    u16 new_row = m_cursor_row;
    if (m_cursor_row == m_scroll_region_bottom) {
        scroll_up();
    } else {
        ++new_row;
    };
    // We shouldn't jump to the first column after receiving a line feed.
    // The TTY will take care of generating the carriage return.
    set_cursor(new_row, m_cursor_column);
}

void Terminal::carriage_return()
{
    set_cursor(m_cursor_row, 0);
}

#ifndef KERNEL
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

void Terminal::put_character_at(unsigned row, unsigned column, u32 code_point)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    auto& line = m_lines[row];
    line.set_code_point(column, code_point);
    line.attribute_at(column) = m_current_attribute;
    line.attribute_at(column).flags |= Attribute::Touched;
    line.set_dirty(true);

    m_last_code_point = code_point;
}
#endif

void Terminal::set_cursor(unsigned a_row, unsigned a_column)
{
    unsigned row = min(a_row, m_rows - 1u);
    unsigned column = min(a_column, m_columns - 1u);
    if (row == m_cursor_row && column == m_cursor_column)
        return;
    VERIFY(row < rows());
    VERIFY(column < columns());
    invalidate_cursor();
    m_cursor_row = row;
    m_cursor_column = column;
    m_stomp = false;
    invalidate_cursor();
}

void Terminal::NEL()
{
    linefeed();
    carriage_return();
}

void Terminal::IND()
{
    CUD({});
}

void Terminal::RI()
{
    CUU({});
}

void Terminal::DSR(Parameters params)
{
    if (params.size() == 1 && params[0] == 5) {
        // Device status
        emit_string("\033[0n"); // Terminal status OK!
    } else if (params.size() == 1 && params[0] == 6) {
        // Cursor position query
        emit_string(String::formatted("\e[{};{}R", m_cursor_row + 1, m_cursor_column + 1));
    } else {
        dbgln("Unknown DSR");
    }
}

#ifndef KERNEL
void Terminal::ICH(Parameters params)
{
    int num = 0;
    if (params.size() >= 1) {
        num = params[0];
    }
    if (num == 0)
        num = 1;

    auto& line = m_lines[m_cursor_row];

    // Move characters after cursor to the right
    for (int i = line.length() - num; i >= m_cursor_column; --i)
        line.set_code_point(i + num, line.code_point(i));

    // Fill n characters after cursor with blanks
    for (int i = 0; i < num; i++)
        line.set_code_point(m_cursor_column + i, ' ');

    line.set_dirty(true);
}
#endif

void Terminal::on_input(u8 byte)
{
    m_parser.on_input(byte);
}

void Terminal::emit_code_point(u32 code_point)
{
    auto new_column = m_cursor_column + 1;
    if (new_column < columns()) {
        put_character_at(m_cursor_row, m_cursor_column, code_point);
        set_cursor(m_cursor_row, new_column);
        return;
    }
    if (m_stomp) {
        m_stomp = false;
        carriage_return();
        linefeed();
        put_character_at(m_cursor_row, m_cursor_column, code_point);
        set_cursor(m_cursor_row, 1);
    } else {
        // Curious: We wait once on the right-hand side
        m_stomp = true;
        put_character_at(m_cursor_row, m_cursor_column, code_point);
    }
}

void Terminal::execute_control_code(u8 code)
{
    switch (code) {
    case '\a':
        m_client.beep();
        return;
    case '\b':
        if (m_cursor_column) {
            set_cursor(m_cursor_row, m_cursor_column - 1);
            return;
        }
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
    case '\n':
    case '\v':
    case '\f':
        linefeed();
        return;
    case '\r':
        carriage_return();
        return;
    default:
        unimplemented_control_code(code);
    }
}

void Terminal::execute_escape_sequence(Intermediates intermediates, bool ignore, u8 last_byte)
{
    // FIXME: Handle it somehow?
    if (ignore)
        dbgln("Escape sequence has its ignore flag set.");

    if (intermediates.size() == 0) {
        switch (last_byte) {
        case 'D':
            IND();
            return;
        case 'E':
            NEL();
            return;
        case 'M':
            RI();
            return;
        case '\\':
            // ST (string terminator) -- do nothing
            return;
        }
    } else if (intermediates[0] == '#') {
        switch (last_byte) {
        case '8':
            // Confidence Test - Fill screen with E's
            for (size_t row = 0; row < m_rows; ++row) {
                for (size_t column = 0; column < m_columns; ++column) {
                    put_character_at(row, column, 'E');
                }
            }
            return;
        }
    }
    unimplemented_escape_sequence(intermediates, last_byte);
}

void Terminal::execute_csi_sequence(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte)
{
    // FIXME: Handle it somehow?
    if (ignore)
        dbgln("CSI sequence has its ignore flag set.");

    switch (last_byte) {
    case '@':
        ICH(parameters);
        break;
    case 'A':
        CUU(parameters);
        break;
    case 'B':
        CUD(parameters);
        break;
    case 'C':
        CUF(parameters);
        break;
    case 'D':
        CUB(parameters);
        break;
    case 'G':
        CHA(parameters);
        break;
    case 'H':
        CUP(parameters);
        break;
    case 'J':
        ED(parameters);
        break;
    case 'K':
        EL(parameters);
        break;
    case 'L':
        IL(parameters);
        break;
    case 'M':
        DL(parameters);
        break;
    case 'P':
        DCH(parameters);
        break;
    case 'S':
        SU(parameters);
        break;
    case 'T':
        SD(parameters);
        break;
    case 'X':
        ECH(parameters);
        break;
    case 'b':
        REP(parameters);
        break;
    case 'd':
        VPA(parameters);
        break;
    case 'm':
        SGR(parameters);
        break;
    case 's':
        SCOSC();
        break;
    case 'u':
        SCORC(parameters);
        break;
    case 't':
        XTERM_WM(parameters);
        break;
    case 'r':
        DECSTBM(parameters);
        break;
    case 'l':
        RM(parameters, intermediates);
        break;
    case 'h':
        SM(parameters, intermediates);
        break;
    case 'c':
        DA(parameters);
        break;
    case 'f':
        HVP(parameters);
        break;
    case 'n':
        DSR(parameters);
        break;
    case 'q':
        if (intermediates.size() >= 1 && intermediates[0] == ' ')
            DECSCUSR(parameters);
        else
            unimplemented_csi_sequence(parameters, intermediates, last_byte);
        break;
    default:
        unimplemented_csi_sequence(parameters, intermediates, last_byte);
    }
}

void Terminal::execute_osc_sequence(OscParameters parameters, u8 last_byte)
{
    auto stringview_ify = [&](size_t param_idx) {
        return StringView(parameters[param_idx]);
    };

    if (parameters.size() == 0 || parameters[0].is_empty()) {
        unimplemented_osc_sequence(parameters, last_byte);
        return;
    }

    auto command_number = stringview_ify(0).to_uint();
    if (!command_number.has_value()) {
        unimplemented_osc_sequence(parameters, last_byte);
        return;
    }

    switch (command_number.value()) {
    case 0:
    case 1:
    case 2:
        if (parameters.size() < 2)
            dbgln("Attempted to set window title without any parameters");
        else
            m_client.set_window_title(stringview_ify(1));
        // FIXME: the split breaks titles containing semicolons.
        // Should we expose the raw OSC string from the parser? Or join by semicolon?
        break;
    case 8:
#ifndef KERNEL
        if (parameters.size() < 3) {
            dbgln("Attempted to set href but gave too few parameters");
        } else if (parameters[1].is_empty() && parameters[2].is_empty()) {
            // Clear hyperlink
            m_current_attribute.href = String();
            m_current_attribute.href_id = String();
        } else {
            m_current_attribute.href = stringview_ify(2);
            // FIXME: Respect the provided ID
            m_current_attribute.href_id = String::number(m_next_href_id++);
        }
#endif
        break;
    case 9:
        if (parameters.size() < 2)
            dbgln("Atttempted to set window progress but gave too few parameters");
        else if (parameters.size() == 2)
            m_client.set_window_progress(stringview_ify(1).to_int().value_or(-1), 0);
        else
            m_client.set_window_progress(stringview_ify(1).to_int().value_or(-1), stringview_ify(2).to_int().value_or(0));
        break;
    default:
        unimplemented_osc_sequence(parameters, last_byte);
    }
}

void Terminal::dcs_hook(Parameters, Intermediates, bool, u8)
{
    dbgln("Received DCS parameters, but we don't support it yet");
}

void Terminal::receive_dcs_char(u8 byte)
{
    dbgln_if(TERMINAL_DEBUG, "DCS string character {:c}", byte);
}

void Terminal::execute_dcs_sequence()
{
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
            emit_string(String::formatted("\e[1;{}{:c}", modifier_mask + 1, final));
        else
            emit_string(String::formatted("\e[{:c}", final));
    };
    auto emit_tilde_with_modifier = [this, modifier_mask](unsigned num) {
        if (modifier_mask)
            emit_string(String::formatted("\e[{};{}~", num, modifier_mask + 1));
        else
            emit_string(String::formatted("\e[{}~", num));
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
    case KeyCode::Key_Return:
        // The standard says that CR should be generated by the return key.
        // The TTY will take care of translating it to CR LF for the terminal.
        emit_string("\r");
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

void Terminal::unimplemented_control_code(u8 code)
{
    dbgln("Unimplemented control code {:02x}", code);
}

void Terminal::unimplemented_escape_sequence(Intermediates intermediates, u8 last_byte)
{
    StringBuilder builder;
    builder.appendff("Unimplemented escape sequence {:c}", last_byte);
    if (!intermediates.is_empty()) {
        builder.append(", intermediates: ");
        for (size_t i = 0; i < intermediates.size(); ++i)
            builder.append((char)intermediates[i]);
    }
    dbgln("{}", builder.string_view());
}

void Terminal::unimplemented_csi_sequence(Parameters parameters, Intermediates intermediates, u8 last_byte)
{
    StringBuilder builder;
    builder.appendff("Unimplemented CSI sequence: {:c}", last_byte);
    if (!parameters.is_empty()) {
        builder.append(", parameters: [");
        for (size_t i = 0; i < parameters.size(); ++i)
            builder.appendff("{}{}", (i == 0) ? "" : ", ", parameters[i]);
        builder.append("]");
    }
    if (!intermediates.is_empty()) {
        builder.append(", intermediates:");
        for (size_t i = 0; i < intermediates.size(); ++i)
            builder.append((char)intermediates[i]);
    }
    dbgln("{}", builder.string_view());
}

void Terminal::unimplemented_osc_sequence(OscParameters parameters, u8 last_byte)
{
    StringBuilder builder;
    builder.appendff("Unimplemented OSC sequence parameters: (bel_terminated={}) [ ", last_byte == '\a');
    bool first = true;
    for (auto parameter : parameters) {
        if (!first)
            builder.append(", ");
        builder.append("[");
        for (auto character : parameter)
            builder.append((char)character);
        builder.append("]");
        first = false;
    }

    builder.append(" ]");
    dbgln("{}", builder.string_view());
}

#ifndef KERNEL
void Terminal::set_size(u16 columns, u16 rows)
{
    if (!columns)
        columns = 1;
    if (!rows)
        rows = 1;

    if (columns == m_columns && rows == m_rows)
        return;

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
#endif

#ifndef KERNEL
void Terminal::invalidate_cursor()
{
    m_lines[m_cursor_row].set_dirty(true);
}

Attribute Terminal::attribute_at(const Position& position) const
{
    if (!position.is_valid())
        return {};
    if (position.row() >= static_cast<int>(line_count()))
        return {};
    auto& line = this->line(position.row());
    if (static_cast<size_t>(position.column()) >= line.length())
        return {};
    return line.attribute_at(position.column());
}
#endif

}
