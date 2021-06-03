/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Terminal.h"
#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <LibVT/Color.h>
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
        active_buffer()[i].clear(m_current_state.attribute);
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
            case 1047:
#ifndef KERNEL
                if (should_set) {
                    dbgln_if(TERMINAL_DEBUG, "Switching to Alternate Screen Buffer");
                    m_use_alternate_screen_buffer = true;
                    clear();
                } else {
                    dbgln_if(TERMINAL_DEBUG, "Switching to Normal Screen Buffer");
                    m_use_alternate_screen_buffer = false;
                }
                m_need_full_flush = true;
#else
                dbgln("Alternate Screen Buffer is not supported");
#endif
                break;
            case 1048:
                if (should_set)
                    SCOSC();
                else
                    SCORC();
                break;
            case 1049:
#ifndef KERNEL
                if (should_set) {
                    dbgln_if(TERMINAL_DEBUG, "Switching to Alternate Screen Buffer and saving state");
                    m_normal_saved_state = m_current_state;
                    m_use_alternate_screen_buffer = true;
                    clear();
                } else {
                    dbgln_if(TERMINAL_DEBUG, "Switching to Normal Screen Buffer and restoring state");
                    m_current_state = m_normal_saved_state;
                    m_use_alternate_screen_buffer = false;
                    set_cursor(cursor_row(), cursor_column());
                }
                m_need_full_flush = true;
#else
                dbgln("Alternate Screen Buffer is not supported");
#endif
                break;
            case 2004:
                dbgln_if(TERMINAL_DEBUG, "Setting bracketed mode enabled={}", should_set);
                m_needs_bracketed_paste = should_set;
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
        m_current_state.attribute.reset();
        return;
    }
    auto parse_color = [&]() -> Optional<Color> {
        if (params.size() < 2) {
            dbgln("Color code has no type");
            return {};
        }
        u32 rgb = 0;
        switch (params[1]) {
        case 5: // 8-bit
            if (params.size() < 3) {
                dbgln("8-bit color code has too few parameters");
                return {};
            }
            if (params[2] > 255) {
                dbgln("8-bit color code has out-of-bounds value");
                return {};
            }
            return Color::indexed(params[2]);
        case 2: // 24-bit
            if (params.size() < 5) {
                dbgln("24-bit color code has too few parameters");
                return {};
            }
            for (size_t i = 0; i < 3; ++i) {
                rgb <<= 8;
                rgb |= params[i + 2];
            }
            return Color::rgb(rgb);
        default:
            dbgln("Unknown color type {}", params[1]);
            return {};
        }
    };

    if (params[0] == 38) {
        m_current_state.attribute.foreground_color = parse_color().value_or(m_current_state.attribute.foreground_color);
    } else if (params[0] == 48) {
        m_current_state.attribute.background_color = parse_color().value_or(m_current_state.attribute.background_color);
    } else {
        // A single escape sequence may set multiple parameters.
        for (auto param : params) {
            switch (param) {
            case 0:
                // Reset
                m_current_state.attribute.reset();
                break;
            case 1:
                m_current_state.attribute.flags |= Attribute::Bold;
                break;
            case 3:
                m_current_state.attribute.flags |= Attribute::Italic;
                break;
            case 4:
                m_current_state.attribute.flags |= Attribute::Underline;
                break;
            case 5:
                m_current_state.attribute.flags |= Attribute::Blink;
                break;
            case 7:
                m_current_state.attribute.flags |= Attribute::Negative;
                break;
            case 22:
                m_current_state.attribute.flags &= ~Attribute::Bold;
                break;
            case 23:
                m_current_state.attribute.flags &= ~Attribute::Italic;
                break;
            case 24:
                m_current_state.attribute.flags &= ~Attribute::Underline;
                break;
            case 25:
                m_current_state.attribute.flags &= ~Attribute::Blink;
                break;
            case 27:
                m_current_state.attribute.flags &= ~Attribute::Negative;
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
                m_current_state.attribute.foreground_color = Color::named(static_cast<Color::ANSIColor>(param - 30));
                break;
            case 39:
                // reset foreground
                m_current_state.attribute.foreground_color = Attribute::default_foreground_color;
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
                m_current_state.attribute.background_color = Color::named(static_cast<Color::ANSIColor>(param - 40));
                break;
            case 49:
                // reset background
                m_current_state.attribute.background_color = Attribute::default_background_color;
                break;
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
                // Bright foreground color
                m_current_state.attribute.foreground_color = Color::named(static_cast<Color::ANSIColor>(8 + param - 90));
                break;
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
                // Bright background color
                m_current_state.attribute.background_color = Color::named(static_cast<Color::ANSIColor>(8 + param - 100));
                break;
            default:
                dbgln("FIXME: SGR: p: {}", param);
            }
        }
    }
}

