/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditor.h"
#include "SearchResultsModel.h"
#include <AK/Debug.h>
#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

HexEditor::HexEditor()
    : m_document(make<HexDocumentMemory>(ByteBuffer::create_zeroed(0).release_value_but_fixme_should_propagate_errors()))
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    set_scrollbars_enabled(true);
    set_font(Gfx::FontDatabase::default_fixed_width_font());
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    vertical_scrollbar().set_step(line_height());

    m_blink_timer = Core::Timer::create_repeating(500, [this]() {
        m_cursor_blink_active = !m_cursor_blink_active;
        update();
    }).release_value_but_fixme_should_propagate_errors();
    m_blink_timer->start();
}

ErrorOr<void> HexEditor::open_new_file(size_t size)
{
    m_document = make<HexDocumentMemory>(TRY(ByteBuffer::create_zeroed(size)));
    set_content_length(m_document->size());
    m_position = 0;
    m_cursor_at_low_nibble = false;
    m_selection_start = 0;
    m_selection_end = 0;
    scroll_position_into_view(m_position);
    update();
    update_status();

    return {};
}

void HexEditor::open_file(NonnullOwnPtr<Core::File> file)
{
    m_document = HexDocumentFile::create(move(file)).release_value_but_fixme_should_propagate_errors();
    set_content_length(m_document->size());
    m_position = 0;
    m_cursor_at_low_nibble = false;
    m_selection_start = 0;
    m_selection_end = 0;
    scroll_position_into_view(m_position);
    update();
    update_status();
}

ErrorOr<void> HexEditor::fill_selection(u8 fill_byte)
{
    if (!has_selection())
        return {};

    ByteBuffer old_values;
    ByteBuffer new_values;

    size_t length = m_selection_end - m_selection_start;

    new_values.resize(length);
    old_values.resize(length);

    for (size_t i = 0; i < length; i++) {
        size_t position = m_selection_start + i;
        old_values[i] = m_document->get(position).value;
        new_values[i] = fill_byte;
        m_document->set(position, fill_byte);
    }

    auto result = did_complete_action(m_selection_start, move(old_values), move(new_values));
    if (result.is_error()) {
        for (size_t i = 0; i < length; i++) {
            size_t position = m_selection_start + i;
            m_document->set(position, old_values[i]);
        }
        return result;
    }

    update();
    did_change();

    return {};
}

void HexEditor::set_position(size_t position)
{
    if (position > m_document->size())
        return;

    m_position = position;
    m_cursor_at_low_nibble = false;
    reset_cursor_blink_state();
    scroll_position_into_view(position);
    update_status();
}
void HexEditor::set_selection(size_t position, size_t length)
{
    if (position > m_document->size() || position + length > m_document->size())
        return;

    m_position = position;
    m_cursor_at_low_nibble = false;
    m_selection_start = position;
    m_selection_end = position + length;
    reset_cursor_blink_state();
    scroll_position_into_view(position);
    update_status();
}

ErrorOr<void> HexEditor::save_as(NonnullOwnPtr<Core::File> new_file)
{
    if (m_document->type() == HexDocument::Type::File) {
        auto& file_document = static_cast<HexDocumentFile&>(*m_document);
        TRY(file_document.write_to_file(*new_file));
        TRY(file_document.set_file(move(new_file)));
    } else {
        auto& memory_document = static_cast<HexDocumentMemory&>(*m_document);
        TRY(memory_document.write_to_file(*new_file));
        m_document = HexDocumentFile::create(move(new_file)).release_value_but_fixme_should_propagate_errors();
    }

    update();

    return {};
}

ErrorOr<void> HexEditor::save()
{
    if (m_document->type() != HexDocument::Type::File)
        return Error::from_string_literal("Unable to save from a memory document");

    TRY(static_cast<HexDocumentFile*>(m_document.ptr())->write_to_file());
    return {};
}

size_t HexEditor::selection_size()
{
    if (!has_selection())
        return 0;
    return m_selection_end - m_selection_start;
}

