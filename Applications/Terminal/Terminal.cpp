#include "Terminal.h"
#include "XtermColors.h"
#include <AK/AKString.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWindow.h>
#include <LibDraw/Font.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

//#define TERMINAL_DEBUG
u8 Terminal::Attribute::default_foreground_color = 7;
u8 Terminal::Attribute::default_background_color = 0;

Terminal::Terminal(int ptm_fd, RefPtr<CConfigFile> config)
    : m_ptm_fd(ptm_fd)
    , m_notifier(ptm_fd, CNotifier::Read)
    , m_config(config)
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    dbgprintf("Terminal: Load config file from %s\n", m_config->file_name().characters());
    m_cursor_blink_timer.set_interval(m_config->read_num_entry("Text",
        "CursorBlinkInterval",
        500));
    m_cursor_blink_timer.on_timeout = [this] {
        m_cursor_blink_state = !m_cursor_blink_state;
        update_cursor();
    };

    auto font_entry = m_config->read_entry("Text", "Font", "default");
    if (font_entry == "default")
        set_font(Font::default_fixed_width_font());
    else
        set_font(Font::load_from_file(font_entry));

    m_notifier.on_ready_to_read = [this] {
        u8 buffer[BUFSIZ];
        ssize_t nread = read(m_ptm_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            dbgprintf("Terminal read error: %s\n", strerror(errno));
            perror("read(ptm)");
            GApplication::the().quit(1);
            return;
        }
        if (nread == 0) {
            dbgprintf("Terminal: EOF on master pty, closing.\n");
            GApplication::the().quit(0);
            return;
        }
        for (ssize_t i = 0; i < nread; ++i)
            on_char(buffer[i]);
        flush_dirty_lines();
    };

    m_line_height = font().glyph_height() + m_line_spacing;

    set_size(m_config->read_num_entry("Window", "Width", 80),
        m_config->read_num_entry("Window", "Height", 25));
}

Terminal::Line::Line(u16 length)
{
    set_length(length);
}

Terminal::Line::~Line()
{
    delete[] characters;
    delete[] attributes;
}

void Terminal::Line::set_length(u16 new_length)
{
    if (m_length == new_length)
        return;
    auto* new_characters = new u8[new_length];
    auto* new_attributes = new Attribute[new_length];
    memset(new_characters, ' ', new_length);
    if (characters && attributes) {
        memcpy(new_characters, characters, min(m_length, new_length));
        memcpy(new_attributes, attributes, min(m_length, new_length) * sizeof(Attribute));
    }
    delete[] characters;
    delete[] attributes;
    characters = new_characters;
    attributes = new_attributes;
    m_length = new_length;
}

void Terminal::Line::clear(Attribute attribute)
{
    if (dirty) {
        memset(characters, ' ', m_length);
        for (u16 i = 0; i < m_length; ++i)
            attributes[i] = attribute;
        return;
    }
    for (unsigned i = 0; i < m_length; ++i) {
        if (characters[i] != ' ')
            dirty = true;
        characters[i] = ' ';
    }
    for (unsigned i = 0; i < m_length; ++i) {
        if (attributes[i] != attribute)
            dirty = true;
        attributes[i] = attribute;
    }
}

Terminal::~Terminal()
{
}

void Terminal::clear()
{
    for (size_t i = 0; i < rows(); ++i)
        line(i).clear(m_current_attribute);
    set_cursor(0, 0);
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

static inline Color lookup_color(unsigned color)
{
    return Color::from_rgb(xterm_colors[color]);
}

void Terminal::escape$h_l(bool should_set, bool question_param, const ParamVector& params)
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
                dbgprintf("Terminal: Hide Cursor escapecode recieved. Not needed: ignored.\n");
            else
                dbgprintf("Terminal: Show Cursor escapecode recieved. Not needed: ignored.\n");
            break;
        default:
            break;
        }
    }
}