void Terminal::SCOSC()
{
    dbgln_if(TERMINAL_DEBUG, "Save cursor position");
    m_saved_cursor_position = m_current_state.cursor;
}

void Terminal::SCORC()
{
    dbgln_if(TERMINAL_DEBUG, "Restore cursor position");
    m_current_state.cursor = m_saved_cursor_position;
    set_cursor(cursor_row(), cursor_column());
}

void Terminal::DECSC()
{
    dbgln_if(TERMINAL_DEBUG, "Save cursor (and other state)");
    if (m_use_alternate_screen_buffer) {
        m_alternate_saved_state = m_current_state;
    } else {
        m_normal_saved_state = m_current_state;
    }
}

void Terminal::DECRC()
{
    dbgln_if(TERMINAL_DEBUG, "Restore cursor (and other state)");
    if (m_use_alternate_screen_buffer) {
        m_current_state = m_alternate_saved_state;
    } else {
        m_current_state = m_normal_saved_state;
    }
    set_cursor(cursor_row(), cursor_column());
}

void Terminal::XTERM_WM(Parameters params)
{
    if (params.size() < 1)
        return;
    switch (params[0]) {
    case 22: {
        if (params.size() > 1 && params[1] == 1) {
            dbgln("FIXME: we don't support icon titles");
            return;
        }
        dbgln_if(TERMINAL_DEBUG, "Title stack push: {}", m_current_window_title);
        [[maybe_unused]] auto rc = m_title_stack.try_append(move(m_current_window_title));
        break;
    }
    case 23: {
        if (params.size() > 1 && params[1] == 1)
            return;
        if (m_title_stack.is_empty()) {
            dbgln("Shenanigans: Tried to pop from empty title stack");
            return;
        }
        m_current_window_title = m_title_stack.take_last();
        dbgln_if(TERMINAL_DEBUG, "Title stack pop: {}", m_current_window_title);
        m_client.set_window_title(m_current_window_title);
        break;
    }
    default:
        dbgln("FIXME: XTERM_WM: Ps: {} (param count: {})", params[0], params.size());
    }
}

void Terminal::DECSTBM(Parameters params)
{
    unsigned top = 1;
    unsigned bottom = m_rows;
    if (params.size() >= 1 && params[0] != 0)
        top = params[0];
    if (params.size() >= 2 && params[1] != 0)
        bottom = params[1];
    if ((bottom - top) < 2 || bottom > m_rows) {
        dbgln("Error: DECSTBM: scrolling region invalid: {}-{}", top, bottom);
        return;
    }
    m_scroll_region_top = top - 1;
    m_scroll_region_bottom = bottom - 1;
    set_cursor(0, 0);
    dbgln_if(TERMINAL_DEBUG, "Set scrolling region: {}-{}", m_scroll_region_top, m_scroll_region_bottom);
}

void Terminal::CUP(Parameters params)
{
    // CUP – Cursor Position
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1 && params[0] != 0)
        row = params[0];
    if (params.size() >= 2 && params[1] != 0)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::HVP(Parameters params)
{
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1 && params[0] != 0)
        row = params[0];
    if (params.size() >= 2 && params[1] != 0)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::CUU(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    int new_row = cursor_row() - num;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, cursor_column());
}

void Terminal::CUD(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    unsigned new_row = cursor_row() + num;
    if (new_row >= m_rows)
        new_row = m_rows - 1;
    set_cursor(new_row, cursor_column());
}

void Terminal::CUF(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    unsigned new_column = cursor_column() + num;
    if (new_column >= m_columns)
        new_column = m_columns - 1;
    set_cursor(cursor_row(), new_column);
}

void Terminal::CUB(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    int new_column = (int)cursor_column() - num;
    if (new_column < 0)
        new_column = 0;
    set_cursor(cursor_row(), new_column);
}

void Terminal::CNL(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    unsigned new_row = cursor_row() + num;
    if (new_row >= m_columns)
        new_row = m_columns - 1;
    set_cursor(new_row, 0);
}

