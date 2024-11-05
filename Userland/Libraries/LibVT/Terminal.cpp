/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Queue.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/TemporaryChange.h>
#include <LibVT/Color.h>
#include <LibVT/Terminal.h>
#ifdef KERNEL
#    include <Kernel/Devices/TTY/VirtualConsole.h>
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
    dbgln_if(TERMINAL_DEBUG, "Clear the entire screen");
    for (size_t i = 0; i < rows(); ++i)
        active_buffer()[i]->clear();
    set_cursor(0, 0);

    if (!m_use_alternate_screen_buffer)
        m_client.terminal_did_perform_possibly_partial_clear();
}

void Terminal::clear_history()
{
    dbgln_if(TERMINAL_DEBUG, "Clear history");
    auto previous_history_size = m_history.size();
    m_history.clear();
    m_history_start = 0;
    m_client.terminal_history_changed(-previous_history_size);
}

void Terminal::clear_to_mark(Mark mark)
{
    auto cursor_row = this->cursor_row();
    ScopeGuard send_sigwinch = [&] {
        set_cursor(cursor_row, 1);
        mark_cursor();
        m_client.terminal_did_perform_possibly_partial_clear();
    };
    m_valid_marks.remove(mark);

    {
        auto it = active_buffer().rbegin();
        size_t row = m_rows - 1;
        // Skip to the cursor line.
        for (size_t i = this->cursor_row() + 1; i < active_buffer().size(); ++i, row--)
            ++it;
        for (; it != active_buffer().rend(); ++it, row--) {
            auto& line = *it;
            auto line_mark = line->mark();
            auto is_target_line = line_mark == mark;
            if (line_mark.has_value())
                m_valid_marks.remove(*line_mark);
            line->clear();
            if (is_target_line) {
                cursor_row = row;
                return;
            }
        }
    }

    // If the mark is not found, go through the history.
    auto it = AK::find_if(
        m_history.rbegin(),
        m_history.rend(),
        [mark](auto& line) {
            return line->mark() == mark;
        });
    auto index = it == m_history.rend() ? 0 : m_history.size() - it.index();
    m_client.terminal_history_changed(m_history.size() - index);
    auto count = m_history.size() - index;
    for (size_t i = 0; i < count; ++i) {
        if (auto mark = m_history[index + i]->mark(); mark.has_value())
            m_valid_marks.remove(*mark);
    }
    m_history.remove(index, count);
    cursor_row = 0;
}

void Terminal::mark_cursor()
{
    static u32 next_mark_id { 0 };

    auto& line = active_buffer()[cursor_row()];
    if (line->mark().has_value()) {
        return;
    }

    auto mark = Mark(next_mark_id++);
    line->set_marked(mark);
    m_valid_marks.set(mark);
}
#endif

void Terminal::alter_ansi_mode(bool should_set, Parameters params)
{
    for (auto mode : params) {
        switch (mode) {
            // FIXME: implement *something* for this
        default:
            dbgln("Terminal::alter_ansi_mode: Unimplemented mode {} (should_set={})", mode, should_set);
            break;
        }
    }
}

void Terminal::alter_private_mode(bool should_set, Parameters params)
{
    for (auto mode : params) {
        switch (mode) {
        case 1:
            // Cursor Keys Mode (DECCKM)
            dbgln_if(TERMINAL_DEBUG, "Setting cursor keys mode (should_set={})", should_set);
            m_cursor_keys_mode = should_set ? CursorKeysMode::Application : CursorKeysMode::Cursor;
            break;
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
                m_client.set_cursor_blinking(true);
            } else {
                // Stop blinking cursor
                m_client.set_cursor_blinking(false);
            }
            break;
        case 25:
            if (should_set) {
                // Show cursor
                m_cursor_shape = m_saved_cursor_shape;
                m_client.set_cursor_shape(m_cursor_shape);
            } else {
                // Hide cursor
                m_saved_cursor_shape = m_cursor_shape;
                m_cursor_shape = VT::CursorShape::None;
                m_client.set_cursor_shape(VT::CursorShape::None);
            }
            break;
        case 1047:
#ifndef KERNEL
            if (should_set) {
                dbgln_if(TERMINAL_DEBUG, "Switching to Alternate Screen Buffer");
                m_use_alternate_screen_buffer = true;
                clear();
                m_client.terminal_history_changed(-m_history.size());
            } else {
                dbgln_if(TERMINAL_DEBUG, "Switching to Normal Screen Buffer");
                m_use_alternate_screen_buffer = false;
                m_client.terminal_history_changed(m_history.size());
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
                m_client.terminal_history_changed(-m_history.size());
            } else {
                dbgln_if(TERMINAL_DEBUG, "Switching to Normal Screen Buffer and restoring state");
                m_current_state = m_normal_saved_state;
                m_use_alternate_screen_buffer = false;
                set_cursor(cursor_row(), cursor_column());
                m_client.terminal_history_changed(m_history.size());
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
            dbgln("Terminal::alter_private_mode: Unimplemented private mode {} (should_set={})", mode, should_set);
            break;
        }
    }
}

