/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibConfig/Client.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/MimeData.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

namespace VT {

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
            dbgln("Terminal read error: {}", strerror(errno));
            perror("read(ptm)");
            GUI::Application::the()->quit(1);
            return;
        }
        if (nread == 0) {
            dbgln("TerminalWidget: EOF on master pty, firing on_command_exit hook.");
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
            m_terminal.on_input(buffer[i]);
        flush_dirty_lines();
    };
}

TerminalWidget::TerminalWidget(int ptm_fd, bool automatic_size_policy)
    : m_terminal(*this)
    , m_automatic_size_policy(automatic_size_policy)
{
    static_assert(sizeof(m_colors) == sizeof(xterm_colors));
    memcpy(m_colors, xterm_colors, sizeof(m_colors));

    set_override_cursor(Gfx::StandardCursor::IBeam);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_accepts_emoji_input(true);
    set_pty_master_fd(ptm_fd);
    m_cursor_blink_timer = add<Core::Timer>();
    m_visual_beep_timer = add<Core::Timer>();
    m_auto_scroll_timer = add<Core::Timer>();

    m_scrollbar = add<GUI::Scrollbar>(Orientation::Vertical);
    m_scrollbar->set_scroll_animation(GUI::Scrollbar::Animation::CoarseScroll);
    m_scrollbar->set_relative_rect(0, 0, 16, 0);
    m_scrollbar->on_change = [this](int) {
        update();
    };

    m_cursor_blink_timer->set_interval(Config::read_i32("Terminal", "Text",
        "CursorBlinkInterval",
        500));
    m_cursor_blink_timer->on_timeout = [this] {
        m_cursor_blink_state = !m_cursor_blink_state;
        update_cursor();
    };

    m_auto_scroll_timer->set_interval(50);
    m_auto_scroll_timer->on_timeout = [this] {
        if (m_auto_scroll_direction != AutoScrollDirection::None) {
            int scroll_amount = m_auto_scroll_direction == AutoScrollDirection::Up ? -1 : 1;
            m_scrollbar->increase_slider_by(scroll_amount);
        }
    };
    m_auto_scroll_timer->start();

    auto font_entry = Config::read_string("Terminal", "Text", "Font", "default");
    if (font_entry == "default")
        set_font(Gfx::FontDatabase::default_fixed_width_font());
    else
        set_font(Gfx::FontDatabase::the().get_by_name(font_entry));

    m_line_height = font().glyph_height() + m_line_spacing;

    m_terminal.set_size(Config::read_i32("Terminal", "Window", "Width", 80), Config::read_i32("Terminal", "Window", "Height", 25));

    m_copy_action = GUI::Action::create("&Copy", { Mod_Ctrl | Mod_Shift, Key_C }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        copy();
    });
    m_copy_action->set_swallow_key_event_when_disabled(true);

    m_paste_action = GUI::Action::create("&Paste", { Mod_Ctrl | Mod_Shift, Key_V }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/paste.png").release_value_but_fixme_should_propagate_errors(), [this](auto&) {
        paste();
    });
    m_paste_action->set_swallow_key_event_when_disabled(true);

    m_clear_including_history_action = GUI::Action::create("Clear Including &History", { Mod_Ctrl | Mod_Shift, Key_K }, [this](auto&) {
        clear_including_history();
    });

    m_context_menu = GUI::Menu::construct();
    m_context_menu->add_action(copy_action());
    m_context_menu->add_action(paste_action());
    m_context_menu->add_separator();
    m_context_menu->add_action(clear_including_history_action());

    update_copy_action();
    update_paste_action();

    set_color_scheme(Config::read_string("Terminal", "Window", "ColorScheme", "Default"));
}