void Terminal::CPL(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    int new_row = (int)cursor_row() - num;
    if (new_row < 0)
        new_row = 0;
    set_cursor(new_row, 0);
}

void Terminal::CHA(Parameters params)
{
    unsigned new_column = 1;
    if (params.size() >= 1 && params[0] != 0)
        new_column = params[0];
    if (new_column > m_columns)
        new_column = m_columns;
    set_cursor(cursor_row(), new_column - 1);
}

void Terminal::REP(Parameters params)
{
    unsigned count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];

    for (unsigned i = 0; i < count; ++i)
        put_character_at(m_current_state.cursor.row, m_current_state.cursor.column++, m_last_code_point);
}

void Terminal::VPA(Parameters params)
{
    unsigned new_row = 1;
    if (params.size() >= 1 && params[0] != 0)
        new_row = params[0];
    if (new_row > m_rows)
        new_row = m_rows;
    set_cursor(new_row - 1, cursor_column());
}

void Terminal::VPR(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    int new_row = cursor_row() + num;
    if (new_row >= m_rows)
        new_row = m_rows - 1;
    set_cursor(new_row, cursor_column());
}

void Terminal::HPA(Parameters params)
{
    unsigned new_column = 1;
    if (params.size() >= 1 && params[0] != 0)
        new_column = params[0];
    if (new_column > m_columns)
        new_column = m_columns;
    set_cursor(cursor_row(), new_column - 1);
}

void Terminal::HPR(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    unsigned new_column = cursor_column() + num;
    if (new_column >= m_columns)
        new_column = m_columns - 1;
    set_cursor(cursor_row(), new_column);
}

void Terminal::ECH(Parameters params)
{
    // Erase characters (without moving cursor)
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];
    // Clear from cursor to end of line.
    for (unsigned i = cursor_column(); i < num; ++i) {
        put_character_at(cursor_row(), i, ' ');
    }
}

void Terminal::EL(Parameters params)
{
    unsigned mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // Clear from cursor to end of line.
        for (int i = cursor_column(); i < m_columns; ++i) {
            put_character_at(cursor_row(), i, ' ');
        }
        break;
    case 1:
        // Clear from cursor to beginning of line.
        for (int i = 0; i <= cursor_column(); ++i) {
            put_character_at(cursor_row(), i, ' ');
        }
        break;
    case 2:
        // Clear the complete line
        for (int i = 0; i < m_columns; ++i) {
            put_character_at(cursor_row(), i, ' ');
        }
        break;
    default:
        unimplemented_csi_sequence(params, {}, 'K');
        break;
    }
}

void Terminal::ED(Parameters params)
{
    unsigned mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        // Clear from cursor to end of screen.
        for (int i = cursor_column(); i < m_columns; ++i)
            put_character_at(cursor_row(), i, ' ');
        for (int row = cursor_row() + 1; row < m_rows; ++row) {
            for (int column = 0; column < m_columns; ++column) {
                put_character_at(row, column, ' ');
            }
        }
        break;
    case 1:
        // Clear from cursor to beginning of screen.
        for (int i = cursor_column(); i >= 0; --i)
            put_character_at(cursor_row(), i, ' ');
        for (int row = cursor_row() - 1; row >= 0; --row) {
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
    unsigned count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];

    for (u16 i = 0; i < count; i++)
        scroll_up();
}

void Terminal::SD(Parameters params)
{
    unsigned count = 1;
    if (params.size() >= 1 && params[0] != 0)
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
    unsigned count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];
    invalidate_cursor();
    for (; count > 0; --count) {
        active_buffer().insert(cursor_row(), make<Line>(m_columns));
        if (m_scroll_region_bottom + 1 < active_buffer().size())
            active_buffer().remove(m_scroll_region_bottom + 1);
        else
            active_buffer().remove(active_buffer().size() - 1);
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
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];

    if (count == 1 && cursor_row() == 0) {
        scroll_up();
        return;
    }

    int max_count = m_rows - (m_scroll_region_top + cursor_row());
    count = min(count, max_count);

    for (int c = count; c > 0; --c) {
        active_buffer().remove(cursor_row());
        if (m_scroll_region_bottom < active_buffer().size())
            active_buffer().insert(m_scroll_region_bottom, make<Line>(m_columns));
        else
            active_buffer().append(make<Line>(m_columns));
    }
}

