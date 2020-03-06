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

#include "TerminalWidget.h"
#include "XtermColors.h"
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <Kernel/KeyCode.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

//#define TERMINAL_DEBUG

void TerminalWidget::set_pty_master_fd(int fd)
{
    m_ptm_fd = fd;
    if (m_ptm_fd == -1) {
        m_notifier = nullptr;
        return;
    }
    m_notifier = Core::Notifier::construct(m_ptm_fd, Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        u8 buffer[BUFSIZ];
        ssize_t nread = read(m_ptm_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            dbgprintf("Terminal read error: %s\n", strerror(errno));
            perror("read(ptm)");
            GUI::Application::the().quit(1);
            return;
        }
        if (nread == 0) {
            dbgprintf("Terminal: EOF on master pty, firing on_command_exit hook.\n");
            if (on_command_exit)
                on_command_exit();
            int rc = close(m_ptm_fd);
            if (rc < 0) {
                perror("close");
            }
            set_pty_master_fd(-1);
            return;
        }
        for (ssize_t i = 0; i < nread; ++i)
            m_terminal.on_char(buffer[i]);
        flush_dirty_lines();
    };
}

TerminalWidget::TerminalWidget(int ptm_fd, bool automatic_size_policy, RefPtr<Core::ConfigFile> config)
    : m_terminal(*this)
    , m_automatic_size_policy(automatic_size_policy)
    , m_config(move(config))
{
    set_pty_master_fd(ptm_fd);
    m_cursor_blink_timer = add<Core::Timer>();
    m_visual_beep_timer = add<Core::Timer>();

    m_scrollbar = add<GUI::ScrollBar>(Orientation::Vertical);
    m_scrollbar->set_relative_rect(0, 0, 16, 0);
    m_scrollbar->on_change = [this](int) {
        force_repaint();
    };

    dbgprintf("Terminal: Load config file from %s\n", m_config->file_name().characters());
    m_cursor_blink_timer->set_interval(m_config->read_num_entry("Text",
        "CursorBlinkInterval",
        500));
    m_cursor_blink_timer->on_timeout = [this] {
        m_cursor_blink_state = !m_cursor_blink_state;
        update_cursor();
    };

    auto font_entry = m_config->read_entry("Text", "Font", "default");
    if (font_entry == "default")
        set_font(Gfx::Font::default_fixed_width_font());
    else
        set_font(Gfx::Font::load_from_file(font_entry));

    m_line_height = font().glyph_height() + m_line_spacing;

    m_terminal.set_size(m_config->read_num_entry("Window", "Width", 80), m_config->read_num_entry("Window", "Height", 25));

    m_copy_action = GUI::Action::create("Copy", { Mod_Ctrl | Mod_Shift, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"), [this](auto&) {
        copy();
    });

    m_paste_action = GUI::Action::create("Paste", { Mod_Ctrl | Mod_Shift, Key_V }, Gfx::Bitmap::load_from_file("/res/icons/paste16.png"), [this](auto&) {
        paste();
    });
}

TerminalWidget::~TerminalWidget()
{
}

static inline Color lookup_color(unsigned color)
{
    return Color::from_rgb(xterm_colors[color]);
}