Gfx::IntRect TerminalWidget::glyph_rect(u16 row, u16 column)
{
    int y = row * m_line_height;
    int x = column * font().glyph_width('x');
    return { x + frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x'), font().glyph_height() };
}

Gfx::IntRect TerminalWidget::row_rect(u16 row)
{
    int y = row * m_line_height;
    Gfx::IntRect rect = { frame_thickness() + m_inset, y + frame_thickness() + m_inset, font().glyph_width('x') * m_terminal.columns(), font().glyph_height() };
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
    set_auto_scroll_direction(AutoScrollDirection::None);
    invalidate_cursor();
    update();
}

void TerminalWidget::focusin_event(GUI::FocusEvent& event)
{
    set_logical_focus(true);
    return GUI::Frame::focusin_event(event);
}

void TerminalWidget::focusout_event(GUI::FocusEvent& event)
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

    if (event.key() == KeyCode::Key_PageUp && event.modifiers() == Mod_Shift) {
        m_scrollbar->decrease_slider_by(m_terminal.rows());
        return;
    }
    if (event.key() == KeyCode::Key_PageDown && event.modifiers() == Mod_Shift) {
        m_scrollbar->increase_slider_by(m_terminal.rows());
        return;
    }
    if (event.key() == KeyCode::Key_Alt) {
        m_alt_key_held = true;
        return;
    }

    // Clear the selection if we type in/behind it.
    auto future_cursor_column = (event.key() == KeyCode::Key_Backspace) ? m_terminal.cursor_column() - 1 : m_terminal.cursor_column();
    auto min_selection_row = min(m_selection.start().row(), m_selection.end().row());
    auto max_selection_row = max(m_selection.start().row(), m_selection.end().row());

    if (future_cursor_column <= last_selection_column_on_row(m_terminal.cursor_row()) && m_terminal.cursor_row() >= min_selection_row && m_terminal.cursor_row() <= max_selection_row) {
        m_selection.set_end({});
        update_copy_action();
        update();
    }

    m_terminal.handle_key_press(event.key(), event.code_point(), event.modifiers());

    if (event.key() != Key_Control && event.key() != Key_Alt && event.key() != Key_LeftShift && event.key() != Key_RightShift && event.key() != Key_Super)
        scroll_to_bottom();
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

    auto visual_beep_active = m_visual_beep_timer->is_active();

    painter.add_clip_rect(event.rect());

    if (visual_beep_active)
        painter.clear_rect(frame_inner_rect(), terminal_color_to_rgb(VT::Color::named(VT::Color::ANSIColor::Red)));
    else
        painter.clear_rect(frame_inner_rect(), terminal_color_to_rgb(VT::Color::named(VT::Color::ANSIColor::DefaultBackground)).with_alpha(m_opacity));
    invalidate_cursor();

    int rows_from_history = 0;
    int first_row_from_history = m_terminal.history_size();
    int row_with_cursor = m_terminal.cursor_row();
    if (m_scrollbar->value() != m_scrollbar->max()) {
        rows_from_history = min((int)m_terminal.rows(), m_scrollbar->max() - m_scrollbar->value());
        first_row_from_history = m_terminal.history_size() - (m_scrollbar->max() - m_scrollbar->value());
        row_with_cursor = m_terminal.cursor_row() + rows_from_history;
    }

    // Pass: Compute the rect(s) of the currently hovered link, if any.
    Vector<Gfx::IntRect> hovered_href_rects;
    if (!m_hovered_href_id.is_null()) {
        for (u16 visual_row = 0; visual_row < m_terminal.rows(); ++visual_row) {
            auto& line = m_terminal.line(first_row_from_history + visual_row);
            for (size_t column = 0; column < line.length(); ++column) {
                if (m_hovered_href_id == line.attribute_at(column).href_id) {
                    bool merged_with_existing_rect = false;
                    auto glyph_rect = this->glyph_rect(visual_row, column);
                    for (auto& rect : hovered_href_rects) {
                        if (rect.inflated(1, 1).intersects(glyph_rect)) {
                            rect = rect.united(glyph_rect);
                            merged_with_existing_rect = true;
                            break;
                        }
                    }
                    if (!merged_with_existing_rect)
                        hovered_href_rects.append(glyph_rect);
                }
            }
        }
    }

    // Pass: Paint background & text decorations.
    for (u16 visual_row = 0; visual_row < m_terminal.rows(); ++visual_row) {
        auto row_rect = this->row_rect(visual_row);
        if (!event.rect().contains(row_rect))
            continue;
        auto& line = m_terminal.line(first_row_from_history + visual_row);
        bool has_only_one_background_color = line.has_only_one_background_color();
        if (visual_beep_active)
            painter.clear_rect(row_rect, terminal_color_to_rgb(VT::Color::named(VT::Color::ANSIColor::Red)));
        else if (has_only_one_background_color)
            painter.clear_rect(row_rect, terminal_color_to_rgb(line.attribute_at(0).effective_background_color()).with_alpha(m_opacity));

        for (size_t column = 0; column < line.length(); ++column) {
            bool should_reverse_fill_for_cursor_or_selection = m_cursor_blink_state
                && (m_cursor_style == VT::CursorStyle::SteadyBlock || m_cursor_style == VT::CursorStyle::BlinkingBlock)
                && m_has_logical_focus
                && visual_row == row_with_cursor
                && column == m_terminal.cursor_column();
            should_reverse_fill_for_cursor_or_selection |= selection_contains({ first_row_from_history + visual_row, (int)column });
            auto attribute = line.attribute_at(column);
            auto character_rect = glyph_rect(visual_row, column);
            auto cell_rect = character_rect.inflated(0, m_line_spacing);
            auto text_color_before_bold_change = should_reverse_fill_for_cursor_or_selection ? attribute.effective_background_color() : attribute.effective_foreground_color();
            auto text_color = terminal_color_to_rgb(m_show_bold_text_as_bright ? text_color_before_bold_change.to_bright() : text_color_before_bold_change);
            if ((!visual_beep_active && !has_only_one_background_color) || should_reverse_fill_for_cursor_or_selection)
                painter.clear_rect(cell_rect, terminal_color_to_rgb(should_reverse_fill_for_cursor_or_selection ? attribute.effective_foreground_color() : attribute.effective_background_color()));

            if constexpr (TERMINAL_DEBUG) {
                if (line.termination_column() == column)
                    painter.clear_rect(cell_rect, Gfx::Color::Magenta);
            }

            enum class UnderlineStyle {
                None,
                Dotted,
                Solid,
            };

            auto underline_style = UnderlineStyle::None;
            auto underline_color = text_color;

            if (has_flag(attribute.flags, VT::Attribute::Flags::Underline)) {
                // Content has specified underline
                underline_style = UnderlineStyle::Solid;
            } else if (!attribute.href.is_empty()) {
                // We're hovering a hyperlink
                if (m_hovered_href_id == attribute.href_id || m_active_href_id == attribute.href_id) {
                    underline_style = UnderlineStyle::Solid;
                    underline_color = palette().active_link();
                } else {
                    underline_style = UnderlineStyle::Dotted;
                    underline_color = text_color.darkened(0.6f);
                }
            }

            if (underline_style == UnderlineStyle::Solid) {
                painter.draw_line(cell_rect.bottom_left(), cell_rect.bottom_right(), underline_color);
            } else if (underline_style == UnderlineStyle::Dotted) {
                int x1 = cell_rect.bottom_left().x();
                int x2 = cell_rect.bottom_right().x();
                int y = cell_rect.bottom_left().y();
                for (int x = x1; x <= x2; ++x) {
                    if ((x % 3) == 0)
                        painter.set_pixel({ x, y }, underline_color);
                }
            }
        }
    }

    // Paint the hovered link rects, if any.
    for (auto rect : hovered_href_rects) {
        rect.inflate(6, 6);
        painter.fill_rect(rect, palette().base());
        painter.draw_rect(rect.inflated(2, 2).intersected(frame_inner_rect()), palette().base_text());
    }

    auto& font = this->font();
    auto& bold_font = font.bold_variant();

    // Pass: Paint foreground (text).
    for (u16 visual_row = 0; visual_row < m_terminal.rows(); ++visual_row) {
        auto row_rect = this->row_rect(visual_row);
        if (!event.rect().contains(row_rect))
            continue;
        auto& line = m_terminal.line(first_row_from_history + visual_row);
        for (size_t column = 0; column < line.length(); ++column) {
            auto attribute = line.attribute_at(column);
            bool should_reverse_fill_for_cursor_or_selection = m_cursor_blink_state
                && (m_cursor_style == VT::CursorStyle::SteadyBlock || m_cursor_style == VT::CursorStyle::BlinkingBlock)
                && m_has_logical_focus
                && visual_row == row_with_cursor
                && column == m_terminal.cursor_column();
            should_reverse_fill_for_cursor_or_selection |= selection_contains({ first_row_from_history + visual_row, (int)column });
            auto text_color_before_bold_change = should_reverse_fill_for_cursor_or_selection ? attribute.effective_background_color() : attribute.effective_foreground_color();
            auto text_color = terminal_color_to_rgb(m_show_bold_text_as_bright ? text_color_before_bold_change.to_bright() : text_color_before_bold_change);
            u32 code_point = line.code_point(column);

            if (code_point == ' ')
                continue;

            auto character_rect = glyph_rect(visual_row, column);

            if (!m_hovered_href_id.is_null() && attribute.href_id == m_hovered_href_id) {
                text_color = palette().base_text();
            }

            painter.draw_glyph_or_emoji(
                character_rect.location(),
                code_point,
                has_flag(attribute.flags, VT::Attribute::Flags::Bold) ? bold_font : font,
                text_color);
        }
    }

    // Draw cursor.
    if (m_cursor_blink_state && row_with_cursor < m_terminal.rows()) {
        auto& cursor_line = m_terminal.line(first_row_from_history + row_with_cursor);
        if (m_terminal.cursor_row() >= (m_terminal.rows() - rows_from_history))
            return;

        if (m_has_logical_focus && (m_cursor_style == VT::CursorStyle::BlinkingBlock || m_cursor_style == VT::CursorStyle::SteadyBlock))
            return; // This has already been handled by inverting the cell colors

        auto cursor_color = terminal_color_to_rgb(cursor_line.attribute_at(m_terminal.cursor_column()).effective_foreground_color());
        auto cell_rect = glyph_rect(row_with_cursor, m_terminal.cursor_column()).inflated(0, m_line_spacing);
        if (m_cursor_style == VT::CursorStyle::BlinkingUnderline || m_cursor_style == VT::CursorStyle::SteadyUnderline) {
            auto x1 = cell_rect.bottom_left().x();
            auto x2 = cell_rect.bottom_right().x();
            auto y = cell_rect.bottom_left().y();
            for (auto x = x1; x <= x2; ++x)
                painter.set_pixel({ x, y }, cursor_color);
        } else if (m_cursor_style == VT::CursorStyle::BlinkingBar || m_cursor_style == VT::CursorStyle::SteadyBar) {
            auto x = cell_rect.bottom_left().x();
            auto y1 = cell_rect.top_left().y();
            auto y2 = cell_rect.bottom_left().y();
            for (auto y = y1; y <= y2; ++y)
                painter.set_pixel({ x, y }, cursor_color);
        } else {
            // We fall back to a block if we don't support the selected cursor type.
            painter.draw_rect(cell_rect, cursor_color);
        }
    }
}