bool HexEditor::copy_selected_hex_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (size_t i = m_selection_start; i < m_selection_end; i++)
        output_string_builder.appendff("{:02X} ", m_document->get(i).value);

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_deprecated_string());
    return true;
}

bool HexEditor::copy_selected_text_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (size_t i = m_selection_start; i < m_selection_end; i++)
        output_string_builder.append(isprint(m_document->get(i).value) ? m_document->get(i).value : '.');

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_deprecated_string());
    return true;
}

bool HexEditor::copy_selected_hex_to_clipboard_as_c_code()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    output_string_builder.appendff("unsigned char raw_data[{}] = {{\n", m_selection_end - m_selection_start);
    output_string_builder.append("    "sv);
    for (size_t i = m_selection_start, j = 1; i < m_selection_end; i++, j++) {
        output_string_builder.appendff("{:#02X}", m_document->get(i).value);
        if (i >= m_selection_end - 1)
            continue;
        if ((j % 12) == 0)
            output_string_builder.append(",\n    "sv);
        else
            output_string_builder.append(", "sv);
    }
    output_string_builder.append("\n};\n"sv);

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_deprecated_string());
    return true;
}

void HexEditor::set_bytes_per_row(size_t bytes_per_row)
{
    m_bytes_per_row = bytes_per_row;
    auto newWidth = offset_margin_width() + (m_bytes_per_row * cell_width()) + 2 * m_padding + (m_bytes_per_row * character_width()) + 4 * m_padding;
    auto newHeight = total_rows() * line_height() + 2 * m_padding;
    set_content_size({ static_cast<int>(newWidth), static_cast<int>(newHeight) });
    update();
}

void HexEditor::set_content_length(size_t length)
{
    if (length == m_content_length)
        return;
    m_content_length = length;
    auto newWidth = offset_margin_width() + (m_bytes_per_row * cell_width()) + 2 * m_padding + (m_bytes_per_row * character_width()) + 4 * m_padding;
    auto newHeight = total_rows() * line_height() + 2 * m_padding;
    set_content_size({ static_cast<int>(newWidth), static_cast<int>(newHeight) });
}

Optional<u8> HexEditor::get_byte(size_t position)
{
    if (position < m_document->size())
        return m_document->get(position).value;

    return {};
}

ByteBuffer HexEditor::get_selected_bytes()
{
    auto num_selected_bytes = m_selection_end - m_selection_start;
    ByteBuffer data;
    data.ensure_capacity(num_selected_bytes);

    for (size_t i = m_selection_start; i < m_selection_end; i++)
        data.append(m_document->get(i).value);

    return data;
}

void HexEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary) {
        return;
    }

    auto absolute_x = horizontal_scrollbar().value() + event.x();
    auto absolute_y = vertical_scrollbar().value() + event.y();

    auto hex_start_x = frame_thickness() + m_address_bar_width;
    auto hex_start_y = frame_thickness() + m_padding;
    auto hex_end_x = static_cast<int>(hex_start_x + bytes_per_row() * cell_width());
    auto hex_end_y = static_cast<int>(hex_start_y + m_padding + total_rows() * line_height());

    auto text_start_x = static_cast<int>(frame_thickness() + m_address_bar_width + 2 * m_padding + bytes_per_row() * cell_width());
    auto text_start_y = frame_thickness() + m_padding;
    auto text_end_x = static_cast<int>(text_start_x + bytes_per_row() * character_width());
    auto text_end_y = static_cast<int>(text_start_y + m_padding + total_rows() * line_height());

    if (absolute_x >= hex_start_x && absolute_x <= hex_end_x && absolute_y >= hex_start_y && absolute_y <= hex_end_y) {
        if (absolute_x < hex_start_x || absolute_y < hex_start_y)
            return;

        auto byte_x = (absolute_x - hex_start_x) / cell_width();
        auto byte_y = (absolute_y - hex_start_y) / line_height();
        auto offset = (byte_y * m_bytes_per_row) + byte_x;

        if (offset >= m_document->size())
            return;

        dbgln_if(HEX_DEBUG, "HexEditor::mousedown_event(hex): offset={}", offset);

        m_edit_mode = EditMode::Hex;
        m_cursor_at_low_nibble = false;
        m_position = offset;
        m_in_drag_select = true;
        m_selection_start = offset;
        m_selection_end = offset;
        update();
        update_status();
    }

    if (absolute_x >= text_start_x && absolute_x <= text_end_x && absolute_y >= text_start_y && absolute_y <= text_end_y) {
        if (absolute_x < hex_start_x || absolute_y < hex_start_y)
            return;

        auto byte_x = (absolute_x - text_start_x) / character_width();
        auto byte_y = (absolute_y - text_start_y) / line_height();
        auto offset = (byte_y * m_bytes_per_row) + byte_x;

        if (offset >= m_document->size())
            return;

        dbgln_if(HEX_DEBUG, "HexEditor::mousedown_event(text): offset={}", offset);

        m_position = offset;
        m_cursor_at_low_nibble = false;
        m_in_drag_select = true;
        m_selection_start = offset;
        m_selection_end = offset;
        m_edit_mode = EditMode::Text;
        update();
        update_status();
    }
}