Gfx::Rect TerminalWidget::glyph_rect(u16 row, u16 column)
{
    int y = row * m_line_height;
    int x = column * font().glyph_width('x');
    return { x + frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x'), font().glyph_height() };
}

Gfx::Rect TerminalWidget::row_rect(u16 row)
{
    int y = row * m_line_height;
    Gfx::Rect rect = { frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x') * m_terminal.columns(), font().glyph_height() };
    rect.inflate(0, m_line_spacing);
    return rect;
}

void TerminalWidget::set_logical_focus(bool focus)
{
    m_has_logical_focus = focus;
    if (!m_has_logical_focus) {
        m_cursor_blink_timer->stop();
    } else {
        m_cursor_blink_state = true;
        m_cursor_blink_timer->start();
    }
    invalidate_cursor();
    update();
}

void TerminalWidget::focusin_event(Core::Event& event)
{
    set_logical_focus(true);
    return GUI::Frame::focusin_event(event);
}

void TerminalWidget::focusout_event(Core::Event& event)
{
    set_logical_focus(false);
    return GUI::Frame::focusout_event(event);
}

void TerminalWidget::event(Core::Event& event)
{
    if (event.type() == GUI::Event::WindowBecameActive)
        set_logical_focus(true);
    else if (event.type() == GUI::Event::WindowBecameInactive)
        set_logical_focus(false);
    return GUI::Frame::event(event);
}

void TerminalWidget::keydown_event(GUI::KeyEvent& event)
{
    if (m_ptm_fd == -1) {
        event.ignore();
        return GUI::Frame::keydown_event(event);
    }

    // Reset timer so cursor doesn't blink while typing.
    m_cursor_blink_timer->stop();
    m_cursor_blink_state = true;
    m_cursor_blink_timer->start();

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
        if (event.modifiers() == Mod_Shift) {
            m_scrollbar->set_value(m_scrollbar->value() - m_terminal.rows());
            return;
        }
        write(m_ptm_fd, "\033[5~", 4);
        return;
    case KeyCode::Key_PageDown:
        if (event.modifiers() == Mod_Shift) {
            m_scrollbar->set_value(m_scrollbar->value() + m_terminal.rows());
            return;
        }
        write(m_ptm_fd, "\033[6~", 4);
        return;
    case KeyCode::Key_Alt:
        m_alt_key_held = true;
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

        //Clear the selection if we type in/behind it
        auto future_cursor_column = (event.key() == KeyCode::Key_Backspace) ? m_terminal.cursor_column() - 1 : m_terminal.cursor_column();
        auto min_selection_row = min(m_selection_start.row(), m_selection_end.row());
        auto max_selection_row = max(m_selection_start.row(), m_selection_end.row());

        if (future_cursor_column <= last_selection_column_on_row(m_terminal.cursor_row()) && m_terminal.cursor_row() >= min_selection_row && m_terminal.cursor_row() <= max_selection_row) {
            m_selection_end = {};
            update();
        }

        write(m_ptm_fd, &ch, 1);
    }

    if (event.key() != Key_Control && event.key() != Key_Alt && event.key() != Key_Shift)
        m_scrollbar->set_value(m_scrollbar->max());
}

void TerminalWidget::keyup_event(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Alt:
        m_alt_key_held = false;
        return;
    default:
        break;
    }
}

void TerminalWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());

    Gfx::Rect terminal_buffer_rect(frame_inner_rect().top_left(), { frame_inner_rect().width() - m_scrollbar->width(), frame_inner_rect().height() });
    painter.add_clip_rect(terminal_buffer_rect);

    if (m_visual_beep_timer->is_active())
        painter.clear_rect(frame_inner_rect(), Color::Red);
    else
        painter.clear_rect(frame_inner_rect(), Color(Color::Black).with_alpha(m_opacity));
    invalidate_cursor();

    int rows_from_history = 0;
    int first_row_from_history = 0;
    int row_with_cursor = m_terminal.cursor_row();
    if (m_scrollbar->value() != m_scrollbar->max()) {
        rows_from_history = min((int)m_terminal.rows(), m_scrollbar->max() - m_scrollbar->value());
        first_row_from_history = m_terminal.history().size() - (m_scrollbar->max() - m_scrollbar->value());
        row_with_cursor = m_terminal.cursor_row() + rows_from_history;
    }

    auto line_for_visual_row = [&](u16 row) -> const VT::Terminal::Line& {
        if (row < rows_from_history)
            return m_terminal.history().at(first_row_from_history + row);
        return m_terminal.line(row - rows_from_history);
    };

    for (u16 row = 0; row < m_terminal.rows(); ++row) {
        auto row_rect = this->row_rect(row);
        if (!event.rect().contains(row_rect))
            continue;
        auto& line = line_for_visual_row(row);
        bool has_only_one_background_color = line.has_only_one_background_color();
        if (m_visual_beep_timer->is_active())
            painter.clear_rect(row_rect, Color::Red);
        else if (has_only_one_background_color)
            painter.clear_rect(row_rect, lookup_color(line.attributes[0].background_color).with_alpha(m_opacity));

        // The terminal insists on thinking characters and
        // bytes are the same thing. We want to still draw
        // emojis in *some* way, but it won't be completely
        // perfect. So what we do is we make multi-byte
        // characters take up multiple columns, and render
        // the character itself in the center of the columns
        // its bytes take up as far as the terminal is concerned.

        Utf8View utf8_view { line.text() };

        for (auto it = utf8_view.begin(); it != utf8_view.end(); ++it) {
            u32 codepoint = *it;
            int this_char_column = utf8_view.byte_offset_of(it);
            AK::Utf8CodepointIterator it_copy = it;
            int next_char_column = utf8_view.byte_offset_of(++it_copy);

            // Columns from this_char_column up until next_char_column
            // are logically taken up by this (possibly multi-byte)
            // character. Iterate over these columns and draw background
            // for each one of them separately.

            bool should_reverse_fill_for_cursor_or_selection = false;
            VT::Attribute attribute;

            for (u16 column = this_char_column; column < next_char_column; ++column) {
                should_reverse_fill_for_cursor_or_selection |= m_cursor_blink_state
                    && m_has_logical_focus
                    && row == row_with_cursor
                    && column == m_terminal.cursor_column();
                should_reverse_fill_for_cursor_or_selection |= selection_contains({ row, column });
                attribute = line.attributes[column];
                auto character_rect = glyph_rect(row, column);
                auto cell_rect = character_rect.inflated(0, m_line_spacing);
                if (!has_only_one_background_color || should_reverse_fill_for_cursor_or_selection) {
                    painter.clear_rect(cell_rect, lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.foreground_color : attribute.background_color).with_alpha(m_opacity));
                }
                if (attribute.flags & VT::Attribute::Underline)
                    painter.draw_line(cell_rect.bottom_left(), cell_rect.bottom_right(), lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.background_color : attribute.foreground_color));
            }

            if (codepoint == ' ')
                continue;

            auto character_rect = glyph_rect(row, this_char_column);
            auto num_columns = next_char_column - this_char_column;
            character_rect.move_by((num_columns - 1) * font().glyph_width('x') / 2, 0);
            painter.draw_glyph_or_emoji(
                character_rect.location(),
                codepoint,
                attribute.flags & VT::Attribute::Bold ? bold_font() : font(),
                lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.background_color : attribute.foreground_color));
        }
    }

    if (!m_has_logical_focus && row_with_cursor < m_terminal.rows()) {
        auto& cursor_line = line_for_visual_row(row_with_cursor);
        if (m_terminal.cursor_row() < (m_terminal.rows() - rows_from_history)) {
            auto cell_rect = glyph_rect(row_with_cursor, m_terminal.cursor_column()).inflated(0, m_line_spacing);
            painter.draw_rect(cell_rect, lookup_color(cursor_line.attributes[m_terminal.cursor_column()].foreground_color));
        }
    }
}

void TerminalWidget::set_window_title(const StringView& title)
{
    if (on_title_change)
        on_title_change(title);
}

void TerminalWidget::invalidate_cursor()
{
    m_terminal.invalidate_cursor();
}

void TerminalWidget::flush_dirty_lines()
{
    // FIXME: Update smarter when scrolled
    if (m_terminal.m_need_full_flush || m_scrollbar->value() != m_scrollbar->max()) {
        update();
        m_terminal.m_need_full_flush = false;
        return;
    }
    Gfx::Rect rect;
    for (int i = 0; i < m_terminal.rows(); ++i) {
        if (m_terminal.line(i).dirty) {
            rect = rect.united(row_rect(i));
            m_terminal.line(i).dirty = false;
        }
    }
    update(rect);
}

void TerminalWidget::force_repaint()
{
    m_needs_background_fill = true;
    update();
}

void TerminalWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout(event.size());
}

void TerminalWidget::relayout(const Gfx::Size& size)
{
    if (!m_scrollbar)
        return;

    auto base_size = compute_base_size();
    int new_columns = (size.width() - base_size.width()) / font().glyph_width('x');
    int new_rows = (size.height() - base_size.height()) / m_line_height;
    m_terminal.set_size(new_columns, new_rows);

    Gfx::Rect scrollbar_rect = {
        size.width() - m_scrollbar->width() - frame_thickness(),
        frame_thickness(),
        m_scrollbar->width(),
        size.height() - frame_thickness() * 2,
    };
    m_scrollbar->set_relative_rect(scrollbar_rect);
}

Gfx::Size TerminalWidget::compute_base_size() const
{
    int base_width = frame_thickness() * 2 + m_inset * 2 + m_scrollbar->width();
    int base_height = frame_thickness() * 2 + m_inset * 2;
    return { base_width, base_height };
}