void TerminalWidget::set_window_progress(int value, int max)
{
    float float_value = value;
    float float_max = max;
    float progress = (float_value / float_max) * 100.0f;
    window()->set_progress((int)roundf(progress));
}

void TerminalWidget::set_window_title(StringView title)
{
    if (!Utf8View(title).validate()) {
        dbgln("TerminalWidget: Attempted to set window title to invalid UTF-8 string");
        return;
    }

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
    Gfx::IntRect rect;
    for (int i = 0; i < m_terminal.rows(); ++i) {
        if (m_terminal.visible_line(i).is_dirty()) {
            rect = rect.united(row_rect(i));
            m_terminal.visible_line(i).set_dirty(false);
        }
    }
    update(rect);
}

void TerminalWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout(event.size());
}

void TerminalWidget::relayout(Gfx::IntSize const& size)
{
    if (!m_scrollbar)
        return;

    TemporaryChange change(m_in_relayout, true);

    auto base_size = compute_base_size();
    int new_columns = (size.width() - base_size.width()) / font().glyph_width('x');
    int new_rows = (size.height() - base_size.height()) / m_line_height;
    m_terminal.set_size(new_columns, new_rows);

    Gfx::IntRect scrollbar_rect = {
        size.width() - m_scrollbar->width() - frame_thickness(),
        frame_thickness(),
        m_scrollbar->width(),
        size.height() - frame_thickness() * 2,
    };
    m_scrollbar->set_relative_rect(scrollbar_rect);
    m_scrollbar->set_page_step(new_rows);
}

