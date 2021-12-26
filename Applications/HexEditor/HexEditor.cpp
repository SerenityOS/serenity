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

#include "HexEditor.h"
#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGfx/Palette.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

HexEditor::HexEditor()
{
    set_scrollbars_enabled(true);
    set_font(GFontDatabase::the().get_by_name("Csilla Thin"));
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    vertical_scrollbar().set_step(line_height());
}

HexEditor::~HexEditor()
{
}

void HexEditor::set_readonly(bool readonly)
{
    if (m_readonly == readonly)
        return;
    m_readonly = readonly;
}

void HexEditor::set_buffer(const ByteBuffer& buffer)
{
    m_buffer = buffer;
    set_content_length(buffer.size());
    m_tracked_changes.clear();
    m_position = 0;
    m_byte_position = 0;
    update();
    update_status();
}

void HexEditor::fill_selection(u8 fill_byte)
{
    if (!has_selection())
        return;

    for (int i = m_selection_start; i <= m_selection_end; i++) {
        m_tracked_changes.set(i, m_buffer.data()[i]);
        m_buffer.data()[i] = fill_byte;
    }

    update();
    did_change();
}

void HexEditor::set_position(int position)
{
    if (position > static_cast<int>(m_buffer.size()))
        return;

    m_position = position;
    m_byte_position = 0;
    scroll_position_into_view(position);
    update_status();
}

bool HexEditor::write_to_file(const StringView& path)
{
    if (m_buffer.is_empty())
        return true;

    int fd = open_with_path_length(path.characters_without_null_termination(), path.length(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd < 0) {
        perror("open");
        return false;
    }

    int rc = ftruncate(fd, m_buffer.size());
    if (rc < 0) {
        perror("ftruncate");
        return false;
    }

    ssize_t nwritten = write(fd, m_buffer.data(), m_buffer.size());
    if (nwritten < 0) {
        perror("write");
        close(fd);
        return false;
    }

    if (static_cast<size_t>(nwritten) == m_buffer.size()) {
        m_tracked_changes.clear();
        update();
    }

    close(fd);
    return true;
}

bool HexEditor::copy_selected_hex_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (int i = m_selection_start; i <= m_selection_end; i++) {
        output_string_builder.appendf("%02X ", m_buffer.data()[i]);
    }

    GUI::Clipboard::the().set_data(output_string_builder.to_string());
    return true;
}

bool HexEditor::copy_selected_text_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (int i = m_selection_start; i <= m_selection_end; i++) {
        output_string_builder.appendf("%c", isprint(m_buffer.data()[i]) ? m_buffer[i] : '.');
    }

    GUI::Clipboard::the().set_data(output_string_builder.to_string());
    return true;
}

bool HexEditor::copy_selected_hex_to_clipboard_as_c_code()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    output_string_builder.appendf("unsigned char raw_data[%d] = {\n", (m_selection_end - m_selection_start) + 1);
    output_string_builder.append("    ");
    for (int i = m_selection_start, j = 1; i <= m_selection_end; i++, j++) {
        output_string_builder.appendf("0x%02X", m_buffer.data()[i]);
        if (i != m_selection_end)
            output_string_builder.append(", ");
        if ((j % 12) == 0) {
            output_string_builder.append("\n");
            output_string_builder.append("    ");
        }
    }
    output_string_builder.append("\n};\n");

    GUI::Clipboard::the().set_data(output_string_builder.to_string());
    return true;
}

void HexEditor::set_bytes_per_row(int bytes_per_row)
{
    m_bytes_per_row = bytes_per_row;
    set_content_size({ offset_margin_width() + (m_bytes_per_row * (character_width() * 3)) + 10 + (m_bytes_per_row * character_width()) + 20, total_rows() * line_height() + 10 });
    update();
}

void HexEditor::set_content_length(int length)
{
    if (length == m_content_length)
        return;
    m_content_length = length;
    set_content_size({ offset_margin_width() + (m_bytes_per_row * (character_width() * 3)) + 10 + (m_bytes_per_row * character_width()) + 20, total_rows() * line_height() + 10 });
}

void HexEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left) {
        return;
    }

    auto absolute_x = horizontal_scrollbar().value() + event.x();
    auto absolute_y = vertical_scrollbar().value() + event.y();

    auto hex_start_x = frame_thickness() + 90;
    auto hex_start_y = frame_thickness() + 5;
    auto hex_end_x = hex_start_x + (bytes_per_row() * (character_width() * 3));
    auto hex_end_y = hex_start_y + 5 + (total_rows() * line_height());

    auto text_start_x = frame_thickness() + 100 + (bytes_per_row() * (character_width() * 3));
    auto text_start_y = frame_thickness() + 5;
    auto text_end_x = text_start_x + (bytes_per_row() * character_width());
    auto text_end_y = text_start_y + 5 + (total_rows() * line_height());

    if (absolute_x >= hex_start_x && absolute_x <= hex_end_x && absolute_y >= hex_start_y && absolute_y <= hex_end_y) {
        auto byte_x = (absolute_x - hex_start_x) / (character_width() * 3);
        auto byte_y = (absolute_y - hex_start_y) / line_height();
        auto offset = (byte_y * m_bytes_per_row) + byte_x;

        if (offset < 0 || offset > static_cast<int>(m_buffer.size()))
            return;

#ifdef HEX_DEBUG
        printf("HexEditor::mousedown_event(hex): offset=%d\n", offset);
#endif

        m_edit_mode = EditMode::Hex;
        m_byte_position = 0;
        m_position = offset;
        m_in_drag_select = true;
        m_selection_start = offset;
        m_selection_end = -1;
        update();
        update_status();
    }

    if (absolute_x >= text_start_x && absolute_x <= text_end_x && absolute_y >= text_start_y && absolute_y <= text_end_y) {
        auto byte_x = (absolute_x - text_start_x) / character_width();
        auto byte_y = (absolute_y - text_start_y) / line_height();
        auto offset = (byte_y * m_bytes_per_row) + byte_x;

        if (offset < 0 || offset > static_cast<int>(m_buffer.size()))
            return;

#ifdef HEX_DEBUG
        printf("HexEditor::mousedown_event(text): offset=%d\n", offset);
#endif

        m_position = offset;
        m_byte_position = 0;
        m_in_drag_select = true;
        m_selection_start = offset;
        m_selection_end = -1;
        m_edit_mode = EditMode::Text;
        update();
        update_status();
    }
}

void HexEditor::mousemove_event(GUI::MouseEvent& event)
{
    auto absolute_x = horizontal_scrollbar().value() + event.x();
    auto absolute_y = vertical_scrollbar().value() + event.y();

    auto hex_start_x = frame_thickness() + 90;
    auto hex_start_y = frame_thickness() + 5;
    auto hex_end_x = hex_start_x + (bytes_per_row() * (character_width() * 3));
    auto hex_end_y = hex_start_y + 5 + (total_rows() * line_height());

    auto text_start_x = frame_thickness() + 100 + (bytes_per_row() * (character_width() * 3));
    auto text_start_y = frame_thickness() + 5;
    auto text_end_x = text_start_x + (bytes_per_row() * character_width());
    auto text_end_y = text_start_y + 5 + (total_rows() * line_height());

    window()->set_override_cursor(GUI::StandardCursor::None);
    if ((absolute_x >= hex_start_x && absolute_x <= hex_end_x
            && absolute_y >= hex_start_y && absolute_y <= hex_end_y)
        || (absolute_x >= text_start_x && absolute_x <= text_end_x
            && absolute_y >= text_start_y && absolute_y <= text_end_y)) {
        window()->set_override_cursor(GUI::StandardCursor::IBeam);
    }

    if (m_in_drag_select) {
        if (absolute_x >= hex_start_x && absolute_x <= hex_end_x && absolute_y >= hex_start_y && absolute_y <= hex_end_y) {
            auto byte_x = (absolute_x - hex_start_x) / (character_width() * 3);
            auto byte_y = (absolute_y - hex_start_y) / line_height();
            auto offset = (byte_y * m_bytes_per_row) + byte_x;

            if (offset < 0 || offset > static_cast<int>(m_buffer.size()))
                return;

            m_selection_end = offset;
            scroll_position_into_view(offset);
        }

        if (absolute_x >= text_start_x && absolute_x <= text_end_x && absolute_y >= text_start_y && absolute_y <= text_end_y) {
            auto byte_x = (absolute_x - text_start_x) / character_width();
            auto byte_y = (absolute_y - text_start_y) / line_height();
            auto offset = (byte_y * m_bytes_per_row) + byte_x;
            if (offset < 0 || offset > static_cast<int>(m_buffer.size()))
                return;

            m_selection_end = offset;
            scroll_position_into_view(offset);
        }
        update_status();
        update();
        return;
    }
}

void HexEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Left) {
        if (m_in_drag_select) {
            if (m_selection_end == -1 || m_selection_start == -1) {
                m_selection_start = -1;
                m_selection_end = -1;
            } else if (m_selection_end < m_selection_start) {
                // lets flip these around
                auto start = m_selection_end;
                m_selection_end = m_selection_start;
                m_selection_start = start;
            }
            m_in_drag_select = false;
        }
        update();
        update_status();
    }
}

void HexEditor::scroll_position_into_view(int position)
{
    int y = position / bytes_per_row();
    int x = position % bytes_per_row();
    Gfx::Rect rect {
        frame_thickness() + offset_margin_width() + (x * (character_width() * 3)) + 10,
        frame_thickness() + 5 + (y * line_height()),
        (character_width() * 3),
        line_height() - m_line_spacing
    };
    scroll_into_view(rect, true, true);
}

void HexEditor::keydown_event(GUI::KeyEvent& event)
{
#ifdef HEX_DEBUG
    printf("HexEditor::keydown_event key=%d\n", event.key());
#endif

    if (event.key() == KeyCode::Key_Up) {
        if (m_position - bytes_per_row() >= 0) {
            m_position -= bytes_per_row();
            m_byte_position = 0;
            scroll_position_into_view(m_position);
            update();
            update_status();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Down) {
        if (m_position + bytes_per_row() < static_cast<int>(m_buffer.size())) {
            m_position += bytes_per_row();
            m_byte_position = 0;
            scroll_position_into_view(m_position);
            update();
            update_status();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Left) {
        if (m_position - 1 >= 0) {
            m_position--;
            m_byte_position = 0;
            scroll_position_into_view(m_position);
            update();
            update_status();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Right) {
        if (m_position + 1 < static_cast<int>(m_buffer.size())) {
            m_position++;
            m_byte_position = 0;
            scroll_position_into_view(m_position);
            update();
            update_status();
        }
        return;
    }

    if (event.key() == KeyCode::Key_Backspace) {
        if (m_position > 0) {
            m_position--;
            m_byte_position = 0;
            scroll_position_into_view(m_position);
            update();
            update_status();
        }
        return;
    }

    if (!is_readonly() && !event.ctrl() && !event.alt() && !event.text().is_empty()) {
        if (m_edit_mode == EditMode::Hex) {
            hex_mode_keydown_event(event);
        } else {
            text_mode_keydown_event(event);
        }
    }
}

void HexEditor::hex_mode_keydown_event(GUI::KeyEvent& event)
{
    if ((event.key() >= KeyCode::Key_0 && event.key() <= KeyCode::Key_9) || (event.key() >= KeyCode::Key_A && event.key() <= KeyCode::Key_F)) {
        if (m_buffer.is_empty())
            return;
        ASSERT(m_position >= 0);
        ASSERT(m_position < static_cast<int>(m_buffer.size()));

        // yes, this is terrible... but it works.
        auto value = (event.key() >= KeyCode::Key_0 && event.key() <= KeyCode::Key_9)
            ? event.key() - KeyCode::Key_0
            : (event.key() - KeyCode::Key_A) + 0xA;

        if (m_byte_position == 0) {
            m_tracked_changes.set(m_position, m_buffer.data()[m_position]);
            m_buffer.data()[m_position] = value << 4 | (m_buffer.data()[m_position] & 0xF); // shift new value left 4 bits, OR with existing last 4 bits
            m_byte_position++;
        } else {
            m_buffer.data()[m_position] = (m_buffer.data()[m_position] & 0xF0) | value; // save the first 4 bits, OR the new value in the last 4
            if (m_position + 1 < static_cast<int>(m_buffer.size()))
                m_position++;
            m_byte_position = 0;
        }

        update();
        update_status();
        did_change();
    }
}

void HexEditor::text_mode_keydown_event(GUI::KeyEvent& event)
{
    if (m_buffer.is_empty())
        return;
    ASSERT(m_position >= 0);
    ASSERT(m_position < static_cast<int>(m_buffer.size()));

    m_tracked_changes.set(m_position, m_buffer.data()[m_position]);
    m_buffer.data()[m_position] = (u8)event.text().characters()[0]; // save the first 4 bits, OR the new value in the last 4
    if (m_position + 1 < static_cast<int>(m_buffer.size()))
        m_position++;
    m_byte_position = 0;

    update();
    update_status();
    did_change();
}

void HexEditor::update_status()
{
    if (on_status_change)
        on_status_change(m_position, m_edit_mode, m_selection_start, m_selection_end);
}

void HexEditor::did_change()
{
    if (on_change)
        on_change();
}

void HexEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), palette().color(background_role()));

    if (m_buffer.is_empty())
        return;

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    Gfx::Rect offset_clip_rect {
        0,
        vertical_scrollbar().value(),
        85,
        height() - height_occupied_by_horizontal_scrollbar() //(total_rows() * line_height()) + 5
    };
    painter.fill_rect(offset_clip_rect, palette().ruler());
    painter.draw_line(offset_clip_rect.top_right(), offset_clip_rect.bottom_right(), palette().ruler_border());

    auto margin_and_hex_width = offset_margin_width() + (m_bytes_per_row * (character_width() * 3)) + 15;
    painter.draw_line({ margin_and_hex_width, 0 },
        { margin_and_hex_width, vertical_scrollbar().value() + (height() - height_occupied_by_horizontal_scrollbar()) },
        palette().ruler_border());

    auto view_height = (height() - height_occupied_by_horizontal_scrollbar());
    auto min_row = max(0, vertical_scrollbar().value() / line_height());              // if below 0 then use 0
    auto max_row = min(total_rows(), min_row + ceil_div(view_height, line_height())); // if above calculated rows, use calculated rows

    // paint offsets
    for (int i = min_row; i < max_row; i++) {
        Gfx::Rect side_offset_rect {
            frame_thickness() + 5,
            frame_thickness() + 5 + (i * line_height()),
            width() - width_occupied_by_vertical_scrollbar(),
            height() - height_occupied_by_horizontal_scrollbar()
        };

        bool is_current_line = (m_position / bytes_per_row()) == i;
        auto line = String::format("0x%08X", i * bytes_per_row());
        painter.draw_text(
            side_offset_rect,
            line,
            is_current_line ? Gfx::Font::default_bold_font() : font(),
            Gfx::TextAlignment::TopLeft,
            is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
    }

    for (int i = min_row; i < max_row; i++) {
        for (int j = 0; j < bytes_per_row(); j++) {
            auto byte_position = (i * bytes_per_row()) + j;
            if (byte_position >= static_cast<int>(m_buffer.size()))
                return;

            Color text_color = palette().color(foreground_role());
            if (m_tracked_changes.contains(byte_position)) {
                text_color = Color::Red;
            }

            auto highlight_flag = false;
            if (m_selection_start > -1 && m_selection_end > -1) {
                if (byte_position >= m_selection_start && byte_position <= m_selection_end) {
                    highlight_flag = true;
                }
                if (byte_position >= m_selection_end && byte_position <= m_selection_start) {
                    highlight_flag = true;
                }
            }

            Gfx::Rect hex_display_rect {
                frame_thickness() + offset_margin_width() + (j * (character_width() * 3)) + 10,
                frame_thickness() + 5 + (i * line_height()),
                (character_width() * 3),
                line_height() - m_line_spacing
            };
            if (highlight_flag) {
                painter.fill_rect(hex_display_rect, palette().selection());
                text_color = text_color == Color::Red ? Color::from_rgb(0xFFC0CB) : palette().selection_text();
            } else if (byte_position == m_position) {
                painter.fill_rect(hex_display_rect, palette().inactive_selection());
                text_color = palette().inactive_selection_text();
            }

            auto line = String::format("%02X", m_buffer[byte_position]);
            painter.draw_text(hex_display_rect, line, Gfx::TextAlignment::TopLeft, text_color);

            Gfx::Rect text_display_rect {
                frame_thickness() + offset_margin_width() + (bytes_per_row() * (character_width() * 3)) + (j * character_width()) + 20,
                frame_thickness() + 5 + (i * line_height()),
                character_width(),
                line_height() - m_line_spacing
            };
            // selection highlighting.
            if (highlight_flag) {
                painter.fill_rect(text_display_rect, palette().selection());
            } else if (byte_position == m_position) {
                painter.fill_rect(text_display_rect, palette().inactive_selection());
            }

            painter.draw_text(text_display_rect, String::format("%c", isprint(m_buffer[byte_position]) ? m_buffer[byte_position] : '.'), Gfx::TextAlignment::TopLeft, text_color);
        }
    }
}

void HexEditor::leave_event(Core::Event&)
{
    ASSERT(window());
    window()->set_override_cursor(GUI::StandardCursor::None);
}