void HexEditor::mousemove_event(GUI::MouseEvent& event)
{
    auto absolute_x = horizontal_scrollbar().value() + event.x();
    auto absolute_y = vertical_scrollbar().value() + event.y();

    auto hex_start_x = frame_thickness() + m_address_bar_width;
    auto hex_start_y = frame_thickness() + m_padding;
    auto hex_end_x = static_cast<int>(hex_start_x + bytes_per_row() * cell_width());
    auto hex_end_y = static_cast<int>(hex_start_y + m_padding + total_rows() * line_height());

    auto text_start_x = static_cast<int>(frame_thickness() + m_address_bar_width + 2 * m_padding + bytes_per_row() * cell_width());
    auto text_start_y = frame_thickness() + m_padding;
    auto text_end_x = static_cast<int>(text_start_x + bytes_per_row() * character_width());
    auto text_end_y = static_cast<int>(text_start_y + m_padding + total_rows() * line_height());

    if ((absolute_x >= hex_start_x && absolute_x <= hex_end_x
            && absolute_y >= hex_start_y && absolute_y <= hex_end_y)
        || (absolute_x >= text_start_x && absolute_x <= text_end_x
            && absolute_y >= text_start_y && absolute_y <= text_end_y)) {
        set_override_cursor(Gfx::StandardCursor::IBeam);
    } else {
        set_override_cursor(Gfx::StandardCursor::None);
    }

    if (m_in_drag_select) {
        if (absolute_x >= hex_start_x && absolute_x <= hex_end_x && absolute_y >= hex_start_y && absolute_y <= hex_end_y) {
            if (absolute_x < hex_start_x || absolute_y < hex_start_y)
                return;

            auto byte_x = (absolute_x - hex_start_x) / cell_width();
            auto byte_y = (absolute_y - hex_start_y) / line_height();
            auto offset = (byte_y * m_bytes_per_row) + byte_x;

            if (offset > m_document->size())
                return;

            m_selection_end = offset;
            m_position = offset;
            scroll_position_into_view(offset);
        }

        if (absolute_x >= text_start_x && absolute_x <= text_end_x && absolute_y >= text_start_y && absolute_y <= text_end_y) {
            if (absolute_x < hex_start_x || absolute_y < hex_start_y)
                return;

            auto byte_x = (absolute_x - text_start_x) / character_width();
            auto byte_y = (absolute_y - text_start_y) / line_height();
            auto offset = (byte_y * m_bytes_per_row) + byte_x;
            if (offset > m_document->size())
                return;

            m_selection_end = offset;
            m_position = offset;
            scroll_position_into_view(offset);
        }
        update_status();
        update();
        return;
    }
}

void HexEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (m_in_drag_select) {
            if (m_selection_end < m_selection_start) {
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

void HexEditor::scroll_position_into_view(size_t position)
{
    size_t y = position / bytes_per_row();
    size_t x = position % bytes_per_row();
    Gfx::IntRect rect {
        static_cast<int>(frame_thickness() + offset_margin_width() + x * cell_width() + 2 * m_padding),
        static_cast<int>(frame_thickness() + m_padding + y * line_height()),
        static_cast<int>(cell_width()),
        static_cast<int>(line_height() - m_line_spacing)
    };
    scroll_into_view(rect, true, true);
}

void HexEditor::keydown_event(GUI::KeyEvent& event)
{
    dbgln_if(HEX_DEBUG, "HexEditor::keydown_event key={}", static_cast<u8>(event.key()));

    auto move_and_update_cursor_by = [&](i64 cursor_location_change) {
        size_t new_position = m_position + cursor_location_change;
        if (event.modifiers() & Mod_Shift) {
            size_t selection_pivot = m_position == m_selection_end ? m_selection_start : m_selection_end;
            m_position = new_position;
            m_selection_start = selection_pivot;
            m_selection_end = m_position;
            if (m_selection_start > m_selection_end)
                swap(m_selection_start, m_selection_end);
        } else
            m_selection_start = m_selection_end = m_position = new_position;
        m_cursor_at_low_nibble = false;
        reset_cursor_blink_state();
        scroll_position_into_view(m_position);
        update();
        update_status();
    };

    if (event.key() == KeyCode::Key_Up) {
        if (m_position >= bytes_per_row())
            move_and_update_cursor_by(-bytes_per_row());
        return;
    }

    if (event.key() == KeyCode::Key_Down) {
        if (m_position + bytes_per_row() < m_document->size())
            move_and_update_cursor_by(bytes_per_row());
        return;
    }

    if (event.key() == KeyCode::Key_Left) {
        if (m_position >= 1)
            move_and_update_cursor_by(-1);
        return;
    }

    if (event.key() == KeyCode::Key_Right) {
        if (m_position + 1 < m_document->size())
            move_and_update_cursor_by(1);
        return;
    }

    if (event.key() == KeyCode::Key_Backspace) {
        if (m_position > 0)
            move_and_update_cursor_by(-1);
        return;
    }

    if (event.key() == KeyCode::Key_PageUp) {
        auto cursor_location_change = min(bytes_per_row() * floor(visible_content_rect().height() / line_height()), m_position);
        if (cursor_location_change > 0)
            move_and_update_cursor_by(-cursor_location_change);
        return;
    }

    if (event.key() == KeyCode::Key_PageDown) {
        auto cursor_location_change = min(bytes_per_row() * floor(visible_content_rect().height() / line_height()), m_document->size() - m_position);
        if (cursor_location_change > 0)
            move_and_update_cursor_by(cursor_location_change);
        return;
    }

    if (!event.ctrl() && !event.alt() && !event.text().is_empty()) {
        ErrorOr<void> result;
        if (m_edit_mode == EditMode::Hex) {
            result = hex_mode_keydown_event(event);
        } else {
            result = text_mode_keydown_event(event);
        }
        if (result.is_error())
            GUI::MessageBox::show_error(window(), DeprecatedString::formatted("{}", result.error()));
    }

    event.ignore();
}

ErrorOr<void> HexEditor::hex_mode_keydown_event(GUI::KeyEvent& event)
{
    if ((event.key() >= KeyCode::Key_0 && event.key() <= KeyCode::Key_9) || (event.key() >= KeyCode::Key_A && event.key() <= KeyCode::Key_F)) {
        if (m_document->size() == 0)
            return {};

        VERIFY(m_position <= m_document->size());

        auto old_value = m_document->get(m_position).value;

        // yes, this is terrible... but it works.
        auto value = (event.key() >= KeyCode::Key_0 && event.key() <= KeyCode::Key_9)
            ? event.key() - KeyCode::Key_0
            : (event.key() - KeyCode::Key_A) + 0xA;

        if (!m_cursor_at_low_nibble) {
            u8 existing_change = m_document->get(m_position).value;
            u8 new_value = value << 4 | (existing_change & 0xF); // shift new value left 4 bits, OR with existing last 4 bits
            m_document->set(m_position, new_value);

            auto result = did_complete_action(m_position, old_value, new_value);
            if (result.is_error()) {
                m_document->set(m_position, old_value);
                return result;
            }

            m_cursor_at_low_nibble = true;
        } else {
            u8 new_value = (m_document->get(m_position).value & 0xF0) | value; // save the first 4 bits, OR the new value in the last 4
            m_document->set(m_position, new_value);

            auto result = did_complete_action(m_position, old_value, new_value);
            if (result.is_error()) {
                m_document->set(m_position, old_value);
                return result;
            }

            if (m_position + 1 < m_document->size())
                m_position++;
            m_cursor_at_low_nibble = false;
        }

        reset_cursor_blink_state();
        update();
        update_status();
        did_change();
    }

    return {};
}

ErrorOr<void> HexEditor::text_mode_keydown_event(GUI::KeyEvent& event)
{
    if (m_document->size() == 0)
        return {};
    VERIFY(m_position < m_document->size());

    if (event.code_point() == 0) // This is a control key
        return {};

    auto old_value = m_document->get(m_position).value;
    auto new_value = event.code_point();
    m_document->set(m_position, new_value);
    TRY(did_complete_action(m_position, old_value, new_value));

    if (m_position + 1 < m_document->size())
        m_position++;
    m_cursor_at_low_nibble = false;

    reset_cursor_blink_state();
    update();
    update_status();
    did_change();

    return {};
}

void HexEditor::update_status()
{
    if (on_status_change)
        on_status_change(m_position, m_edit_mode, m_selection_start, m_selection_end);
}

void HexEditor::did_change()
{
    if (on_change)
        on_change(m_document->is_dirty());
}

void HexEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), palette().color(background_role()));

    if (m_document->size() == 0)
        return;

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    Gfx::IntRect offset_clip_rect {
        0,
        vertical_scrollbar().value(),
        m_address_bar_width - m_padding,
        height() - height_occupied_by_horizontal_scrollbar() //(total_rows() * line_height()) + 5
    };
    painter.fill_rect(offset_clip_rect, palette().ruler());
    painter.draw_line(offset_clip_rect.top_right(), offset_clip_rect.bottom_right().translated(-1), palette().ruler_border());

    auto margin_and_hex_width = static_cast<int>(offset_margin_width() + m_bytes_per_row * cell_width() + 3 * m_padding);
    painter.draw_line({ margin_and_hex_width, 0 },
        { margin_and_hex_width, vertical_scrollbar().value() + (height() - height_occupied_by_horizontal_scrollbar()) },
        palette().ruler_border());

    size_t view_height = (height() - height_occupied_by_horizontal_scrollbar());
    size_t min_row = max(0, vertical_scrollbar().value() / line_height());              // if below 0 then use 0
    size_t max_row = min(total_rows(), min_row + ceil_div(view_height, line_height())); // if above calculated rows, use calculated rows

    // paint offsets
    for (size_t i = min_row; i < max_row; i++) {
        Gfx::IntRect side_offset_rect {
            frame_thickness() + m_padding,
            static_cast<int>(frame_thickness() + m_padding + i * line_height()),
            width() - width_occupied_by_vertical_scrollbar(),
            height() - height_occupied_by_horizontal_scrollbar()
        };

        bool is_current_line = (m_position / bytes_per_row()) == i;
        auto line = DeprecatedString::formatted("{:#08X}", i * bytes_per_row());
        painter.draw_text(
            side_offset_rect,
            line,
            is_current_line ? font().bold_variant() : font(),
            Gfx::TextAlignment::TopLeft,
            is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
    }

    for (size_t i = min_row; i < max_row; i++) {
        for (size_t j = 0; j < bytes_per_row(); j++) {
            auto byte_position = (i * bytes_per_row()) + j;
            if (byte_position >= m_document->size())
                return;

            bool const edited_flag = m_document->get(byte_position).modified;

            bool const selection_inbetween_start_end = byte_position >= m_selection_start && byte_position < m_selection_end;
            bool const selection_inbetween_end_start = byte_position >= m_selection_end && byte_position < m_selection_start;
            bool const highlight_flag = selection_inbetween_start_end || selection_inbetween_end_start;

            Gfx::IntRect hex_display_rect {
                frame_thickness() + offset_margin_width() + j * cell_width() + 2 * m_padding,
                frame_thickness() + m_padding + i * line_height(),
                cell_width(),
                line_height() - m_line_spacing
            };
            Gfx::IntRect background_rect {
                frame_thickness() + offset_margin_width() + j * cell_width() + 1 * m_padding,
                frame_thickness() + m_line_spacing / 2 + i * line_height(),
                cell_width(),
                line_height()
            };

            u8 const cell_value = m_document->get(byte_position).value;
            auto line = DeprecatedString::formatted("{:02X}", cell_value);

            Gfx::Color background_color = palette().color(background_role());
            Gfx::Color text_color = [&]() -> Gfx::Color {
                if (edited_flag)
                    return Color::Red;
                if (cell_value == 0x00)
                    return palette().color(ColorRole::PlaceholderText);
                return palette().color(foreground_role());
            }();

            if (highlight_flag) {
                background_color = edited_flag ? palette().selection().inverted() : palette().selection();
                text_color = edited_flag ? palette().selection_text().inverted() : palette().selection_text();
            } else if (byte_position == m_position && m_edit_mode == EditMode::Text) {
                background_color = palette().inactive_selection();
                text_color = palette().inactive_selection_text();
            }
            painter.fill_rect(background_rect, background_color);

            painter.draw_text(hex_display_rect, line, Gfx::TextAlignment::TopLeft, text_color);

            if (m_edit_mode == EditMode::Hex) {
                if (byte_position == m_position && m_cursor_blink_active) {
                    Gfx::IntRect cursor_position_rect {
                        static_cast<int>(hex_display_rect.left() + m_cursor_at_low_nibble * character_width()),
                        hex_display_rect.top(),
                        2,
                        hex_display_rect.height()
                    };
                    painter.fill_rect(cursor_position_rect, palette().text_cursor());
                }
            }

            Gfx::IntRect text_display_rect {
                frame_thickness() + offset_margin_width() + bytes_per_row() * cell_width() + j * character_width() + 4 * m_padding,
                frame_thickness() + m_padding + i * line_height(),
                character_width(),
                line_height() - m_line_spacing
            };
            Gfx::IntRect text_background_rect {
                frame_thickness() + offset_margin_width() + bytes_per_row() * cell_width() + j * character_width() + 4 * m_padding,
                frame_thickness() + m_line_spacing / 2 + i * line_height(),
                character_width(),
                line_height()
            };

            background_color = palette().color(background_role());
            text_color = [&]() -> Gfx::Color {
                if (edited_flag)
                    return Color::Red;
                if (cell_value == 0x00)
                    return palette().color(ColorRole::PlaceholderText);
                return palette().color(foreground_role());
            }();

            if (highlight_flag) {
                background_color = edited_flag ? palette().selection().inverted() : palette().selection();
                text_color = edited_flag ? palette().selection_text().inverted() : palette().selection_text();
            } else if (byte_position == m_position && m_edit_mode == EditMode::Hex) {
                background_color = palette().inactive_selection();
                text_color = palette().inactive_selection_text();
            }

            painter.fill_rect(text_background_rect, background_color);
            painter.draw_text(text_display_rect, DeprecatedString::formatted("{:c}", isprint(cell_value) ? cell_value : '.'), Gfx::TextAlignment::TopLeft, text_color);

            if (m_edit_mode == EditMode::Text) {
                if (byte_position == m_position && m_cursor_blink_active) {
                    Gfx::IntRect cursor_position_rect {
                        text_display_rect.left(), text_display_rect.top(), 2, text_display_rect.height()
                    };
                    painter.fill_rect(cursor_position_rect, palette().text_cursor());
                }
            }
        }
    }
}