Gfx::IntSize TerminalWidget::compute_base_size() const
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
    update();
}

void TerminalWidget::set_show_scrollbar(bool show_scrollbar)
{
    m_scrollbar->set_visible(show_scrollbar);
}

bool TerminalWidget::has_selection() const
{
    return m_selection.is_valid();
}

void TerminalWidget::set_selection(const VT::Range& selection)
{
    m_selection = selection;
    update_copy_action();
    update();
}

bool TerminalWidget::selection_contains(const VT::Position& position) const
{
    if (!has_selection())
        return false;

    if (m_rectangle_selection) {
        auto m_selection_start = m_selection.start();
        auto m_selection_end = m_selection.end();
        auto min_selection_column = min(m_selection_start.column(), m_selection_end.column());
        auto max_selection_column = max(m_selection_start.column(), m_selection_end.column());
        auto min_selection_row = min(m_selection_start.row(), m_selection_end.row());
        auto max_selection_row = max(m_selection_start.row(), m_selection_end.row());

        return position.column() >= min_selection_column && position.column() <= max_selection_column && position.row() >= min_selection_row && position.row() <= max_selection_row;
    }

    auto normalized_selection = m_selection.normalized();
    return position >= normalized_selection.start() && position <= normalized_selection.end();
}

VT::Position TerminalWidget::buffer_position_at(Gfx::IntPoint const& position) const
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
    auto& line = m_terminal.line(row);
    if (column >= (int)line.length())
        column = line.length() - 1;
    row += m_scrollbar->value();
    return { row, column };
}

u32 TerminalWidget::code_point_at(const VT::Position& position) const
{
    VERIFY(position.is_valid());
    VERIFY(position.row() >= 0 && static_cast<size_t>(position.row()) < m_terminal.line_count());
    auto& line = m_terminal.line(position.row());
    if (static_cast<size_t>(position.column()) == line.length())
        return '\n';
    return line.code_point(position.column());
}

VT::Position TerminalWidget::next_position_after(const VT::Position& position, bool should_wrap) const
{
    VERIFY(position.is_valid());
    VERIFY(position.row() >= 0 && static_cast<size_t>(position.row()) < m_terminal.line_count());
    auto& line = m_terminal.line(position.row());
    if (static_cast<size_t>(position.column()) == line.length()) {
        if (static_cast<size_t>(position.row()) == m_terminal.line_count() - 1) {
            if (should_wrap)
                return { 0, 0 };
            return {};
        }
        return { position.row() + 1, 0 };
    }
    return { position.row(), position.column() + 1 };
}

VT::Position TerminalWidget::previous_position_before(const VT::Position& position, bool should_wrap) const
{
    VERIFY(position.row() >= 0 && static_cast<size_t>(position.row()) < m_terminal.line_count());
    if (position.column() == 0) {
        if (position.row() == 0) {
            if (should_wrap) {
                auto& last_line = m_terminal.line(m_terminal.line_count() - 1);
                return { static_cast<int>(m_terminal.line_count() - 1), static_cast<int>(last_line.length()) };
            }
            return {};
        }
        auto& prev_line = m_terminal.line(position.row() - 1);
        return { position.row() - 1, static_cast<int>(prev_line.length()) };
    }
    return { position.row(), position.column() - 1 };
}

static u32 to_lowercase_code_point(u32 code_point)
{
    // FIXME: this only handles ascii characters, but handling unicode lowercasing seems like a mess
    if (code_point < 128)
        return tolower(code_point);
    return code_point;
}

VT::Range TerminalWidget::find_next(StringView needle, const VT::Position& start, bool case_sensitivity, bool should_wrap)
{
    if (needle.is_empty())
        return {};

    VT::Position position = start.is_valid() ? start : VT::Position(0, 0);
    VT::Position original_position = position;

    VT::Position start_of_potential_match;
    size_t needle_index = 0;

    do {
        auto ch = code_point_at(position);
        // FIXME: This is not the right way to use a Unicode needle!
        auto needle_ch = (u32)needle[needle_index];
        if (case_sensitivity ? ch == needle_ch : to_lowercase_code_point(ch) == to_lowercase_code_point(needle_ch)) {
            if (needle_index == 0)
                start_of_potential_match = position;
            ++needle_index;
            if (needle_index >= needle.length())
                return { start_of_potential_match, position };
        } else {
            if (needle_index > 0)
                position = start_of_potential_match;
            needle_index = 0;
        }
        position = next_position_after(position, should_wrap);
    } while (position.is_valid() && position != original_position);

    return {};
}