void Terminal::RM(Parameters params)
{
    alter_ansi_mode(false, params);
}

void Terminal::DECRST(Parameters params)
{
    alter_private_mode(false, params);
}

void Terminal::SM(Parameters params)
{
    alter_ansi_mode(true, params);
}

void Terminal::DECSET(Parameters params)
{
    alter_private_mode(true, params);
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
                m_current_state.attribute.flags |= Attribute::Flags::Bold;
                break;
            case 3:
                m_current_state.attribute.flags |= Attribute::Flags::Italic;
                break;
            case 4:
                m_current_state.attribute.flags |= Attribute::Flags::Underline;
                break;
            case 5:
                m_current_state.attribute.flags |= Attribute::Flags::Blink;
                break;
            case 7:
                m_current_state.attribute.flags |= Attribute::Flags::Negative;
                break;
            case 8:
                m_current_state.attribute.flags |= Attribute::Flags::Concealed;
                break;
            case 22:
                m_current_state.attribute.flags &= ~Attribute::Flags::Bold;
                break;
            case 23:
                m_current_state.attribute.flags &= ~Attribute::Flags::Italic;
                break;
            case 24:
                m_current_state.attribute.flags &= ~Attribute::Flags::Underline;
                break;
            case 25:
                m_current_state.attribute.flags &= ~Attribute::Flags::Blink;
                break;
            case 27:
                m_current_state.attribute.flags &= ~Attribute::Flags::Negative;
                break;
            case 28:
                m_current_state.attribute.flags &= ~Attribute::Flags::Concealed;
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
#ifndef KERNEL
        if (params.size() > 1 && params[1] == 1) {
            dbgln("FIXME: we don't support icon titles");
            return;
        }
        dbgln_if(TERMINAL_DEBUG, "Title stack push: {}", m_current_window_title);
        (void)m_title_stack.try_append(move(m_current_window_title));
#endif
        break;
    }
    case 23: {
#ifndef KERNEL
        if (params.size() > 1 && params[1] == 1)
            return;
        if (m_title_stack.is_empty()) {
            dbgln("Shenanigans: Tried to pop from empty title stack");
            return;
        }
        m_current_window_title = m_title_stack.take_last();
        dbgln_if(TERMINAL_DEBUG, "Title stack pop: {}", m_current_window_title);
        m_client.set_window_title(m_current_window_title);
#endif
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
    if (top >= bottom) {
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
    // Clear num characters from the right of the cursor.
    auto clear_end = min<unsigned>(m_columns, cursor_column() + num - 1);
    dbgln_if(TERMINAL_DEBUG, "Erase characters {}-{} on line {}", cursor_column(), clear_end, cursor_row());
    clear_in_line(cursor_row(), cursor_column(), clear_end);
}

void Terminal::EL(Parameters params)
{
    unsigned mode = 0;
    if (params.size() >= 1)
        mode = params[0];
    switch (mode) {
    case 0:
        dbgln_if(TERMINAL_DEBUG, "Clear line {} from cursor column ({}) to the end", cursor_row(), cursor_column());
        clear_in_line(cursor_row(), cursor_column(), m_columns - 1);
        break;
    case 1:
        dbgln_if(TERMINAL_DEBUG, "Clear line {} from the start to cursor column ({})", cursor_row(), cursor_column());
        clear_in_line(cursor_row(), 0, cursor_column());
        break;
    case 2:
        dbgln_if(TERMINAL_DEBUG, "Clear line {} completely", cursor_row());
        clear_in_line(cursor_row(), 0, m_columns - 1);
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
        dbgln_if(TERMINAL_DEBUG, "Clear from cursor ({},{}) to end of screen", cursor_row(), cursor_column());
        clear_in_line(cursor_row(), cursor_column(), m_columns - 1);
        for (int row = cursor_row() + 1; row < m_rows; ++row)
            clear_in_line(row, 0, m_columns - 1);
        break;
    case 1:
        dbgln_if(TERMINAL_DEBUG, "Clear from beginning of screen to cursor ({},{})", cursor_row(), cursor_column());
        clear_in_line(cursor_row(), 0, cursor_column());
        for (int row = cursor_row() - 1; row >= 0; --row)
            clear_in_line(row, 0, m_columns - 1);
        break;
    case 2:
        clear();
        break;
    case 3:
        clear_history();
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

    scroll_up(count);
}

void Terminal::SD(Parameters params)
{
    unsigned count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];

    scroll_down(count);
}

void Terminal::DECSCUSR(Parameters params)
{
    unsigned style = 1;
    if (params.size() >= 1 && params[0] != 0)
        style = params[0];
    switch (style) {
    case 1:
        m_client.set_cursor_shape(VT::CursorShape::Block);
        m_client.set_cursor_blinking(true);
        break;
    case 2:
        m_client.set_cursor_shape(VT::CursorShape::Block);
        m_client.set_cursor_blinking(false);
        break;
    case 3:
        m_client.set_cursor_shape(VT::CursorShape::Underline);
        m_client.set_cursor_blinking(true);
        break;
    case 4:
        m_client.set_cursor_shape(VT::CursorShape::Underline);
        m_client.set_cursor_blinking(false);
        break;
    case 5:
        m_client.set_cursor_shape(VT::CursorShape::Bar);
        m_client.set_cursor_blinking(true);
        break;
    case 6:
        m_client.set_cursor_shape(VT::CursorShape::Bar);
        m_client.set_cursor_blinking(false);
        break;
    default:
        dbgln("Unknown cursor style {}", style);
    }
}

void Terminal::IL(Parameters params)
{
    size_t count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];
    if (!is_within_scroll_region(cursor_row())) {
        dbgln("Shenanigans! Tried to insert line outside the scroll region");
        return;
    }
    scroll_down(cursor_row(), m_scroll_region_bottom, count);
}