void Terminal::escape$m(const ParamVector& params)
{
    if (params.is_empty()) {
        m_current_attribute.reset();
        return;
    }
    if (params.size() == 3 && params[1] == 5) {
        if (params[0] == 38) {
            m_current_attribute.foreground_color = params[2];
            return;
        } else if (params[0] == 48) {
            m_current_attribute.background_color = params[2];
            return;
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
            m_current_attribute.foreground_color = param - 30;
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
            m_current_attribute.background_color = param - 40;
            break;
        case 49:
            // reset background
            m_current_attribute.background_color = Attribute::default_background_color;
            break;
        default:
            dbgprintf("FIXME: escape$m: p: %u\n", param);
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

void Terminal::escape$r(const ParamVector& params)
{
    unsigned top = 1;
    unsigned bottom = m_rows;
    if (params.size() >= 1)
        top = params[0];
    if (params.size() >= 2)
        bottom = params[1];
    if ((bottom - top) < 2 || bottom > m_rows) {
        dbgprintf("Error: escape$r: scrolling region invalid: %u-%u\n", top, bottom);
        return;
    }
    m_scroll_region_top = top - 1;
    m_scroll_region_bottom = bottom - 1;
    set_cursor(0, 0);
}

void Terminal::escape$H(const ParamVector& params)
{
    unsigned row = 1;
    unsigned col = 1;
    if (params.size() >= 1)
        row = params[0];
    if (params.size() >= 2)
        col = params[1];
    set_cursor(row - 1, col - 1);
}

void Terminal::escape$A(const ParamVector& params)
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

void Terminal::escape$B(const ParamVector& params)
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

void Terminal::escape$C(const ParamVector& params)
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

void Terminal::escape$D(const ParamVector& params)
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
        put_character_at(m_cursor_row, m_cursor_column++, m_last_char);
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

void Terminal::escape$K(const ParamVector& params)
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
        for (int i = 0; i < m_cursor_column; ++i) {
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

void Terminal::escape$J(const ParamVector& params)
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
        /// Clear from cursor to beginning of screen
        for (int i = m_cursor_column - 1; i >= 0; --i)
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

    auto& line = this->line(m_cursor_row);

    // Move n characters of line to the left
    for (int i = m_cursor_column; i < line.m_length - num; i++)
        line.characters[i] = line.characters[i + num];

    // Fill remainder of line with blanks
    for (int i = line.m_length - num; i < line.m_length; i++)
        line.characters[i] = ' ';

    line.dirty = true;
}

void Terminal::execute_xterm_command()
{
    m_final = '@';
    bool ok;
    unsigned value = String::copy(m_xterm_param1).to_uint(ok);
    if (ok) {
        switch (value) {
        case 0:
        case 1:
        case 2:
            set_window_title(String::copy(m_xterm_param2));
            break;
        default:
            unimplemented_xterm_escape();
            break;
        }
    }
    m_xterm_param1.clear_with_capacity();
    m_xterm_param2.clear_with_capacity();
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
        bool ok;
        unsigned value = parampart.to_uint(ok);
        if (!ok) {
            // FIXME: Should we do something else?
            m_parameters.clear_with_capacity();
            m_intermediates.clear_with_capacity();
            return;
        }
        params.append(value);
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
        escape$A(params);
        break;
    case 'B':
        escape$B(params);
        break;
    case 'C':
        escape$C(params);
        break;
    case 'D':
        escape$D(params);
        break;
    case 'H':
        escape$H(params);
        break;
    case 'J':
        escape$J(params);
        break;
    case 'K':
        escape$K(params);
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
        escape$m(params);
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
        escape$r(params);
        break;
    case 'l':
        escape$h_l(true, question_param, params);
        break;
    case 'h':
        escape$h_l(false, question_param, params);
        break;
    default:
        dbgprintf("Terminal::execute_escape_sequence: Unhandled final '%c'\n", final);
        break;
    }

#if defined(TERMINAL_DEBUG)
    dbgprintf("\n");
    for (auto& line : m_lines) {
        dbgprintf("Terminal: Line: ");
        for (int i = 0; i < line->length; i++) {
            dbgprintf("%c", line->characters[i]);
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
    if (column != columns() - 1u)
        m_stomp = false;
    invalidate_cursor();
}

void Terminal::put_character_at(unsigned row, unsigned column, u8 ch)
{
    ASSERT(row < rows());
    ASSERT(column < columns());
    auto& line = this->line(row);
    line.characters[column] = ch;
    line.attributes[column] = m_current_attribute;
    line.attributes[column].flags |= Attribute::Touched;
    line.dirty = true;

    m_last_char = ch;
}

void Terminal::on_char(u8 ch)
{
#ifdef TERMINAL_DEBUG
    dbgprintf("Terminal::on_char: %b (%c), fg=%u, bg=%u\n", ch, ch, m_current_attribute.foreground_color, m_current_attribute.background_color);
#endif
    switch (m_escape_state) {
    case ExpectBracket:
        if (ch == '[')
            m_escape_state = ExpectParameter;
        else if (ch == '(') {
            m_swallow_current = true;
            m_escape_state = ExpectParameter;
        } else if (ch == ']')
            m_escape_state = ExpectXtermParameter1;
        else
            m_escape_state = Normal;
        return;
    case ExpectXtermParameter1:
        if (ch != ';') {
            m_xterm_param1.append(ch);
            return;
        }
        m_escape_state = ExpectXtermParameter2;
        return;
    case ExpectXtermParameter2:
        if (ch != '\007') {
            m_xterm_param2.append(ch);
            return;
        }
        m_escape_state = ExpectXtermFinal;
        [[fallthrough]];
    case ExpectXtermFinal:
        m_escape_state = Normal;
        if (ch == '\007')
            execute_xterm_command();
        return;
    case ExpectParameter:
        if (is_valid_parameter_character(ch)) {
            m_parameters.append(ch);
            return;
        }
        m_escape_state = ExpectIntermediate;
        [[fallthrough]];
    case ExpectIntermediate:
        if (is_valid_intermediate_character(ch)) {
            m_intermediates.append(ch);
            return;
        }
        m_escape_state = ExpectFinal;
        [[fallthrough]];
    case ExpectFinal:
        if (is_valid_final_character(ch)) {
            m_escape_state = Normal;
            if (!m_swallow_current)
                execute_escape_sequence(ch);
            m_swallow_current = false;
            return;
        }
        m_escape_state = Normal;
        m_swallow_current = false;
        return;
    case Normal:
        break;
    }

    switch (ch) {
    case '\0':
        return;
    case '\033':
        m_escape_state = ExpectBracket;
        m_swallow_current = false;
        return;
    case 8: // Backspace
        if (m_cursor_column) {
            set_cursor(m_cursor_row, m_cursor_column - 1);
            put_character_at(m_cursor_row, m_cursor_column, ' ');
            return;
        }
        return;
    case '\a':
        if (m_should_beep)
            sysbeep();
        else {
            m_visual_beep_timer.restart(200);
            m_visual_beep_timer.set_single_shot(true);
            m_visual_beep_timer.on_timeout = [this] {
                force_repaint();
            };
            force_repaint();
        }
        return;
    case '\t': {
        for (unsigned i = m_cursor_column; i < columns(); ++i) {
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

    auto new_column = m_cursor_column + 1;
    if (new_column < columns()) {
        put_character_at(m_cursor_row, m_cursor_column, ch);
        set_cursor(m_cursor_row, new_column);
    } else {
        if (m_stomp) {
            m_stomp = false;
            newline();
            put_character_at(m_cursor_row, m_cursor_column, ch);
            set_cursor(m_cursor_row, 1);
        } else {
            // Curious: We wait once on the right-hand side
            m_stomp = true;
            put_character_at(m_cursor_row, m_cursor_column, ch);
        }
    }
}

void Terminal::inject_string(const String& str)
{
    for (int i = 0; i < str.length(); ++i)
        on_char(str[i]);
}

void Terminal::unimplemented_escape()
{
    StringBuilder builder;
    builder.appendf("((Unimplemented escape: %c", m_final);
    if (!m_parameters.is_empty()) {
        builder.append(" parameters:");
        for (int i = 0; i < m_parameters.size(); ++i)
            builder.append((char)m_parameters[i]);
    }
    if (!m_intermediates.is_empty()) {
        builder.append(" intermediates:");
        for (int i = 0; i < m_intermediates.size(); ++i)
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

    m_pixel_width = (frame_thickness() * 2) + (m_inset * 2) + (m_columns * font().glyph_width('x'));
    m_pixel_height = (frame_thickness() * 2) + (m_inset * 2) + (m_rows * (font().glyph_height() + m_line_spacing)) - m_line_spacing;

    set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    set_preferred_size(m_pixel_width, m_pixel_height);

    m_needs_background_fill = true;
    force_repaint();

    winsize ws;
    ws.ws_row = rows;
    ws.ws_col = columns;
    int rc = ioctl(m_ptm_fd, TIOCSWINSZ, &ws);
    ASSERT(rc == 0);
}

Rect Terminal::glyph_rect(u16 row, u16 column)
{
    int y = row * m_line_height;
    int x = column * font().glyph_width('x');
    return { x + frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x'), font().glyph_height() };
}

Rect Terminal::row_rect(u16 row)
{
    int y = row * m_line_height;
    Rect rect = { frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x') * m_columns, font().glyph_height() };
    rect.inflate(0, m_line_spacing);
    return rect;
}

bool Terminal::Line::has_only_one_background_color() const
{
    if (!m_length)
        return true;
    // FIXME: Cache this result?
    auto color = attributes[0].background_color;
    for (size_t i = 1; i < m_length; ++i) {
        if (attributes[i].background_color != color)
            return false;
    }
    return true;
}

void Terminal::event(CEvent& event)
{
    if (event.type() == GEvent::WindowBecameActive || event.type() == GEvent::WindowBecameInactive) {
        m_in_active_window = event.type() == GEvent::WindowBecameActive;
        if (!m_in_active_window) {
            m_cursor_blink_timer.stop();
        } else {
            m_cursor_blink_state = true;
            m_cursor_blink_timer.start();
        }
        invalidate_cursor();
        update();
    }
    return GWidget::event(event);
}

void Terminal::keydown_event(GKeyEvent& event)
{
    // Reset timer so cursor doesn't blink while typing.
    m_cursor_blink_timer.stop();
    m_cursor_blink_state = true;
    m_cursor_blink_timer.start();

    switch (event.key()) {
    case KeyCode::Key_Up:
        write(m_ptm_fd, "\033[A", 3);
        return;
    case KeyCode::Key_Down:
        write(m_ptm_fd, "\033[B", 3);
        return;
    case KeyCode::Key_Right:
        write(m_ptm_fd, "\033[C", 3);
        return;
    case KeyCode::Key_Left:
        write(m_ptm_fd, "\033[D", 3);
        return;
    case KeyCode::Key_Insert:
        write(m_ptm_fd, "\033[2~", 4);
        return;
    case KeyCode::Key_Delete:
        write(m_ptm_fd, "\033[3~", 4);
        return;
    case KeyCode::Key_Home:
        write(m_ptm_fd, "\033[H", 3);
        return;
    case KeyCode::Key_End:
        write(m_ptm_fd, "\033[F", 3);
        return;
    case KeyCode::Key_PageUp:
        write(m_ptm_fd, "\033[5~", 4);
        return;
    case KeyCode::Key_PageDown:
        write(m_ptm_fd, "\033[6~", 4);
        return;
    default:
        break;
    }

    // Key event was not one of the above special cases,
    // attempt to treat it as a character...
    char ch = !event.text().is_empty() ? event.text()[0] : 0;
    if (ch) {
        if (event.ctrl()) {
            if (ch >= 'a' && ch <= 'z') {
                ch = ch - 'a' + 1;
            } else if (ch == '\\') {
                ch = 0x1c;
            }
        }

        // ALT modifier sends escape prefix
        if (event.alt())
            write(m_ptm_fd, "\033", 1);

        write(m_ptm_fd, &ch, 1);
    }
}

void Terminal::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);

    if (m_visual_beep_timer.is_active())
        painter.fill_rect(frame_inner_rect(), Color::Red);
    else
        painter.fill_rect(frame_inner_rect(), Color(Color::Black).with_alpha(m_opacity));
    invalidate_cursor();

    for (u16 row = 0; row < m_rows; ++row) {
        auto& line = this->line(row);
        bool has_only_one_background_color = line.has_only_one_background_color();
        if (m_visual_beep_timer.is_active())
            painter.fill_rect(row_rect(row), Color::Red);
        else if (has_only_one_background_color)
            painter.fill_rect(row_rect(row), lookup_color(line.attributes[0].background_color).with_alpha(m_opacity));
        for (u16 column = 0; column < m_columns; ++column) {
            char ch = line.characters[column];
            bool should_reverse_fill_for_cursor_or_selection = (m_cursor_blink_state && m_in_active_window && row == m_cursor_row && column == m_cursor_column)
                || selection_contains({ row, column });
            auto& attribute = line.attributes[column];
            auto character_rect = glyph_rect(row, column);
            if (!has_only_one_background_color || should_reverse_fill_for_cursor_or_selection) {
                auto cell_rect = character_rect.inflated(0, m_line_spacing);
                painter.fill_rect(cell_rect, lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.foreground_color : attribute.background_color).with_alpha(m_opacity));
            }
            if (ch == ' ')
                continue;
            painter.draw_glyph(character_rect.location(), ch, lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.background_color : attribute.foreground_color));
        }
    }

    if (!m_in_active_window) {
        auto cell_rect = glyph_rect(m_cursor_row, m_cursor_column).inflated(0, m_line_spacing);
        painter.draw_rect(cell_rect, lookup_color(line(m_cursor_row).attributes[m_cursor_column].foreground_color));
    }
}

void Terminal::set_window_title(const String& title)
{
    auto* w = window();
    if (!w)
        return;
    w->set_title(title);
}

void Terminal::invalidate_cursor()
{
    line(m_cursor_row).dirty = true;
}

void Terminal::flush_dirty_lines()
{
    if (m_need_full_flush) {
        update();
        m_need_full_flush = false;
        return;
    }
    Rect rect;
    for (int i = 0; i < m_rows; ++i) {
        if (line(i).dirty)
            rect = rect.united(row_rect(i));
    }
    update(rect);
}

void Terminal::force_repaint()
{
    m_needs_background_fill = true;
    for (int i = 0; i < m_rows; ++i)
        line(i).dirty = true;
    update();
}

void Terminal::resize_event(GResizeEvent& event)
{
    int new_columns = (event.size().width() - frame_thickness() * 2 - m_inset * 2) / font().glyph_width('x');
    int new_rows = (event.size().height() - frame_thickness() * 2 - m_inset * 2) / m_line_height;
    set_size(new_columns, new_rows);
}

void Terminal::apply_size_increments_to_window(GWindow& window)
{
    window.set_size_increment({ font().glyph_width('x'), m_line_height });
    window.set_base_size({ frame_thickness() * 2 + m_inset * 2, frame_thickness() * 2 + m_inset * 2 });
}

void Terminal::update_cursor()
{
    invalidate_cursor();
    flush_dirty_lines();
}

void Terminal::set_opacity(u8 new_opacity)
{
    if (m_opacity == new_opacity)
        return;

    window()->set_has_alpha_channel(new_opacity < 255);
    m_opacity = new_opacity;
    force_repaint();
}

BufferPosition Terminal::normalized_selection_start() const
{
    if (m_selection_start < m_selection_end)
        return m_selection_start;
    return m_selection_end;
}

BufferPosition Terminal::normalized_selection_end() const
{
    if (m_selection_start < m_selection_end)
        return m_selection_end;
    return m_selection_start;
}

bool Terminal::has_selection() const
{
     return m_selection_start.is_valid() && m_selection_end.is_valid();
}

bool Terminal::selection_contains(const BufferPosition& position) const
{
    if (!has_selection())
        return false;

    return position >= normalized_selection_start() && position <= normalized_selection_end();
}

BufferPosition Terminal::buffer_position_at(const Point& position) const
{
    auto adjusted_position = position.translated(-(frame_thickness() + m_inset), -(frame_thickness() + m_inset));
    int row = adjusted_position.y() / m_line_height;
    int column = adjusted_position.x() / font().glyph_width('x');
    if (row < 0)
        row = 0;
    if (column < 0)
        column = 0;
    if (row >= m_rows)
        row = m_rows - 1;
    if (column >= m_columns)
        column = m_columns - 1;
    return { row, column };
}

void Terminal::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        m_selection_start = buffer_position_at(event.position());
        m_selection_end = {};
        update();
    } else if (event.button() == GMouseButton::Right) {
        auto text = GClipboard::the().data();
        if (text.is_empty())
            return;
        int nwritten = write(m_ptm_fd, text.characters(), text.length());
        if (nwritten < 0) {
            perror("write");
            ASSERT_NOT_REACHED();
        }
    }
}

void Terminal::mousemove_event(GMouseEvent& event)
{
    if (!(event.buttons() & GMouseButton::Left))
        return;

    auto old_selection_end = m_selection_end;
    m_selection_end = buffer_position_at(event.position());
    if (old_selection_end != m_selection_end)
        update();
}

void Terminal::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    if (!has_selection())
        return;
    GClipboard::the().set_data(selected_text());
}

String Terminal::selected_text() const
{
    StringBuilder builder;
    auto start = normalized_selection_start();
    auto end = normalized_selection_end();

    for (int row = start.row(); row <= end.row(); ++row) {
        int first_column = row == start.row() ? start.column() : 0;
        int last_column = row == end.row() ? end.column() : m_columns - 1;
        for (int column = first_column; column <= last_column; ++column) {
            auto& line = this->line(row);
            if (line.attributes[column].is_untouched()) {
                builder.append('\n');
                break;
            }
            builder.append(line.characters[column]);
            if (column == line.m_length - 1) {
                builder.append('\n');
            }
        }
    }

    return builder.to_string();
}