void Terminal::DCH(Parameters params)
{
    int num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    auto& line = active_buffer()[cursor_row()];
    // Move n characters of line to the left
    for (size_t i = cursor_column(); i < line.length() - num; i++)
        line.set_code_point(i, line.code_point(i + num));

    // Fill remainder of line with blanks
    for (size_t i = line.length() - num; i < line.length(); i++)
        line.set_code_point(i, ' ');

    line.set_dirty(true);
}
#endif

void Terminal::linefeed()
{
    u16 new_row = cursor_row();
    if (cursor_row() == m_scroll_region_bottom) {
        scroll_up();
    } else {
        ++new_row;
    };
    // We shouldn't jump to the first column after receiving a line feed.
    // The TTY will take care of generating the carriage return.
    set_cursor(new_row, cursor_column());
}

void Terminal::carriage_return()
{
    set_cursor(cursor_row(), 0);
}

#ifndef KERNEL
void Terminal::scroll_up()
{
    dbgln_if(TERMINAL_DEBUG, "Scroll up 1 line");
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();
    if (m_scroll_region_top == 0 && !m_use_alternate_screen_buffer) {
        auto line = move(active_buffer().ptr_at(m_scroll_region_top));
        add_line_to_history(move(line));
        m_client.terminal_history_changed();
    }
    active_buffer().remove(m_scroll_region_top);
    active_buffer().insert(m_scroll_region_bottom, make<Line>(m_columns));
    m_need_full_flush = true;
}

void Terminal::scroll_down()
{
    dbgln_if(TERMINAL_DEBUG, "Scroll down 1 line");
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();
    active_buffer().remove(m_scroll_region_bottom);
    active_buffer().insert(m_scroll_region_top, make<Line>(m_columns));
    m_need_full_flush = true;
}

void Terminal::put_character_at(unsigned row, unsigned column, u32 code_point)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    auto& line = active_buffer()[row];
    line.set_code_point(column, code_point);
    line.attribute_at(column) = m_current_state.attribute;
    line.attribute_at(column).flags |= Attribute::Touched;
    line.set_dirty(true);

    m_last_code_point = code_point;
}
#endif

void Terminal::set_cursor(unsigned a_row, unsigned a_column, bool skip_debug)
{
    unsigned row = min(a_row, m_rows - 1u);
    unsigned column = min(a_column, m_columns - 1u);
    if (row == cursor_row() && column == cursor_column())
        return;
    VERIFY(row < rows());
    VERIFY(column < columns());
    invalidate_cursor();
    m_current_state.cursor.row = row;
    m_current_state.cursor.column = column;
    m_stomp = false;
    invalidate_cursor();
    if (!skip_debug)
        dbgln_if(TERMINAL_DEBUG, "Set cursor position: {},{}", cursor_row(), cursor_column());
}

void Terminal::NEL()
{
    if (cursor_row() == m_scroll_region_bottom)
        scroll_up();
    else
        set_cursor(cursor_row() + 1, 0);
}

void Terminal::IND()
{
    // Not equivalent to CUD: if we are at the bottom margin, we have to scroll up.
    if (cursor_row() == m_scroll_region_bottom)
        scroll_up();
    else
        set_cursor(cursor_row() + 1, cursor_column());
}

void Terminal::RI()
{
    // Not equivalent to CUU : if we at the top margin , we have to scroll down.
    if (cursor_row() == m_scroll_region_top)
        scroll_down();
    else
        set_cursor(cursor_row() - 1, cursor_column());
}

void Terminal::DSR(Parameters params)
{
    if (params.size() == 1 && params[0] == 5) {
        // Device status
        emit_string("\033[0n"); // Terminal status OK!
    } else if (params.size() == 1 && params[0] == 6) {
        // Cursor position query
        emit_string(String::formatted("\e[{};{}R", cursor_row() + 1, cursor_column() + 1));
    } else {
        dbgln("Unknown DSR");
    }
}

#ifndef KERNEL
void Terminal::ICH(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    auto& line = active_buffer()[cursor_row()];
    // Move characters after cursor to the right
    for (unsigned i = line.length() - num; i >= cursor_column(); --i)
        line.set_code_point(i + num, line.code_point(i));

    // Fill n characters after cursor with blanks
    for (unsigned i = 0; i < num; i++)
        line.set_code_point(cursor_column() + i, ' ');

    line.set_dirty(true);
}
#endif

void Terminal::on_input(u8 byte)
{
    m_parser.on_input(byte);
}