void Terminal::DA(Parameters)
{
    emit_string("\033[?1;0c"sv);
}

void Terminal::DL(Parameters params)
{
    size_t count = 1;
    if (params.size() >= 1 && params[0] != 0)
        count = params[0];
    if (!is_within_scroll_region(cursor_row())) {
        dbgln("Shenanigans! Tried to delete line outside the scroll region");
        return;
    }
    scroll_up(cursor_row(), m_scroll_region_bottom, count);
}

void Terminal::DCH(Parameters params)
{
    int num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    num = min<int>(num, columns() - cursor_column());
    scroll_left(cursor_row(), cursor_column(), num);
}

void Terminal::linefeed()
{
    u16 new_row = cursor_row();
#ifndef KERNEL
    if (!m_controls_are_logically_generated)
        active_buffer()[new_row]->set_terminated(m_column_before_carriage_return.value_or(cursor_column()));
#endif
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
    dbgln_if(TERMINAL_DEBUG, "Carriage return");
    m_column_before_carriage_return = cursor_column();
    set_cursor(cursor_row(), 0);
}

void Terminal::scroll_up(size_t count)
{
    scroll_up(m_scroll_region_top, m_scroll_region_bottom, count);
}

void Terminal::scroll_down(size_t count)
{
    scroll_down(m_scroll_region_top, m_scroll_region_bottom, count);
}

#ifndef KERNEL
// Insert `count` blank lines at the bottom of the region. Text moves up, top lines get added to the scrollback.
void Terminal::scroll_up(u16 region_top, u16 region_bottom, size_t count)
{
    VERIFY(region_top <= region_bottom);
    VERIFY(region_bottom < rows());
    // Only the specified region should be affected.
    size_t region_size = region_bottom - region_top + 1;
    count = min(count, region_size);
    dbgln_if(TERMINAL_DEBUG, "Scroll up {} lines in region {}-{}", count, region_top, region_bottom);
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();

    int history_delta = -count;
    bool should_move_to_scrollback = !m_use_alternate_screen_buffer && max_history_size() != 0;
    if (should_move_to_scrollback) {
        auto remaining_lines = max_history_size() - history_size();
        history_delta = (count > remaining_lines) ? remaining_lines - count : 0;
        for (size_t i = 0; i < count; ++i)
            add_line_to_history(move(active_buffer().at(region_top + i)));
    }

    // Move lines into their new place.
    for (u16 row = region_top; row + count <= region_bottom; ++row)
        swap(active_buffer().at(row), active_buffer().at(row + count));
    // Clear 'new' lines at the bottom.
    if (should_move_to_scrollback) {
        // Since we moved the previous lines into history, we can't just clear them.
        for (u16 row = region_bottom + 1 - count; row <= region_bottom; ++row)
            active_buffer().at(row) = make<Line>(columns());
    } else {
        // The new lines haven't been moved and we don't want to leak memory.
        for (u16 row = region_bottom + 1 - count; row <= region_bottom; ++row)
            active_buffer()[row]->clear();
    }
    // Set dirty flag on swapped lines.
    // The other lines have implicitly been set dirty by being cleared.
    for (u16 row = region_top; row + count <= region_bottom; ++row)
        active_buffer()[row]->set_dirty(true);
    m_client.terminal_history_changed(history_delta);
}