VT::Range TerminalWidget::find_previous(StringView needle, const VT::Position& start, bool case_sensitivity, bool should_wrap)
{
    if (needle.is_empty())
        return {};

    VT::Position position = start.is_valid() ? start : VT::Position(m_terminal.line_count() - 1, m_terminal.line(m_terminal.line_count() - 1).length() - 1);
    VT::Position original_position = position;

    VT::Position end_of_potential_match;
    size_t needle_index = needle.length() - 1;

    do {
        auto ch = code_point_at(position);
        // FIXME: This is not the right way to use a Unicode needle!
        auto needle_ch = (u32)needle[needle_index];
        if (case_sensitivity ? ch == needle_ch : to_lowercase_code_point(ch) == to_lowercase_code_point(needle_ch)) {
            if (needle_index == needle.length() - 1)
                end_of_potential_match = position;
            if (needle_index == 0)
                return { position, end_of_potential_match };
            --needle_index;
        } else {
            if (needle_index < needle.length() - 1)
                position = end_of_potential_match;
            needle_index = needle.length() - 1;
        }
        position = previous_position_before(position, should_wrap);
    } while (position.is_valid() && position != original_position);

    return {};
}

void TerminalWidget::doubleclick_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        auto attribute = m_terminal.attribute_at(buffer_position_at(event.position()));
        if (!attribute.href_id.is_null()) {
            dbgln("Open hyperlinked URL: '{}'", attribute.href);
            Desktop::Launcher::open(attribute.href);
            return;
        }

        m_triple_click_timer.start();

        auto position = buffer_position_at(event.position());
        auto& line = m_terminal.line(position.row());
        bool want_whitespace = line.code_point(position.column()) == ' ';

        int start_column = 0;
        int end_column = 0;

        for (int column = position.column(); column >= 0 && (line.code_point(column) == ' ') == want_whitespace; --column) {
            start_column = column;
        }

        for (int column = position.column(); column < (int)line.length() && (line.code_point(column) == ' ') == want_whitespace; ++column) {
            end_column = column;
        }

        m_selection.set({ position.row(), start_column }, { position.row(), end_column });
        update_copy_action();
        update();
    }
    GUI::Frame::doubleclick_event(event);
}

void TerminalWidget::paste()
{
    if (m_ptm_fd == -1)
        return;

    auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
    if (!mime_type.starts_with("text/"))
        return;
    if (data.is_empty())
        return;
    send_non_user_input(data);
}

void TerminalWidget::copy()
{
    if (has_selection())
        GUI::Clipboard::the().set_plain_text(selected_text());
}

void TerminalWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (!m_active_href_id.is_null()) {
            m_active_href = {};
            m_active_href_id = {};
            update();
        }

        if (m_triple_click_timer.is_valid())
            m_triple_click_timer.reset();

        set_auto_scroll_direction(AutoScrollDirection::None);
    }
}

void TerminalWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_left_mousedown_position = event.position();
        m_left_mousedown_position_buffer = buffer_position_at(m_left_mousedown_position);

        auto attribute = m_terminal.attribute_at(m_left_mousedown_position_buffer);
        if (!(event.modifiers() & Mod_Shift) && !attribute.href.is_empty()) {
            m_active_href = attribute.href;
            m_active_href_id = attribute.href_id;
            update();
            return;
        }
        m_active_href = {};
        m_active_href_id = {};

        if (m_triple_click_timer.is_valid() && m_triple_click_timer.elapsed() < 250) {
            int start_column = 0;
            int end_column = m_terminal.columns() - 1;

            m_selection.set({ m_left_mousedown_position_buffer.row(), start_column }, { m_left_mousedown_position_buffer.row(), end_column });
        } else {
            m_selection.set(m_left_mousedown_position_buffer, {});
        }
        if (m_alt_key_held)
            m_rectangle_selection = true;
        else if (m_rectangle_selection)
            m_rectangle_selection = false;

        update_copy_action();
        update();
    }
}