void HexEditor::select_all()
{
    highlight(0, m_document->size());
    set_position(0);
}

void HexEditor::highlight(size_t start, size_t end)
{
    m_selection_start = start;
    m_selection_end = end;
    set_position(start);
}

Optional<size_t> HexEditor::find_and_highlight(ByteBuffer& needle, size_t start)
{
    auto end_of_match = find(needle, start);
    if (end_of_match.has_value()) {
        highlight(end_of_match.value() - needle.size(), end_of_match.value());
    }
    return end_of_match;
}

Optional<size_t> HexEditor::find(ByteBuffer& needle, size_t start)
{
    if (m_document->size() == 0)
        return {};

    for (size_t i = start; i < m_document->size(); i++) {
        if (m_document->get(i).value == *needle.data()) {
            bool found = true;
            for (size_t j = 1; j < needle.size(); j++) {
                if (m_document->get(i + j).value != needle.data()[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                auto end_of_match = i + needle.size();
                return end_of_match;
            }
        }
    }

    return {};
}

Vector<Match> HexEditor::find_all(ByteBuffer& needle, size_t start)
{
    if (m_document->size() == 0 || needle.size() == 0)
        return {};

    Vector<Match> matches;

    size_t i = start;
    for (; i < m_document->size(); i++) {
        if (m_document->get(i).value == *needle.data()) {
            bool found = true;
            for (size_t j = 1; j < needle.size(); j++) {
                if (m_document->get(i + j).value != needle.data()[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                matches.append({ i, String::formatted("{}", StringView { needle }).release_value_but_fixme_should_propagate_errors() });
                i += needle.size() - 1;
            }
        }
    }

    if (matches.is_empty())
        return {};

    auto first_match = matches.at(0);
    highlight(first_match.offset, first_match.offset + first_match.value.bytes().size());

    return matches;
}

Vector<Match> HexEditor::find_all_strings(size_t min_length)
{
    if (m_document->size() == 0)
        return {};

    Vector<Match> matches;

    bool found_string = false;
    size_t offset = 0;
    StringBuilder builder;
    for (size_t i = 0; i < m_document->size(); i++) {
        char c = m_document->get(i).value;
        if (isprint(c)) {
            if (!found_string) {
                offset = i;
                found_string = true;
            }
            builder.append(c);
        } else {
            if (builder.length() >= min_length)
                matches.append({ offset, builder.to_string().release_value_but_fixme_should_propagate_errors() });
            builder.clear();
            found_string = false;
        }
    }

    if (matches.is_empty())
        return {};

    auto first_match = matches.at(0);
    highlight(first_match.offset, first_match.offset + first_match.value.bytes().size());

    return matches;
}

void HexEditor::reset_cursor_blink_state()
{
    m_cursor_blink_active = true;
    m_blink_timer->restart();
}

ErrorOr<void> HexEditor::did_complete_action(size_t position, u8 old_value, u8 new_value)
{
    if (old_value == new_value)
        return {};

    auto command = make<HexDocumentUndoCommand>(m_document->make_weak_ptr(), position);

    // We know this won't allocate because the buffers start empty
    MUST(command->try_add_changed_byte(old_value, new_value));

    TRY(m_undo_stack.try_push(move(command)));
    return {};
}

ErrorOr<void> HexEditor::did_complete_action(size_t position, ByteBuffer&& old_values, ByteBuffer&& new_values)
{
    auto command = make<HexDocumentUndoCommand>(m_document->make_weak_ptr(), position);

    TRY(command->try_add_changed_bytes(move(old_values), move(new_values)));
    TRY(m_undo_stack.try_push(move(command)));
    return {};
}

bool HexEditor::undo()
{
    if (!m_undo_stack.can_undo())
        return false;

    m_undo_stack.undo();
    reset_cursor_blink_state();
    update();
    update_status();
    did_change();
    return true;
}

bool HexEditor::redo()
{
    if (!m_undo_stack.can_redo())
        return false;

    m_undo_stack.redo();
    reset_cursor_blink_state();
    update();
    update_status();
    did_change();
    return true;
}

GUI::UndoStack& HexEditor::undo_stack()
{
    return m_undo_stack;
}