// Insert `count` blank lines at the top of the region. Text moves down. Does not affect the scrollback buffer.
void Terminal::scroll_down(u16 region_top, u16 region_bottom, size_t count)
{
    VERIFY(region_top <= region_bottom);
    VERIFY(region_bottom < rows());
    // Only the specified region should be affected.
    size_t region_size = region_bottom - region_top + 1;
    count = min(count, region_size);
    dbgln_if(TERMINAL_DEBUG, "Scroll down {} lines in region {}-{}", count, region_top, region_bottom);
    // NOTE: We have to invalidate the cursor first.
    invalidate_cursor();

    // Move lines into their new place.
    for (int row = region_bottom; row >= static_cast<int>(region_top + count); --row)
        swap(active_buffer().at(row), active_buffer().at(row - count));
    // Clear the 'new' lines at the top.
    for (u16 row = region_top; row < region_top + count; ++row)
        active_buffer()[row]->clear();
    // Set dirty flag on swapped lines.
    // The other lines have implicitly been set dirty by being cleared.
    for (u16 row = region_top + count; row <= region_bottom; ++row)
        active_buffer()[row]->set_dirty(true);
}

// Insert `count` blank cells at the end of the line. Text moves left.
void Terminal::scroll_left(u16 row, u16 column, size_t count)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    count = min<size_t>(count, columns() - column);
    dbgln_if(TERMINAL_DEBUG, "Scroll left {} columns from line {} column {}", count, row, column);

    auto& line = active_buffer()[row];
    for (size_t i = column; i < columns() - count; ++i)
        swap(line->cell_at(i), line->cell_at(i + count));
    clear_in_line(row, columns() - count, columns() - 1);
    line->set_dirty(true);
}

// Insert `count` blank cells after `row`. Text moves right.
void Terminal::scroll_right(u16 row, u16 column, size_t count)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    count = min<size_t>(count, columns() - column);
    dbgln_if(TERMINAL_DEBUG, "Scroll right {} columns from line {} column {}", count, row, column);

    auto& line = active_buffer()[row];
    for (int i = columns() - 1; i >= static_cast<int>(column + count); --i)
        swap(line->cell_at(i), line->cell_at(i - count));
    clear_in_line(row, column, column + count - 1);
    line->set_dirty(true);
}

void Terminal::put_character_at(unsigned row, unsigned column, u32 code_point)
{
    VERIFY(row < rows());
    VERIFY(column < columns());
    auto& line = active_buffer()[row];
    line->set_code_point(column, code_point);
    line->attribute_at(column) = m_current_state.attribute;
    line->attribute_at(column).flags |= Attribute::Flags::Touched;
    line->set_dirty(true);

    m_last_code_point = code_point;
}

void Terminal::clear_in_line(u16 row, u16 first_column, u16 last_column)
{
    VERIFY(row < rows());
    active_buffer()[row]->clear_range(first_column, last_column, m_current_state.attribute);
}
#endif