void TerminalWidget::mousemove_event(GUI::MouseEvent& event)
{
    auto position = buffer_position_at(event.position());

    auto attribute = m_terminal.attribute_at(position);

    if (attribute.href_id != m_hovered_href_id) {
        if (!attribute.href_id.is_null()) {
            m_hovered_href_id = attribute.href_id;
            m_hovered_href = attribute.href;

            auto handlers = Desktop::Launcher::get_handlers_for_url(attribute.href);
            if (!handlers.is_empty()) {
                auto path = URL(attribute.href).path();
                auto name = LexicalPath::basename(path);
                if (path == handlers[0])
                    set_tooltip(String::formatted("Execute {}", name));
                else
                    set_tooltip(String::formatted("Open {} with {}", name, LexicalPath::basename(handlers[0])));
            }
        } else {
            m_hovered_href_id = {};
            m_hovered_href = {};
            set_tooltip({});
        }
        show_or_hide_tooltip();
        if (!m_hovered_href.is_empty())
            set_override_cursor(Gfx::StandardCursor::Arrow);
        else
            set_override_cursor(Gfx::StandardCursor::IBeam);
        update();
    }

    if (!(event.buttons() & GUI::MouseButton::Primary))
        return;

    if (!m_active_href_id.is_null()) {
        auto diff = event.position() - m_left_mousedown_position;
        auto distance_travelled_squared = diff.x() * diff.x() + diff.y() * diff.y();
        constexpr int drag_distance_threshold = 5;

        if (distance_travelled_squared <= drag_distance_threshold)
            return;

        auto drag_operation = GUI::DragOperation::construct();
        drag_operation->set_text(m_active_href);
        drag_operation->set_data("text/uri-list", m_active_href);

        m_active_href = {};
        m_active_href_id = {};
        m_hovered_href = {};
        m_hovered_href_id = {};
        drag_operation->exec();
        update();
        return;
    }

    auto adjusted_position = event.position().translated(-(frame_thickness() + m_inset), -(frame_thickness() + m_inset));
    if (adjusted_position.y() < 0)
        set_auto_scroll_direction(AutoScrollDirection::Up);
    else if (adjusted_position.y() > m_terminal.rows() * m_line_height)
        set_auto_scroll_direction(AutoScrollDirection::Down);
    else
        set_auto_scroll_direction(AutoScrollDirection::None);

    VT::Position old_selection_end = m_selection.end();
    VT::Position old_selection_start = m_selection.start();

    if (m_triple_click_timer.is_valid()) {
        int start_column = 0;
        int end_column = m_terminal.columns() - 1;

        if (position.row() < m_left_mousedown_position_buffer.row())
            m_selection.set({ position.row(), start_column }, { m_left_mousedown_position_buffer.row(), end_column });
        else
            m_selection.set({ m_left_mousedown_position_buffer.row(), start_column }, { position.row(), end_column });
    } else {
        m_selection.set_end(position);
    }

    if (old_selection_end != m_selection.end() || old_selection_start != m_selection.start()) {
        update_copy_action();
        update();
    }
}

void TerminalWidget::leave_event(Core::Event&)
{
    bool should_update = !m_hovered_href.is_empty();
    m_hovered_href = {};
    m_hovered_href_id = {};
    set_tooltip(m_hovered_href);
    set_override_cursor(Gfx::StandardCursor::IBeam);
    if (should_update)
        update();
}

void TerminalWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (!is_scrollable())
        return;
    set_auto_scroll_direction(AutoScrollDirection::None);
    m_scrollbar->increase_slider_by(event.wheel_delta_y() * scroll_length());
    GUI::Frame::mousewheel_event(event);
}

bool TerminalWidget::is_scrollable() const
{
    return m_scrollbar->is_scrollable();
}

int TerminalWidget::scroll_length() const
{
    return m_scrollbar->step();
}

String TerminalWidget::selected_text() const
{
    StringBuilder builder;

    auto normalized_selection = m_selection.normalized();
    auto start = normalized_selection.start();
    auto end = normalized_selection.end();

    for (int row = start.row(); row <= end.row(); ++row) {
        int first_column = first_selection_column_on_row(row);
        int last_column = last_selection_column_on_row(row);
        for (int column = first_column; column <= last_column; ++column) {
            auto& line = m_terminal.line(row);
            if (line.attribute_at(column).is_untouched()) {
                builder.append('\n');
                break;
            }
            // FIXME: This is a bit hackish.
            u32 code_point = line.code_point(column);
            builder.append(Utf32View(&code_point, 1));
            if (column == static_cast<int>(line.length()) - 1 || (m_rectangle_selection && column == last_column)) {
                builder.append('\n');
            }
        }
    }

    return builder.to_string();
}

int TerminalWidget::first_selection_column_on_row(int row) const
{
    auto normalized_selection_start = m_selection.normalized().start();
    return row == normalized_selection_start.row() || m_rectangle_selection ? normalized_selection_start.column() : 0;
}

int TerminalWidget::last_selection_column_on_row(int row) const
{
    auto normalized_selection_end = m_selection.normalized().end();
    return row == normalized_selection_end.row() || m_rectangle_selection ? normalized_selection_end.column() : m_terminal.columns() - 1;
}

void TerminalWidget::terminal_history_changed(int delta)
{
    bool was_max = m_scrollbar->value() == m_scrollbar->max();
    m_scrollbar->set_max(m_terminal.history_size());
    if (was_max)
        m_scrollbar->set_value(m_scrollbar->max());
    m_scrollbar->update();
    // If the history buffer wrapped around, the selection needs to be offset accordingly.
    if (m_selection.is_valid() && delta < 0)
        m_selection.offset_row(delta);
}