void Terminal::emit_code_point(u32 code_point)
{
    auto new_column = cursor_column() + 1;
    if (new_column < columns()) {
        put_character_at(cursor_row(), cursor_column(), code_point);
        set_cursor(cursor_row(), new_column, true);
        return;
    }
    if (m_stomp) {
        m_stomp = false;
        carriage_return();
        linefeed();
        put_character_at(cursor_row(), cursor_column(), code_point);
        set_cursor(cursor_row(), 1);
    } else {
        // Curious: We wait once on the right-hand side
        m_stomp = true;
        put_character_at(cursor_row(), cursor_column(), code_point);
    }
}

void Terminal::execute_control_code(u8 code)
{
    switch (code) {
    case '\a':
        m_client.beep();
        return;
    case '\b':
        if (cursor_column()) {
            set_cursor(cursor_row(), cursor_column() - 1);
            return;
        }
        return;
    case '\t': {
        for (unsigned i = cursor_column() + 1; i < columns(); ++i) {
            if (m_horizontal_tabs[i]) {
                set_cursor(cursor_row(), i);
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
        case '7':
            DECSC();
            return;
        case '8':
            DECRC();
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
    case 'E':
        CNL(parameters);
        break;
    case 'F':
        CPL(parameters);
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
    case '`':
        HPA(parameters);
        break;
    case 'a':
        HPR(parameters);
        break;
    case 'b':
        REP(parameters);
        break;
    case 'c':
        DA(parameters);
        break;
    case 'd':
        VPA(parameters);
        break;
    case 'e':
        VPR(parameters);
        break;
    case 'f':
        HVP(parameters);
        break;
    case 'h':
        SM(parameters, intermediates);
        break;
    case 'l':
        RM(parameters, intermediates);
        break;
    case 'm':
        SGR(parameters);
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
    case 'r':
        DECSTBM(parameters);
        break;
    case 's':
        SCOSC();
        break;
    case 't':
        XTERM_WM(parameters);
        break;
    case 'u':
        SCORC();
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
        if (parameters.size() < 2) {
            dbgln("Attempted to set window title without any parameters");
        } else {
            // FIXME: the split breaks titles containing semicolons.
            // Should we expose the raw OSC string from the parser? Or join by semicolon?
            m_current_window_title = stringview_ify(1).to_string();
            m_client.set_window_title(m_current_window_title);
        }
        break;
    case 8:
#ifndef KERNEL
        if (parameters.size() < 3) {
            dbgln("Attempted to set href but gave too few parameters");
        } else if (parameters[1].is_empty() && parameters[2].is_empty()) {
            // Clear hyperlink
            m_current_state.attribute.href = String();
            m_current_state.attribute.href_id = String();
        } else {
            m_current_state.attribute.href = stringview_ify(2);
            // FIXME: Respect the provided ID
            m_current_state.attribute.href_id = String::number(m_next_href_id++);
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
        while (m_normal_screen_buffer.size() < rows)
            m_normal_screen_buffer.append(make<Line>(columns));
        while (m_alternate_screen_buffer.size() < rows)
            m_alternate_screen_buffer.append(make<Line>(columns));
    } else {
        m_normal_screen_buffer.shrink(rows);
        m_alternate_screen_buffer.shrink(rows);
    }

    for (int i = 0; i < rows; ++i) {
        m_normal_screen_buffer[i].set_length(columns);
        m_alternate_screen_buffer[i].set_length(columns);
    }

    m_columns = columns;
    m_rows = rows;

    m_scroll_region_top = 0;
    m_scroll_region_bottom = rows - 1;

    m_current_state.cursor.clamp(m_rows - 1, m_columns - 1);
    m_normal_saved_state.cursor.clamp(m_rows - 1, m_columns - 1);
    m_alternate_saved_state.cursor.clamp(m_rows - 1, m_columns - 1);
    m_saved_cursor_position.clamp(m_rows - 1, m_columns - 1);

    m_horizontal_tabs.resize(columns);
    for (unsigned i = 0; i < columns; ++i)
        m_horizontal_tabs[i] = (i % 8) == 0;
    // Rightmost column is always last tab on line.
    m_horizontal_tabs[columns - 1] = 1;

    m_client.terminal_did_resize(m_columns, m_rows);

    dbgln_if(TERMINAL_DEBUG, "Set terminal size: {}x{}", m_rows, m_columns);
}
#endif

#ifndef KERNEL
void Terminal::invalidate_cursor()
{
    active_buffer()[cursor_row()].set_dirty(true);
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