void Terminal::set_cursor(unsigned a_row, unsigned a_column, bool skip_debug)
{
    unsigned row = min(a_row, m_rows - 1u);
    unsigned column = min(a_column, m_columns - 1u);
    m_stomp = false;
    if (row == cursor_row() && column == cursor_column())
        return;
    VERIFY(row < rows());
    VERIFY(column < columns());
    invalidate_cursor();
    m_current_state.cursor.row = row;
    m_current_state.cursor.column = column;
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

void Terminal::DECFI()
{
    if (cursor_column() == columns() - 1)
        scroll_left(cursor_row(), 0, 1);
    else
        set_cursor(cursor_row(), cursor_column() + 1);
}

void Terminal::DECBI()
{
    if (cursor_column() == 0)
        scroll_right(cursor_row(), 0, 1);
    else
        set_cursor(cursor_row(), cursor_column() - 1);
}

void Terminal::DECIC(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    num = min<unsigned>(num, columns() - cursor_column());
    for (unsigned row = cursor_row(); row <= m_scroll_region_bottom; ++row)
        scroll_right(row, cursor_column(), num);
}

void Terminal::DECDC(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    num = min<unsigned>(num, columns() - cursor_column());
    for (unsigned row = cursor_row(); row <= m_scroll_region_bottom; ++row)
        scroll_left(row, cursor_column(), num);
}

void Terminal::DECKPNM()
{
    m_in_application_keypad_mode = false;
}

void Terminal::DECKPAM()
{
    m_in_application_keypad_mode = true;
}

void Terminal::DSR(Parameters params)
{
    if (params.size() == 1 && params[0] == 5) {
        // Device status
        emit_string("\033[0n"sv); // Terminal status OK!
    } else if (params.size() == 1 && params[0] == 6) {
        // Cursor position query
        StringBuilder builder;
        MUST(builder.try_appendff("\e[{};{}R", cursor_row() + 1, cursor_column() + 1)); // StringBuilder's inline capacity of 256 is enough to guarantee no allocations
        emit_string(builder.string_view());
    } else {
        dbgln("Unknown DSR");
    }
}

void Terminal::ICH(Parameters params)
{
    unsigned num = 1;
    if (params.size() >= 1 && params[0] != 0)
        num = params[0];

    num = min<unsigned>(num, columns() - cursor_column());
    scroll_right(cursor_row(), cursor_column(), num);
}

void Terminal::on_input(u8 byte)
{
    m_parser.on_input(byte);
}

void Terminal::emit_code_point(u32 code_point)
{
    auto working_set = m_working_sets[m_active_working_set_index];
    code_point = m_character_set_translator.translate_code_point(working_set, code_point);

    auto new_column = cursor_column() + 1;
    if (new_column < columns()) {
        put_character_at(cursor_row(), cursor_column(), code_point);
        set_cursor(cursor_row(), new_column, true);
        return;
    }
    if (m_stomp) {
        m_stomp = false;
        TemporaryChange change { m_controls_are_logically_generated, true };
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
    ArmedScopeGuard clear_position_before_cr {
        [&] {
            m_column_before_carriage_return.clear();
        }
    };
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
        if (m_column_before_carriage_return == m_columns - 1)
            m_column_before_carriage_return = m_columns;
        linefeed();
        return;
    case '\r':
        carriage_return();
        clear_position_before_cr.disarm();
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
        case '6':
            DECBI();
            return;
        case '7':
            DECSC();
            return;
        case '8':
            DECRC();
            return;
        case '9':
            DECFI();
            return;
        case '=':
            DECKPAM();
            return;
        case '>':
            DECKPNM();
            return;
        }
        unimplemented_escape_sequence(intermediates, last_byte);
        return;
    }

    char intermediate = intermediates[0];
    switch (intermediate) {
    case '#':
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
        break;
    case '(':
    case ')':
    case '*':
    case '+':
        // Determine G0..G3 index
        size_t working_set_index = intermediate - '(';

        CharacterSet new_set;
        switch (last_byte) {
        case 'B':
            new_set = CharacterSet::Iso_8859_1;
            break;
        case '0':
            new_set = CharacterSet::VT100;
            break;
        case 'U':
            new_set = CharacterSet::Null;
            break;
        case 'K':
            new_set = CharacterSet::UserDefined;
            break;
        default:
            unimplemented_escape_sequence(intermediates, last_byte);
            return;
        }

        dbgln_if(TERMINAL_DEBUG, "Setting G{} working set to character set {}", working_set_index, to_underlying(new_set));
        VERIFY(working_set_index <= 3);
        m_working_sets[working_set_index] = new_set;
        return;
    }

    unimplemented_escape_sequence(intermediates, last_byte);
}

void Terminal::execute_csi_sequence(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte)
{
    // FIXME: Handle it somehow?
    if (ignore)
        dbgln("CSI sequence has its ignore flag set.");

    if (intermediates.is_empty()) {
        switch (last_byte) {
        case '@':
            return ICH(parameters);
        case 'A':
            return CUU(parameters);
        case 'B':
            return CUD(parameters);
        case 'C':
            return CUF(parameters);
        case 'D':
            return CUB(parameters);
        case 'E':
            return CNL(parameters);
        case 'F':
            return CPL(parameters);
        case 'G':
            return CHA(parameters);
        case 'H':
            return CUP(parameters);
        case 'J':
            return ED(parameters);
        case 'K':
            return EL(parameters);
        case 'L':
            return IL(parameters);
        case 'M':
            return DL(parameters);
        case 'P':
            return DCH(parameters);
        case 'S':
            return SU(parameters);
        case 'T':
            return SD(parameters);
        case 'X':
            return ECH(parameters);
        case '`':
            return HPA(parameters);
        case 'a':
            return HPR(parameters);
        case 'b':
            return REP(parameters);
        case 'c':
            return DA(parameters);
        case 'd':
            return VPA(parameters);
        case 'e':
            return VPR(parameters);
        case 'f':
            return HVP(parameters);
        case 'h':
            return SM(parameters);
        case 'l':
            return RM(parameters);
        case 'm':
            return SGR(parameters);
        case 'n':
            return DSR(parameters);
        case 'r':
            return DECSTBM(parameters);
        case 's':
            return SCOSC();
        case 't':
            return XTERM_WM(parameters);
        case 'u':
            return SCORC();
        }
    } else if (intermediates.size() == 1 && intermediates[0] == '?') {
        switch (last_byte) {
        case 'h':
            return DECSET(parameters);
        case 'l':
            return DECRST(parameters);
        }
    } else if (intermediates.size() == 1 && intermediates[0] == '\'') {
        switch (last_byte) {
        case '}':
            return DECIC(parameters);
        case '~':
            return DECDC(parameters);
        }
    } else if (intermediates.size() == 1 && intermediates[0] == ' ') {
        switch (last_byte) {
        case 'q':
            return DECSCUSR(parameters);
        }
    }

    unimplemented_csi_sequence(parameters, intermediates, last_byte);
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

    auto command_number = stringview_ify(0).to_number<unsigned>();
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
#ifndef KERNEL
            m_current_window_title = stringview_ify(1).to_byte_string();
            m_client.set_window_title(m_current_window_title);
#endif
        }
        break;
    case 8:
#ifndef KERNEL
        if (parameters.size() < 3) {
            dbgln("Attempted to set href but gave too few parameters");
        } else if (parameters[1].is_empty() && parameters[2].is_empty()) {
            // Clear hyperlink
            m_current_state.attribute.href = {};
            m_current_state.attribute.href_id = {};
        } else {
            m_current_state.attribute.href = stringview_ify(2);
            // FIXME: Respect the provided ID
            m_current_state.attribute.href_id = ByteString::number(m_next_href_id++);
        }
#endif
        break;
    case 9:
        if (parameters.size() < 2)
            dbgln("Atttempted to set window progress but gave too few parameters");
        else if (parameters.size() == 2)
            m_client.set_window_progress(stringview_ify(1).to_number<int>().value_or(-1), 0);
        else
            m_client.set_window_progress(stringview_ify(1).to_number<int>().value_or(-1), stringview_ify(2).to_number<int>().value_or(0));
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

void Terminal::inject_string(StringView str)
{
    for (size_t i = 0; i < str.length(); ++i)
        on_input(str[i]);
}

void Terminal::emit_string(StringView string)
{
    m_client.emit((u8 const*)string.characters_without_null_termination(), string.length());
}

void Terminal::handle_key_press(KeyCode key, u32 code_point, u8 flags)
{
    bool ctrl = flags & Mod_Ctrl;
    bool alt = flags & Mod_Alt;
    bool shift = flags & Mod_Shift;
    bool keypad = flags & Mod_Keypad;
    unsigned modifier_mask = int(shift) + (int(alt) << 1) + (int(ctrl) << 2);

    auto emit_final_with_modifier = [this, modifier_mask](char final) {
        char escape_character = m_cursor_keys_mode == CursorKeysMode::Application ? 'O' : '[';
        StringBuilder builder;
        if (modifier_mask)
            MUST(builder.try_appendff("\e{}1;{}{:c}", escape_character, modifier_mask + 1, final)); // StringBuilder's inline capacity of 256 is enough to guarantee no allocations
        else
            MUST(builder.try_appendff("\e{}{:c}", escape_character, final)); // StringBuilder's inline capacity of 256 is enough to guarantee no allocations
        emit_string(builder.string_view());
    };
    auto emit_tilde_with_modifier = [this, modifier_mask](unsigned num) {
        StringBuilder builder;
        if (modifier_mask)
            MUST(builder.try_appendff("\e[{};{}~", num, modifier_mask + 1)); // StringBuilder's inline capacity of 256 is enough to guarantee no allocations
        else
            MUST(builder.try_appendff("\e[{}~", num)); // StringBuilder's inline capacity of 256 is enough to guarantee no allocations
        emit_string(builder.string_view());
    };

    auto emit_application_code = [this](KeyCode key) {
        // The table providing mapping from numeric keys to application keys can be found at https://vt100.net/docs/vt100-ug/chapter3.html#T3-7
        StringBuilder builder;
        builder.append("\x1bO"sv);
        switch (key) {
        case KeyCode::Key_0:
            builder.append('p');
            break;
        case KeyCode::Key_1:
            builder.append('q');
            break;
        case KeyCode::Key_2:
            builder.append('r');
            break;
        case KeyCode::Key_3:
            builder.append('s');
            break;
        case KeyCode::Key_4:
            builder.append('t');
            break;
        case KeyCode::Key_5:
            builder.append('u');
            break;
        case KeyCode::Key_6:
            builder.append('v');
            break;
        case KeyCode::Key_7:
            builder.append('w');
            break;
        case KeyCode::Key_8:
            builder.append('x');
            break;
        case KeyCode::Key_9:
            builder.append('y');
            break;
        case KeyCode::Key_Minus:
            builder.append('m');
            break;
        case KeyCode::Key_Comma:
            builder.append('l');
            break;
        case KeyCode::Key_Period:
            builder.append('n');
            break;
        case KeyCode::Key_Return:
            builder.append('M');
            break;
        default:
            break;
        }
        emit_string(builder.string_view());
    };

    if (keypad && m_in_application_keypad_mode) {
        emit_application_code(key);
        return;
    }

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
    case KeyCode::Key_Backspace:
        if (ctrl) {
            // This is an extension that allows Editor.cpp to delete whole words when
            // Ctrl+Backspace is pressed. Ctrl cannot be transmitted without a CSI, and
            // ANSI delete (127) is within the valid range for CSI codes in Editor.cpp.
            // The code also has the same behavior as backspace when emitted with no CSI,
            // though the backspace code (8) is preserved when Ctrl is not pressed.
            emit_final_with_modifier(127);
            return;
        }
        break;
    case KeyCode::Key_Return:
        // The standard says that CR should be generated by the return key.
        // The TTY will take care of translating it to CR LF for the terminal.
        emit_string("\r"sv);
        return;
    default:
        break;
    }

    if (!code_point) {
        // Probably a modifier being pressed.
        return;
    }

    if (shift && key == KeyCode::Key_Tab) {
        emit_string("\033[Z"sv);
        return;
    }

    // Key event was not one of the above special cases,
    // attempt to treat it as a character...
    if (ctrl) {
        constexpr auto ESC = '\033';
        constexpr auto NUL = 0;
        constexpr auto DEL = 0x7f;

        if (code_point >= '@' && code_point < DEL)
            code_point &= 0x1f;
        // Legacy aliases
        else if (code_point == '2' || code_point == ' ')
            // Ctrl+{2, Space, @, `} -> ^@ (NUL)
            code_point = NUL;
        else if (code_point >= '3' && code_point <= '7')
            // Ctrl+3 -> ^[ (ESC), Ctrl+4 -> ^\, Ctrl+5 -> ^], Ctrl+6 -> ^^, Ctrl+7 -> ^_
            code_point = ESC + (code_point - '3');
        else if (code_point == '8')
            // Ctrl+8 -> ^? (DEL)
            code_point = DEL;
        else if (code_point == '/')
            // Ctrl+/ -> ^_
            code_point = '_' & 0x1f;
    }

    // Alt modifier sends escape prefix.
    if (alt)
        emit_string("\033"sv);

    StringBuilder sb;
    sb.append_code_point(code_point);
    emit_string(sb.string_view());
}