void TerminalWidget::terminal_did_resize(u16 columns, u16 rows)
{
    auto pixel_size = widget_size_for_font(font());
    m_pixel_width = pixel_size.width();
    m_pixel_height = pixel_size.height();

    if (!m_in_relayout) {
        if (on_terminal_size_change)
            on_terminal_size_change(Gfx::IntSize { m_pixel_width, m_pixel_height });
    }

    if (m_automatic_size_policy) {
        set_fixed_size(m_pixel_width, m_pixel_height);
    }

    update();

    winsize ws;
    ws.ws_row = rows;
    ws.ws_col = columns;
    if (m_ptm_fd != -1) {
        if (ioctl(m_ptm_fd, TIOCSWINSZ, &ws) < 0) {
            // This can happen if we resize just as the shell exits.
            dbgln("Notifying the pseudo-terminal about a size change failed.");
        }
    }
}

void TerminalWidget::beep()
{
    if (m_bell_mode == BellMode::Disabled) {
        return;
    }
    if (m_bell_mode == BellMode::AudibleBeep) {
        sysbeep();
        return;
    }
    m_visual_beep_timer->restart(200);
    m_visual_beep_timer->set_single_shot(true);
    m_visual_beep_timer->on_timeout = [this] {
        update();
    };
    update();
}

void TerminalWidget::emit(u8 const* data, size_t size)
{
    if (write(m_ptm_fd, data, size) < 0) {
        perror("TerminalWidget::emit: write");
    }
}

void TerminalWidget::set_cursor_style(CursorStyle style)
{
    switch (style) {
    case None:
        m_cursor_blink_timer->stop();
        m_cursor_blink_state = false;
        break;
    case SteadyBlock:
    case SteadyUnderline:
    case SteadyBar:
        m_cursor_blink_timer->stop();
        m_cursor_blink_state = true;
        break;
    case BlinkingBlock:
    case BlinkingUnderline:
    case BlinkingBar:
        m_cursor_blink_state = true;
        m_cursor_blink_timer->restart();
        break;
    default:
        dbgln("Cursor style not implemented");
    }
    m_cursor_style = style;
    invalidate_cursor();
}

void TerminalWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (m_hovered_href_id.is_null()) {
        m_context_menu->popup(event.screen_position());
    } else {
        m_context_menu_href = m_hovered_href;

        // Ask LaunchServer for a list of programs that can handle the right-clicked URL.
        auto handlers = Desktop::Launcher::get_handlers_for_url(m_hovered_href);
        if (handlers.is_empty()) {
            m_context_menu->popup(event.screen_position());
            return;
        }

        m_context_menu_for_hyperlink = GUI::Menu::construct();
        RefPtr<GUI::Action> context_menu_default_action;

        // Go through the list of handlers and see if we can find a nice display name + icon for them.
        // Then add them to the context menu.
        // FIXME: Adapt this code when we actually support calling LaunchServer with a specific handler in mind.
        for (auto& handler : handlers) {
            auto af = Desktop::AppFile::get_for_app(LexicalPath::basename(handler));
            if (!af->is_valid())
                continue;
            auto action = GUI::Action::create(String::formatted("&Open in {}", af->name()), af->icon().bitmap_for_size(16), [this, handler](auto&) {
                Desktop::Launcher::open(m_context_menu_href, handler);
            });

            if (context_menu_default_action.is_null()) {
                context_menu_default_action = action;
            }

            m_context_menu_for_hyperlink->add_action(action);
        }
        m_context_menu_for_hyperlink->add_action(GUI::Action::create("Copy &URL", [this](auto&) {
            GUI::Clipboard::the().set_plain_text(m_context_menu_href);
        }));
        m_context_menu_for_hyperlink->add_action(GUI::Action::create("Copy &Name", [&](auto&) {
            // file://courage/home/anon/something -> /home/anon/something
            auto path = URL(m_context_menu_href).path();
            // /home/anon/something -> something
            auto name = LexicalPath::basename(path);
            GUI::Clipboard::the().set_plain_text(name);
        }));
        m_context_menu_for_hyperlink->add_separator();
        m_context_menu_for_hyperlink->add_action(copy_action());
        m_context_menu_for_hyperlink->add_action(paste_action());

        m_context_menu_for_hyperlink->popup(event.screen_position(), context_menu_default_action);
    }
}

void TerminalWidget::drop_event(GUI::DropEvent& event)
{
    if (event.mime_data().has_urls()) {
        event.accept();
        auto urls = event.mime_data().urls();
        bool first = true;
        for (auto& url : event.mime_data().urls()) {
            if (!first)
                send_non_user_input(" "sv.bytes());

            if (url.protocol() == "file")
                send_non_user_input(url.path().bytes());
            else
                send_non_user_input(url.to_string().bytes());

            first = false;
        }
    } else if (event.mime_data().has_text()) {
        event.accept();
        auto text = event.mime_data().text();
        send_non_user_input(text.bytes());
    }
}

void TerminalWidget::did_change_font()
{
    GUI::Frame::did_change_font();
    m_line_height = font().glyph_height() + m_line_spacing;
    if (!size().is_empty())
        relayout(size());
}

void TerminalWidget::clear_including_history()
{
    m_terminal.clear_including_history();
}

void TerminalWidget::scroll_to_bottom()
{
    m_scrollbar->set_value(m_scrollbar->max());
}

void TerminalWidget::scroll_to_row(int row)
{
    m_scrollbar->set_value(row);
}

void TerminalWidget::update_copy_action()
{
    m_copy_action->set_enabled(has_selection());
}