void TerminalWidget::apply_size_increments_to_window(GUI::Window& window)
{
    window.set_size_increment({ font().glyph_width('x'), m_line_height });
    window.set_base_size(compute_base_size());
}

void TerminalWidget::update_cursor()
{
    invalidate_cursor();
    flush_dirty_lines();
}

void TerminalWidget::set_opacity(u8 new_opacity)
{
    if (m_opacity == new_opacity)
        return;

    window()->set_has_alpha_channel(new_opacity < 255);
    m_opacity = new_opacity;
    force_repaint();
}

VT::Position TerminalWidget::normalized_selection_start() const
{
    if (m_selection_start < m_selection_end)
        return m_selection_start;
    return m_selection_end;
}

VT::Position TerminalWidget::normalized_selection_end() const
{
    if (m_selection_start < m_selection_end)
        return m_selection_end;
    return m_selection_start;
}

bool TerminalWidget::has_selection() const
{
    return m_selection_start.is_valid() && m_selection_end.is_valid();
}

bool TerminalWidget::selection_contains(const VT::Position& position) const
{
    if (!has_selection())
        return false;

    if (m_rectangle_selection) {
        auto min_selection_column = min(m_selection_start.column(), m_selection_end.column());
        auto max_selection_column = max(m_selection_start.column(), m_selection_end.column());
        auto min_selection_row = min(m_selection_start.row(), m_selection_end.row());
        auto max_selection_row = max(m_selection_start.row(), m_selection_end.row());

        return position.column() >= min_selection_column && position.column() <= max_selection_column && position.row() >= min_selection_row && position.row() <= max_selection_row;
    }

    return position >= normalized_selection_start() && position <= normalized_selection_end();
}

VT::Position TerminalWidget::buffer_position_at(const Gfx::Point& position) const
{
    auto adjusted_position = position.translated(-(frame_thickness() + m_inset), -(frame_thickness() + m_inset));
    int row = adjusted_position.y() / m_line_height;
    int column = adjusted_position.x() / font().glyph_width('x');
    if (row < 0)
        row = 0;
    if (column < 0)
        column = 0;
    if (row >= m_terminal.rows())
        row = m_terminal.rows() - 1;
    if (column >= m_terminal.columns())
        column = m_terminal.columns() - 1;
    return { row, column };
}

void TerminalWidget::doubleclick_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        m_triple_click_timer.start();

        auto position = buffer_position_at(event.position());
        auto& line = m_terminal.line(position.row());
        bool want_whitespace = line.characters[position.column()] == ' ';

        int start_column = 0;
        int end_column = 0;

        for (int column = position.column(); column >= 0 && (line.characters[column] == ' ') == want_whitespace; --column) {
            start_column = column;
        }

        for (int column = position.column(); column < m_terminal.columns() && (line.characters[column] == ' ') == want_whitespace; ++column) {
            end_column = column;
        }

        m_selection_start = { position.row(), start_column };
        m_selection_end = { position.row(), end_column };
    }
    GUI::Frame::doubleclick_event(event);
}

void TerminalWidget::paste()
{
    if (m_ptm_fd == -1)
        return;
    auto text = GUI::Clipboard::the().data();
    if (text.is_empty())
        return;
    int nwritten = write(m_ptm_fd, text.characters(), text.length());
    if (nwritten < 0) {
        perror("write");
        ASSERT_NOT_REACHED();
    }
}

void TerminalWidget::copy()
{
    if (has_selection())
        GUI::Clipboard::the().set_data(selected_text());
}

void TerminalWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
            int start_column = 0;
            int end_column = m_terminal.columns() - 1;

            auto position = buffer_position_at(event.position());
            m_selection_start = { position.row(), start_column };
            m_selection_end = { position.row(), end_column };
        } else {
            m_selection_start = buffer_position_at(event.position());
            m_selection_end = {};
        }
        if (m_alt_key_held)
            m_rectangle_selection = true;
        else if (m_rectangle_selection)
            m_rectangle_selection = false;

        update();
    }
}

void TerminalWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!(event.buttons() & GUI::MouseButton::Left))
        return;

    auto old_selection_end = m_selection_end;
    m_selection_end = buffer_position_at(event.position());
    if (old_selection_end != m_selection_end)
        update();
}

void TerminalWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (!is_scrollable())
        return;
    m_scrollbar->set_value(m_scrollbar->value() + event.wheel_delta());
    GUI::Frame::mousewheel_event(event);
}

