#include "TerminalWidget.h"
#include "XtermColors.h"
#include <AK/AKString.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibDraw/Font.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GWindow.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

//#define TERMINAL_DEBUG

TerminalWidget::TerminalWidget(int ptm_fd, RefPtr<CConfigFile> config)
    : m_terminal(*this)
    , m_ptm_fd(ptm_fd)
    , m_notifier(ptm_fd, CNotifier::Read)
    , m_config(move(config))
{
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_thickness(2);

    m_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_scrollbar->set_relative_rect(0, 0, 16, 0);
    m_scrollbar->on_change = [this](int) {
        force_repaint();
    };

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
            m_terminal.on_char(buffer[i]);
        flush_dirty_lines();
    };

    m_line_height = font().glyph_height() + m_line_spacing;

    m_terminal.set_size(m_config->read_num_entry("Window", "Width", 80), m_config->read_num_entry("Window", "Height", 25));
}

TerminalWidget::~TerminalWidget()
{
}

static inline Color lookup_color(unsigned color)
{
    return Color::from_rgb(xterm_colors[color]);
}

Rect TerminalWidget::glyph_rect(u16 row, u16 column)
{
    int y = row * m_line_height;
    int x = column * font().glyph_width('x');
    return { x + frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x'), font().glyph_height() };
}

Rect TerminalWidget::row_rect(u16 row)
{
    int y = row * m_line_height;
    Rect rect = { frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x') * m_terminal.columns(), font().glyph_height() };
    rect.inflate(0, m_line_spacing);
    return rect;
}

void TerminalWidget::event(CEvent& event)
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

void TerminalWidget::keydown_event(GKeyEvent& event)
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
}

void TerminalWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);

    painter.add_clip_rect(event.rect());

    if (m_visual_beep_timer.is_active())
        painter.fill_rect(frame_inner_rect(), Color::Red);
    else
        painter.fill_rect(frame_inner_rect(), Color(Color::Black).with_alpha(m_opacity));
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
        if (m_visual_beep_timer.is_active())
            painter.fill_rect(row_rect, Color::Red);
        else if (has_only_one_background_color)
            painter.fill_rect(row_rect, lookup_color(line.attributes[0].background_color).with_alpha(m_opacity));
        for (u16 column = 0; column < m_terminal.columns(); ++column) {
            char ch = line.characters[column];
            bool should_reverse_fill_for_cursor_or_selection = (m_cursor_blink_state && m_in_active_window && row == row_with_cursor && column == m_terminal.cursor_column())
                || selection_contains({ row, column });
            auto& attribute = line.attributes[column];
            auto character_rect = glyph_rect(row, column);
            if (!has_only_one_background_color || should_reverse_fill_for_cursor_or_selection) {
                auto cell_rect = character_rect.inflated(0, m_line_spacing);
                painter.fill_rect(cell_rect, lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.foreground_color : attribute.background_color).with_alpha(m_opacity));
            }
            if (ch == ' ')
                continue;
            painter.draw_glyph(
                character_rect.location(),
                ch,
                attribute.flags & VT::Attribute::Bold ? Font::default_bold_fixed_width_font() : font(),
                lookup_color(should_reverse_fill_for_cursor_or_selection ? attribute.background_color : attribute.foreground_color));
        }
    }

    if (!m_in_active_window && row_with_cursor < m_terminal.rows()) {
        auto& cursor_line = line_for_visual_row(row_with_cursor);
        if (m_terminal.cursor_row() < (m_terminal.rows() - rows_from_history)) {
            auto cell_rect = glyph_rect(row_with_cursor, m_terminal.cursor_column()).inflated(0, m_line_spacing);
            painter.draw_rect(cell_rect, lookup_color(cursor_line.attributes[m_terminal.cursor_column()].foreground_color));
        }
    }
}

void TerminalWidget::set_window_title(const StringView& title)
{
    auto* w = window();
    if (!w)
        return;
    w->set_title(title);
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
    Rect rect;
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

void TerminalWidget::resize_event(GResizeEvent& event)
{
    auto base_size = compute_base_size();
    int new_columns = (event.size().width() - base_size.width()) / font().glyph_width('x');
    int new_rows = (event.size().height() - base_size.height()) / m_line_height;
    m_terminal.set_size(new_columns, new_rows);

    Rect scrollbar_rect = {
        event.size().width() - m_scrollbar->width() - frame_thickness(),
        frame_thickness(),
        m_scrollbar->width(),
        event.size().height() - frame_thickness() * 2,
    };
    m_scrollbar->set_relative_rect(scrollbar_rect);
}

Size TerminalWidget::compute_base_size() const
{
    int base_width = frame_thickness() * 2 + m_inset * 2 + m_scrollbar->width();
    int base_height = frame_thickness() * 2 + m_inset * 2;
    return { base_width, base_height };
}

void TerminalWidget::apply_size_increments_to_window(GWindow& window)
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

    return position >= normalized_selection_start() && position <= normalized_selection_end();
}

VT::Position TerminalWidget::buffer_position_at(const Point& position) const
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

void TerminalWidget::doubleclick_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
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

        if (has_selection())
            GClipboard::the().set_data(selected_text());
    }
    GFrame::doubleclick_event(event);
}

void TerminalWidget::mousedown_event(GMouseEvent& event)
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

void TerminalWidget::mousemove_event(GMouseEvent& event)
{
    if (!(event.buttons() & GMouseButton::Left))
        return;

    auto old_selection_end = m_selection_end;
    m_selection_end = buffer_position_at(event.position());
    if (old_selection_end != m_selection_end)
        update();
}

void TerminalWidget::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    if (!has_selection())
        return;
    GClipboard::the().set_data(selected_text());
}

void TerminalWidget::mousewheel_event(GMouseEvent& event)
{
    if (!is_scrollable())
        return;
    m_scrollbar->set_value(m_scrollbar->value() + event.wheel_delta());
    GFrame::mousewheel_event(event);
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
            if (column == line.m_length - 1) {
                builder.append('\n');
            }
        }
    }

    return builder.to_string();
}

int TerminalWidget::first_selection_column_on_row(int row) const
{
    return row == normalized_selection_start().row() ? normalized_selection_start().column() : 0;
}

int TerminalWidget::last_selection_column_on_row(int row) const
{
    return row == normalized_selection_end().row() ? normalized_selection_end().column() : m_terminal.columns() - 1;
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
    m_pixel_width = (frame_thickness() * 2) + (m_inset * 2) + (columns * font().glyph_width('x'));
    m_pixel_height = (frame_thickness() * 2) + (m_inset * 2) + (rows * (font().glyph_height() + m_line_spacing)) - m_line_spacing;

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

void TerminalWidget::beep()
{
    if (m_should_beep) {
        sysbeep();
        return;
    }
    m_visual_beep_timer.restart(200);
    m_visual_beep_timer.set_single_shot(true);
    m_visual_beep_timer.on_timeout = [this] {
        force_repaint();
    };
    force_repaint();
}