void TerminalWidget::update_paste_action()
{
    auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
    m_paste_action->set_enabled(mime_type.starts_with("text/") && !data.is_empty());
}

void TerminalWidget::set_color_scheme(StringView name)
{
    if (name.contains('/')) {
        dbgln("Shenanigans! Color scheme names can't contain slashes.");
        return;
    }

    m_color_scheme_name = name;

    constexpr StringView color_names[] = {
        "Black",
        "Red",
        "Green",
        "Yellow",
        "Blue",
        "Magenta",
        "Cyan",
        "White"
    };

    auto path = String::formatted("/res/terminal-colors/{}.ini", name);
    auto color_config_or_error = Core::ConfigFile::open(path);
    if (color_config_or_error.is_error()) {
        dbgln("Unable to read color scheme file '{}': {}", path, color_config_or_error.error());
        return;
    }
    auto color_config = color_config_or_error.release_value();

    m_show_bold_text_as_bright = color_config->read_bool_entry("Options", "ShowBoldTextAsBright", true);

    auto default_background = Gfx::Color::from_string(color_config->read_entry("Primary", "Background"));
    if (default_background.has_value())
        m_default_background_color = default_background.value();
    else
        m_default_background_color = Gfx::Color::from_rgb(m_colors[(u8)VT::Color::ANSIColor::Black]);

    auto default_foreground = Gfx::Color::from_string(color_config->read_entry("Primary", "Foreground"));
    if (default_foreground.has_value())
        m_default_foreground_color = default_foreground.value();
    else
        m_default_foreground_color = Gfx::Color::from_rgb(m_colors[(u8)VT::Color::ANSIColor::White]);

    for (u8 color_idx = 0; color_idx < 8; ++color_idx) {
        auto rgb = Gfx::Color::from_string(color_config->read_entry("Normal", color_names[color_idx]));
        if (rgb.has_value())
            m_colors[color_idx] = rgb.value().value();
    }

    for (u8 color_idx = 0; color_idx < 8; ++color_idx) {
        auto rgb = Gfx::Color::from_string(color_config->read_entry("Bright", color_names[color_idx]));
        if (rgb.has_value())
            m_colors[color_idx + 8] = rgb.value().value();
    }
    update();
}

Gfx::IntSize TerminalWidget::widget_size_for_font(Gfx::Font const& font) const
{
    return {
        (frame_thickness() * 2) + (m_inset * 2) + (m_terminal.columns() * font.glyph_width('x')) + m_scrollbar->width(),
        (frame_thickness() * 2) + (m_inset * 2) + (m_terminal.rows() * (font.glyph_height() + m_line_spacing))
    };
}

constexpr Gfx::Color TerminalWidget::terminal_color_to_rgb(VT::Color color) const
{
    switch (color.kind()) {
    case VT::Color::Kind::RGB:
        return Gfx::Color::from_rgb(color.as_rgb());
    case VT::Color::Kind::Indexed:
        return Gfx::Color::from_rgb(m_colors[color.as_indexed()]);
    case VT::Color::Kind::Named: {
        auto ansi = color.as_named();
        if ((u16)ansi < 256)
            return Gfx::Color::from_rgb(m_colors[(u16)ansi]);
        else if (ansi == VT::Color::ANSIColor::DefaultForeground)
            return m_default_foreground_color;
        else if (ansi == VT::Color::ANSIColor::DefaultBackground)
            return m_default_background_color;
        else
            VERIFY_NOT_REACHED();
    }
    default:
        VERIFY_NOT_REACHED();
    }
};

void TerminalWidget::set_font_and_resize_to_fit(Gfx::Font const& font)
{
    set_font(font);
    resize(widget_size_for_font(font));
}

// Used for sending data that was not directly typed by the user.
// This basically wraps the code that handles sending the escape sequence in bracketed paste mode.
void TerminalWidget::send_non_user_input(ReadonlyBytes bytes)
{
    constexpr StringView leading_control_sequence = "\e[200~";
    constexpr StringView trailing_control_sequence = "\e[201~";

    int nwritten;
    if (m_terminal.needs_bracketed_paste()) {
        // We do not call write() separately for the control sequences and the data,
        // because that would present a race condition where another process could inject data
        // to prematurely terminate the escape. Could probably be solved by file locking.
        Vector<u8> output;
        output.ensure_capacity(leading_control_sequence.bytes().size() + bytes.size() + trailing_control_sequence.bytes().size());

        // HACK: We don't have a `Vector<T>::unchecked_append(Span<T> const&)` yet :^(
        output.append(leading_control_sequence.bytes().data(), leading_control_sequence.bytes().size());
        output.append(bytes.data(), bytes.size());
        output.append(trailing_control_sequence.bytes().data(), trailing_control_sequence.bytes().size());
        nwritten = write(m_ptm_fd, output.data(), output.size());
    } else {
        nwritten = write(m_ptm_fd, bytes.data(), bytes.size());
    }
    if (nwritten < 0) {
        perror("write");
        VERIFY_NOT_REACHED();
    }
}

void TerminalWidget::set_auto_scroll_direction(AutoScrollDirection direction)
{
    m_auto_scroll_direction = direction;
    m_auto_scroll_timer->set_active(direction != AutoScrollDirection::None);
}

}