bool TerminalWidget::is_scrollable() const
{
    return m_scrollbar->is_scrollable();
}

String TerminalWidget::selected_text() const
{
    StringBuilder builder;
    auto start = normalized_selection_start();
    auto end = normalized_selection_end();

    for (int row = start.row(); row <= end.row(); ++row) {
        int first_column = first_selection_column_on_row(row);
        int last_column = last_selection_column_on_row(row);
        for (int column = first_column; column <= last_column; ++column) {
            auto& line = m_terminal.line(row);
            if (line.attributes[column].is_untouched()) {
                builder.append('\n');
                break;
            }
            builder.append(line.characters[column]);
            if (column == line.m_length - 1 || (m_rectangle_selection && column == last_column)) {
                builder.append('\n');
            }
        }
    }

    return builder.to_string();
}

int TerminalWidget::first_selection_column_on_row(int row) const
{
    return row == normalized_selection_start().row() || m_rectangle_selection ? normalized_selection_start().column() : 0;
}

int TerminalWidget::last_selection_column_on_row(int row) const
{
    return row == normalized_selection_end().row() || m_rectangle_selection ? normalized_selection_end().column() : m_terminal.columns() - 1;
}

void TerminalWidget::terminal_history_changed()
{
    bool was_max = m_scrollbar->value() == m_scrollbar->max();
    m_scrollbar->set_max(m_terminal.history().size());
    if (was_max)
        m_scrollbar->set_value(m_scrollbar->max());
    m_scrollbar->update();
}

void TerminalWidget::terminal_did_resize(u16 columns, u16 rows)
{
    m_pixel_width = (frame_thickness() * 2) + (m_inset * 2) + (columns * font().glyph_width('x')) + m_scrollbar->width();
    m_pixel_height = (frame_thickness() * 2) + (m_inset * 2) + (rows * (font().glyph_height() + m_line_spacing));

    if (m_automatic_size_policy) {
        set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        set_preferred_size(m_pixel_width, m_pixel_height);
    }

    m_needs_background_fill = true;
    force_repaint();

    winsize ws;
    ws.ws_row = rows;
    ws.ws_col = columns;
    if (m_ptm_fd != -1) {
        int rc = ioctl(m_ptm_fd, TIOCSWINSZ, &ws);
        ASSERT(rc == 0);
    }
}

void TerminalWidget::beep()
{
    if (m_should_beep) {
        sysbeep();
        return;
    }
    m_visual_beep_timer->restart(200);
    m_visual_beep_timer->set_single_shot(true);
    m_visual_beep_timer->on_timeout = [this] {
        force_repaint();
    };
    force_repaint();
}

void TerminalWidget::emit_char(u8 ch)
{
    if (write(m_ptm_fd, &ch, 1) < 0) {
        perror("emit_char: write");
    }
}

void TerminalWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_context_menu->add_action(copy_action());
        m_context_menu->add_action(paste_action());
    }
    m_context_menu->popup(event.screen_position());
}

void TerminalWidget::drop_event(GUI::DropEvent& event)
{
    if (event.mime_data().has_text()) {
        event.accept();
        auto text = event.mime_data().text();
        write(m_ptm_fd, text.characters(), text.length());
    } else if (event.mime_data().has_urls()) {
        event.accept();
        auto urls = event.mime_data().urls();
        bool first = true;
        for (auto& url : event.mime_data().urls()) {
            if (!first) {
                write(m_ptm_fd, " ", 1);
                first = false;
            }
            if (url.protocol() == "file")
                write(m_ptm_fd, url.path().characters(), url.path().length());
            else
                write(m_ptm_fd, url.to_string().characters(), url.to_string().length());
        }
    }
}

void TerminalWidget::did_change_font()
{
    GUI::Frame::did_change_font();
    m_line_height = font().glyph_height() + m_line_spacing;

    // TODO: try to find a bold version of the new font (e.g. CsillaThin7x10 -> CsillaBold7x10)
    const Gfx::Font& bold_font = Gfx::Font::default_bold_fixed_width_font();

    if (bold_font.glyph_height() == font().glyph_height() && bold_font.glyph_width(' ') == font().glyph_width(' '))
        m_bold_font = &bold_font;
    else
        m_bold_font = font();

    if (!size().is_empty())
        relayout(size());
}