void Terminal::unimplemented_control_code(u8 code)
{
    dbgln_if(TERMINAL_DEBUG, "Unimplemented control code {:02x}", code);
}

void Terminal::unimplemented_escape_sequence(Intermediates intermediates, u8 last_byte)
{
    StringBuilder builder;
    builder.appendff("Unimplemented escape sequence {:c}", last_byte);
    if (!intermediates.is_empty()) {
        builder.append(", intermediates: "sv);
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
        builder.append(", parameters: ["sv);
        for (size_t i = 0; i < parameters.size(); ++i)
            builder.appendff("{}{}", (i == 0) ? "" : ", ", parameters[i]);
        builder.append("]"sv);
    }
    if (!intermediates.is_empty()) {
        builder.append(", intermediates:"sv);
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
            builder.append(", "sv);
        builder.append('[');
        for (auto character : parameter)
            builder.append((char)character);
        builder.append(']');
        first = false;
    }

    builder.append(" ]"sv);
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

    // If we're making the terminal larger (column-wise), start at the end and go up, taking cells from the line below.
    // otherwise start at the beginning and go down, pushing cells into the line below.
    auto resize_and_rewrap = [&](auto& buffer, auto& old_cursor) {
        auto cursor_on_line = [&](auto index) {
            return index == old_cursor.row ? &old_cursor : nullptr;
        };
        // Two passes, one from top to bottom, another from bottom to top
        for (size_t pass = 0; pass < 2; ++pass) {
            auto forwards = (pass == 0) ^ (columns < m_columns);
            if (forwards) {
                for (size_t i = 1; i <= buffer.size(); ++i) {
                    auto is_at_seam = i == 1;
                    Line* next_line = is_at_seam ? nullptr : buffer[buffer.size() - i + 1].ptr();
                    Line* line = buffer[buffer.size() - i].ptr();
                    auto next_cursor = cursor_on_line(buffer.size() - i + 1);
                    line->rewrap(columns, next_line, next_cursor ?: cursor_on_line(buffer.size() - i), !!next_cursor);
                }
            } else {
                for (size_t i = 0; i < buffer.size(); ++i) {
                    auto is_at_seam = i + 1 == buffer.size();
                    Line* next_line = is_at_seam ? nullptr : buffer[i + 1].ptr();
                    auto next_cursor = cursor_on_line(i + 1);
                    buffer[i]->rewrap(columns, next_line, next_cursor ?: cursor_on_line(i), !!next_cursor);
                }
            }

            Queue<size_t> lines_to_reevaluate;
            for (size_t i = 0; i < buffer.size(); ++i) {
                if (buffer[i]->length() != columns)
                    lines_to_reevaluate.enqueue(i);
            }
            while (!lines_to_reevaluate.is_empty()) {
                auto index = lines_to_reevaluate.dequeue();
                auto is_at_seam = index + 1 == buffer.size();
                Line* const next_line = is_at_seam ? nullptr : buffer[index + 1].ptr();
                Line* const line = buffer[index].ptr();
                auto next_cursor = cursor_on_line(index + 1);
                line->rewrap(columns, next_line, next_cursor ?: cursor_on_line(index), !!next_cursor);
                if (line->length() > columns) {
                    auto current_cursor = cursor_on_line(index);
                    // Split the line into two (or more)
                    ++index;
                    buffer.insert(index, make<Line>(0));
                    VERIFY(buffer[index]->length() == 0);
                    line->rewrap(columns, buffer[index].ptr(), current_cursor, false);
                    // If we inserted a line and the old cursor was after that line, increment its row
                    if (!current_cursor && old_cursor.row >= index)
                        ++old_cursor.row;

                    if (buffer[index]->length() != columns)
                        lines_to_reevaluate.enqueue(index);
                }
                if (next_line && next_line->length() != columns)
                    lines_to_reevaluate.enqueue(index + 1);
            }
        }

        for (auto& line : buffer)
            line->set_length(columns);

        return old_cursor;
    };

    auto old_history_size = m_history.size();
    m_history.extend(move(m_normal_screen_buffer));
    CursorPosition cursor_tracker { cursor_row() + old_history_size, cursor_column() };
    resize_and_rewrap(m_history, cursor_tracker);
    if (auto extra_lines = m_history.size() - rows) {
        while (extra_lines > 0) {
            if (m_history.size() <= cursor_tracker.row)
                break;
            if (m_history.last()->is_empty()) {
                if (m_history.size() >= 2 && m_history[m_history.size() - 2]->termination_column().has_value())
                    break;
                --extra_lines;
                (void)m_history.take_last();
                continue;
            }
            break;
        }
    }

    // FIXME: This can use a more performant way to move the last N entries
    //        from the history into the normal buffer
    m_normal_screen_buffer.ensure_capacity(rows);
    while (m_normal_screen_buffer.size() < rows) {
        if (!m_history.is_empty())
            m_normal_screen_buffer.prepend(m_history.take_last());
        else
            m_normal_screen_buffer.unchecked_append(make<Line>(columns));
    }

    cursor_tracker.row -= m_history.size();

    if (m_history.size() != old_history_size) {
        m_client.terminal_history_changed(-old_history_size);
        m_client.terminal_history_changed(m_history.size());
    }

    CursorPosition dummy_cursor_tracker {};
    resize_and_rewrap(m_alternate_screen_buffer, dummy_cursor_tracker);
    if (m_alternate_screen_buffer.size() > rows)
        m_alternate_screen_buffer.remove(0, m_alternate_screen_buffer.size() - rows);

    if (rows > m_rows) {
        while (m_normal_screen_buffer.size() < rows)
            m_normal_screen_buffer.append(make<Line>(columns));
        while (m_alternate_screen_buffer.size() < rows)
            m_alternate_screen_buffer.append(make<Line>(columns));
    } else {
        m_normal_screen_buffer.shrink(rows);
        m_alternate_screen_buffer.shrink(rows);
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

    set_cursor(cursor_tracker.row, cursor_tracker.column);

    m_client.terminal_did_resize(m_columns, m_rows);

    dbgln_if(TERMINAL_DEBUG, "Set terminal size: {}x{}", m_rows, m_columns);
}
#endif

#ifndef KERNEL
void Terminal::invalidate_cursor()
{
    if (cursor_row() < active_buffer().size())
        active_buffer()[cursor_row()]->set_dirty(true);
}

Attribute Terminal::attribute_at(Position const& position) const
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
