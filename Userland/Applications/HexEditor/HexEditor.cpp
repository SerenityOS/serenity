/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HexEditor.h"
#include "EditAnnotationDialog.h"
#include "SearchResultsModel.h"
#include <AK/ByteString.h>
#include <AK/Debug.h>
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

namespace HexEditor {

HexEditor::OffsetFormat HexEditor::offset_format_from_string(StringView string)
{
    if (string.equals_ignoring_ascii_case("decimal"sv)) {
        return OffsetFormat::Decimal;
    }
    // Default to hex if invalid
    return OffsetFormat::Hexadecimal;
}

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

    m_context_menu = GUI::Menu::construct();
    m_add_annotation_action = GUI::Action::create(
        "&Add Annotation",
        Gfx::Bitmap::load_from_file("/res/icons/16x16/annotation-add.png"sv).release_value_but_fixme_should_propagate_errors(),
        [this](GUI::Action&) { show_create_annotation_dialog(); },
        this);
    m_context_menu->add_action(*m_add_annotation_action);
    m_edit_annotation_action = GUI::Action::create(
        "&Edit Annotation",
        Gfx::Bitmap::load_from_file("/res/icons/16x16/annotation.png"sv).release_value_but_fixme_should_propagate_errors(),
        [this](GUI::Action&) {
            VERIFY(m_hovered_annotation.has_value());
            show_edit_annotation_dialog(*m_hovered_annotation);
        },
        this);
    m_context_menu->add_action(*m_edit_annotation_action);
    m_delete_annotation_action = GUI::Action::create(
        "&Delete Annotation",
        Gfx::Bitmap::load_from_file("/res/icons/16x16/annotation-remove.png"sv).release_value_but_fixme_should_propagate_errors(),
        [this](GUI::Action&) {
            VERIFY(m_hovered_annotation.has_value());
            show_delete_annotation_dialog(*m_hovered_annotation);
        },
        this);
    m_context_menu->add_action(*m_delete_annotation_action);
}

ErrorOr<void> HexEditor::open_new_file(size_t size)
{
    m_document = make<HexDocumentMemory>(TRY(ByteBuffer::create_zeroed(size)));
    set_content_length(m_document->size());
    m_position = 0;
    m_cursor_at_low_nibble = false;
    m_selection.clear();
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
    m_selection.clear();
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

    size_t length = m_selection.size();

    new_values.resize(length);
    old_values.resize(length);

    for (size_t i = 0; i < length; i++) {
        size_t position = m_selection.start + i;
        old_values[i] = m_document->get(position).value;
        new_values[i] = fill_byte;
        m_document->set(position, fill_byte);
    }

    auto result = did_complete_action(m_selection.start, move(old_values), move(new_values));
    if (result.is_error()) {
        for (size_t i = 0; i < length; i++) {
            size_t position = m_selection.start + i;
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
    scroll_position_into_view(position);
    update_status();
}

void HexEditor::set_selection(size_t position, size_t length)
{
    if (position > m_document->size() || position + length > m_document->size())
        return;

    m_position = position;
    m_cursor_at_low_nibble = false;
    m_selection.start = position;
    m_selection.end = position + length;
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
        m_document = TRY(HexDocumentFile::create(move(new_file)));
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

bool HexEditor::copy_selected_hex_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (size_t i = m_selection.start; i < m_selection.end; i++)
        output_string_builder.appendff("{:02X} ", m_document->get(i).value);

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_byte_string());
    return true;
}

bool HexEditor::copy_selected_text_to_clipboard()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    for (size_t i = m_selection.start; i < m_selection.end; i++)
        output_string_builder.append(isprint(m_document->get(i).value) ? m_document->get(i).value : '.');

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_byte_string());
    return true;
}

bool HexEditor::copy_selected_hex_to_clipboard_as_c_code()
{
    if (!has_selection())
        return false;

    StringBuilder output_string_builder;
    output_string_builder.appendff("unsigned char raw_data[{}] = {{\n", m_selection.end - m_selection.start);
    output_string_builder.append("    "sv);
    for (size_t i = m_selection.start, j = 1; i < m_selection.end; i++, j++) {
        output_string_builder.appendff("{:#02X}", m_document->get(i).value);
        if (i >= m_selection.end - 1)
            continue;
        if ((j % 12) == 0)
            output_string_builder.append(",\n    "sv);
        else
            output_string_builder.append(", "sv);
    }
    output_string_builder.append("\n};\n"sv);

    GUI::Clipboard::the().set_plain_text(output_string_builder.to_byte_string());
    return true;
}

void HexEditor::update_content_size()
{
    auto new_width = offset_area_width() + hex_area_width() + text_area_width();
    auto new_height = total_rows() * line_height() + 2 * m_padding;
    set_content_size({ static_cast<int>(new_width), static_cast<int>(new_height) });
    update();
}

void HexEditor::set_show_offsets_column(bool value)
{
    if (value == m_show_offsets_column)
        return;

    m_show_offsets_column = value;
    update_content_size();
}

void HexEditor::set_offset_format(OffsetFormat format)
{
    if (format == m_offset_format)
        return;

    m_offset_format = format;
    update_content_size();
}

void HexEditor::set_bytes_per_row(size_t bytes_per_row)
{
    if (bytes_per_row == this->bytes_per_row())
        return;
    set_groups_per_row(ceil_div(bytes_per_row, m_bytes_per_group));
}

void HexEditor::set_bytes_per_group(size_t bytes_per_group)
{
    if (bytes_per_group == m_bytes_per_group)
        return;
    m_bytes_per_group = bytes_per_group;
    update_content_size();
}

void HexEditor::set_groups_per_row(size_t groups_per_row)
{
    if (groups_per_row == m_groups_per_row)
        return;
    m_groups_per_row = groups_per_row;
    update_content_size();
}

void HexEditor::set_content_length(size_t length)
{
    if (length == m_content_length)
        return;
    m_content_length = length;
    update_content_size();
}

Optional<u8> HexEditor::get_byte(size_t position)
{
    if (position < m_document->size())
        return m_document->get(position).value;

    return {};
}

ByteBuffer HexEditor::get_selected_bytes()
{
    auto num_selected_bytes = m_selection.size();
    ByteBuffer data;
    data.ensure_capacity(num_selected_bytes);

    for (size_t i = m_selection.start; i < m_selection.end; i++)
        data.append(m_document->get(i).value);

    return data;
}

Optional<HexEditor::OffsetData> HexEditor::offset_at(Gfx::IntPoint position) const
{
    auto absolute_x = horizontal_scrollbar().value() + position.x();
    auto absolute_y = vertical_scrollbar().value() + position.y();

    auto hex_start_x = frame_thickness() + offset_area_width();
    auto hex_start_y = frame_thickness() + m_padding;
    auto hex_end_x = hex_start_x + hex_area_width();
    auto hex_end_y = static_cast<int>(hex_start_y + m_padding + total_rows() * line_height());

    auto text_start_x = hex_start_x + hex_area_width();
    auto text_start_y = frame_thickness() + m_padding;
    auto text_end_x = text_start_x + text_area_width();
    auto text_end_y = static_cast<int>(text_start_y + m_padding + total_rows() * line_height());

    // Hexadecimal display
    if (absolute_x >= hex_start_x && absolute_x <= hex_end_x && absolute_y >= hex_start_y && absolute_y <= hex_end_y) {
        auto hex_text_start_x = hex_start_x + m_padding;
        auto hex_text_end_x = hex_end_x - m_padding;
        absolute_x = clamp(absolute_x, hex_text_start_x, hex_text_end_x);

        auto group_x = (absolute_x - hex_text_start_x) / group_width();
        auto byte_within_group = (absolute_x - hex_text_start_x - group_x * group_width()) / cell_width();
        auto byte_y = (absolute_y - hex_start_y) / line_height();
        auto offset = (byte_y * bytes_per_row()) + (group_x * bytes_per_group()) + byte_within_group;

        if (offset >= m_document->size())
            return {};

        return OffsetData { offset, EditMode::Hex };
    }

    // Text display
    if (absolute_x >= text_start_x && absolute_x <= text_end_x && absolute_y >= text_start_y && absolute_y <= text_end_y) {
        auto text_text_start_x = text_start_x + m_padding;
        auto text_text_end_x = text_end_x - m_padding;
        absolute_x = clamp(absolute_x, text_text_start_x, text_text_end_x);

        auto byte_x = (absolute_x - text_text_start_x) / character_width();
        auto byte_y = (absolute_y - text_start_y) / line_height();
        auto offset = (byte_y * bytes_per_row()) + byte_x;

        if (offset >= m_document->size())
            return {};

        return OffsetData { offset, EditMode::Text };
    }

    return {};
}

void HexEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary) {
        return;
    }

    auto maybe_offset_data = offset_at(event.position());
    if (!maybe_offset_data.has_value())
        return;
    auto offset_data = maybe_offset_data.release_value();

    dbgln_if(HEX_DEBUG, "Editor::mousedown_event({}): offset={}", offset_data.panel == EditMode::Hex ? "hex"sv : "text"sv, offset_data.offset);
    m_edit_mode = offset_data.panel;
    m_cursor_at_low_nibble = false;
    m_position = offset_data.offset;
    m_in_drag_select = true;
    m_selection.start = offset_data.offset;
    m_selection.end = offset_data.offset;
    update();
    update_status();
}

void HexEditor::mousemove_event(GUI::MouseEvent& event)
{
    auto maybe_offset_data = offset_at(event.position());

    if (maybe_offset_data.has_value()) {
        set_override_cursor(Gfx::StandardCursor::IBeam);
        m_hovered_annotation = m_document->annotations().closest_annotation_at(maybe_offset_data->offset);
    } else {
        set_override_cursor(Gfx::StandardCursor::None);
        m_hovered_annotation.clear();
    }

    if (m_in_drag_select) {
        if (maybe_offset_data.has_value()) {
            auto offset_data = maybe_offset_data.release_value();
            m_selection.end = offset_data.offset;
            m_position = (m_selection.end <= m_selection.start) ? offset_data.offset : offset_data.offset - 1;
            scroll_position_into_view(offset_data.offset);
        }

        update();
        update_status();
        set_tooltip(""_string);
    } else {
        set_tooltip(m_hovered_annotation.has_value() ? m_hovered_annotation->comments : ""_string);
    }
    show_or_hide_tooltip();
}

void HexEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (m_in_drag_select) {
            if (m_selection.end < m_selection.start) {
                // lets flip these around
                swap(m_selection.start, m_selection.end);
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
        static_cast<int>(frame_thickness() + offset_area_width() + m_padding + x * cell_width()),
        static_cast<int>(frame_thickness() + m_padding + y * line_height()),
        static_cast<int>(cell_width()),
        static_cast<int>(line_height() - m_line_spacing)
    };
    scroll_into_view(rect, true, true);
}

size_t HexEditor::total_rows() const
{
    return ceil_div(m_content_length, bytes_per_row());
}

size_t HexEditor::line_height() const
{
    return font().pixel_size_rounded_up() + m_line_spacing;
}

size_t HexEditor::character_width() const
{
    return font().glyph_fixed_width();
}

size_t HexEditor::cell_gap() const
{
    return character_width() / 2;
}

size_t HexEditor::cell_width() const
{
    return character_width() * 2 + cell_gap();
}

size_t HexEditor::group_gap() const
{
    return character_width() * 1.5;
}

size_t HexEditor::group_width() const
{
    return (character_width() * 2 * bytes_per_group())
        + (cell_gap() * (bytes_per_group() - 1))
        + group_gap();
}

int HexEditor::offset_area_width() const
{
    if (!m_show_offsets_column)
        return 0;
    // By a fun coincidence, decimal and hexadecimal are both 10 characters for the 32-bit range.
    // (decimal is up to 10 digits; hex is 8 digits with a 2-character prefix)
    return m_padding + font().width_rounded_up("0X12345678"sv) + m_padding;
}

int HexEditor::hex_area_width() const
{
    return m_padding
        + groups_per_row() * group_width() - group_gap()
        + m_padding;
}

int HexEditor::text_area_width() const
{
    return m_padding + bytes_per_row() * character_width() + m_padding;
}

void HexEditor::keydown_event(GUI::KeyEvent& event)
{
    dbgln_if(HEX_DEBUG, "Editor::keydown_event key={}", static_cast<u8>(event.key()));

    auto move_and_update_cursor_by = [&](i64 cursor_location_change) {
        size_t new_position = m_position + cursor_location_change;
        if (event.modifiers() & Mod_Shift) {
            size_t selection_pivot = m_position == m_selection.end ? m_selection.start : m_selection.end;
            m_position = new_position;
            m_selection.start = selection_pivot;
            m_selection.end = m_position;
            if (m_selection.start > m_selection.end)
                swap(m_selection.start, m_selection.end);
        } else {
            m_selection.start = m_selection.end = m_position = new_position;
        }
        m_cursor_at_low_nibble = false;
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
            GUI::MessageBox::show_error(window(), ByteString::formatted("{}", result.error()));
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

    update();
    update_status();
    did_change();

    return {};
}

void HexEditor::context_menu_event(GUI::ContextMenuEvent& event)
{
    m_edit_annotation_action->set_visible(m_hovered_annotation.has_value());
    m_delete_annotation_action->set_visible(m_hovered_annotation.has_value());
    m_context_menu->popup(event.screen_position());
}

void HexEditor::theme_change_event(GUI::ThemeChangeEvent&)
{
    set_font(Gfx::FontDatabase::default_fixed_width_font());
    update_content_size();
}

void HexEditor::update_status()
{
    if (on_status_change)
        on_status_change(m_position, m_edit_mode, m_selection);
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

    int offset_area_start_x = frame_thickness();
    int offset_area_text_start_x = offset_area_start_x + m_padding;
    int hex_area_start_x = offset_area_start_x + offset_area_width();
    int hex_area_text_start_x = hex_area_start_x + m_padding;
    int text_area_start_x = hex_area_start_x + hex_area_width();
    int text_area_text_start_x = text_area_start_x + m_padding;

    if (m_show_offsets_column) {
        Gfx::IntRect offset_clip_rect {
            0,
            vertical_scrollbar().value(),
            offset_area_width(),
            height() - height_occupied_by_horizontal_scrollbar()
        };
        painter.fill_rect(offset_clip_rect, palette().ruler());
        painter.draw_line(offset_clip_rect.top_right(), offset_clip_rect.bottom_right(), palette().ruler_border());
    }

    painter.draw_line({ text_area_start_x, 0 },
        { text_area_start_x, vertical_scrollbar().value() + (height() - height_occupied_by_horizontal_scrollbar()) },
        palette().ruler_border());

    size_t view_height = (height() - height_occupied_by_horizontal_scrollbar());
    size_t min_row = max(0, vertical_scrollbar().value() / line_height());              // if below 0 then use 0
    size_t max_row = min(total_rows(), min_row + ceil_div(view_height, line_height())); // if above calculated rows, use calculated rows

    for (size_t row = min_row; row < max_row; row++) {
        int row_text_y = frame_thickness() + m_padding + row * line_height();
        int row_background_y = row_text_y - m_line_spacing / 2;

        // Paint offsets
        if (m_show_offsets_column) {
            Gfx::IntRect side_offset_rect {
                offset_area_text_start_x,
                row_text_y,
                width() - width_occupied_by_vertical_scrollbar(),
                height() - height_occupied_by_horizontal_scrollbar()
            };

            bool is_current_line = (m_position / bytes_per_row()) == row;
            String offset_text;
            switch (m_offset_format) {
            case OffsetFormat::Decimal:
                offset_text = MUST(String::formatted("{:010d}", row * bytes_per_row()));
                break;
            case OffsetFormat::Hexadecimal:
                offset_text = MUST(String::formatted("{:#08X}", row * bytes_per_row()));
                break;
            }
            painter.draw_text(
                side_offset_rect,
                offset_text,
                is_current_line ? font().bold_variant() : font(),
                Gfx::TextAlignment::TopLeft,
                is_current_line ? palette().ruler_active_text() : palette().ruler_inactive_text());
        }

        // Paint bytes
        for (size_t column = 0; column < bytes_per_row(); column++) {
            auto byte_position = (row * bytes_per_row()) + column;
            if (byte_position >= m_document->size())
                return;

            auto group = column / bytes_per_group();
            auto column_within_group = column % bytes_per_group();

            auto const cell = m_document->get(byte_position);
            auto const annotation = m_document->annotations().closest_annotation_at(byte_position);

            Gfx::IntRect hex_display_rect_high_nibble {
                static_cast<int>(hex_area_text_start_x + (group * group_width()) + (column_within_group * cell_width())),
                row_text_y,
                static_cast<int>(character_width()),
                static_cast<int>(line_height() - m_line_spacing),
            };

            Gfx::IntRect hex_display_rect_low_nibble {
                static_cast<int>(hex_display_rect_high_nibble.x() + character_width()),
                hex_display_rect_high_nibble.y(),
                hex_display_rect_high_nibble.width(),
                hex_display_rect_high_nibble.height()
            };

            Gfx::IntRect background_rect {
                static_cast<int>(hex_display_rect_high_nibble.x() - character_width() / 2),
                row_background_y,
                static_cast<int>(character_width() * 3),
                static_cast<int>(line_height()),
            };

            auto line = String::formatted("{:02X}", cell.value).release_value_but_fixme_should_propagate_errors();
            auto high_nibble = line.substring_from_byte_offset(0, 1).release_value_but_fixme_should_propagate_errors();
            auto low_nibble = line.substring_from_byte_offset(1).release_value_but_fixme_should_propagate_errors();

            bool const selected = m_selection.contains(byte_position);

            // Styling priorities are as follows, with smaller numbers beating larger ones:
            // 1. Modified bytes
            // 2. The cursor position
            // 3. The selection
            // 4. Annotations
            // 5. Null bytes
            // 6. Regular formatting
            auto determine_background_color = [&](EditMode edit_mode) -> Optional<Gfx::Color> {
                if (selected)
                    return cell.modified ? palette().selection().inverted() : palette().selection();
                if (byte_position == m_position && m_edit_mode != edit_mode)
                    return palette().inactive_selection();
                if (annotation.has_value())
                    return annotation->background_color;
                return {};
            };
            auto determine_text_color = [&](EditMode edit_mode) -> Gfx::Color {
                if (cell.modified)
                    return Color::Red;
                if (selected)
                    return palette().selection_text();
                if (byte_position == m_position)
                    return (m_edit_mode == edit_mode) ? palette().color(foreground_role()) : palette().inactive_selection_text();
                if (annotation.has_value())
                    return annotation->background_color.suggested_foreground_color();
                if (cell.value == 0x00)
                    return palette().color(ColorRole::PlaceholderText);
                return palette().color(foreground_role());
            };
            auto background_color_hex = determine_background_color(EditMode::Hex);
            auto background_color_text = determine_background_color(EditMode::Text);
            auto text_color_hex = determine_text_color(EditMode::Hex);
            auto text_color_text = determine_text_color(EditMode::Text);
            auto& font = cell.modified ? this->font().bold_variant() : this->font();

            if (background_color_hex.has_value())
                painter.fill_rect(background_rect, *background_color_hex);

            Gfx::IntRect text_display_rect {
                static_cast<int>(text_area_text_start_x + column * character_width()),
                row_text_y,
                static_cast<int>(character_width()),
                static_cast<int>(line_height() - m_line_spacing),
            };

            auto draw_cursor_rect = [&]() {
                if (byte_position == m_position) {
                    Gfx::IntRect cursor_position_rect {
                        (m_edit_mode == EditMode::Hex) ? static_cast<int>(hex_display_rect_high_nibble.left() + m_cursor_at_low_nibble * character_width()) : text_display_rect.left(),
                        row_background_y,
                        static_cast<int>(character_width()),
                        static_cast<int>(line_height())
                    };
                    painter.fill_rect(cursor_position_rect, palette().black());
                }
            };

            if (m_edit_mode == EditMode::Hex)
                draw_cursor_rect();

            if (byte_position == m_position && !cell.modified) {
                painter.draw_text(hex_display_rect_high_nibble, high_nibble, font, Gfx::TextAlignment::TopLeft, m_cursor_at_low_nibble ? text_color_hex : palette().selection_text());
                painter.draw_text(hex_display_rect_low_nibble, low_nibble, font, Gfx::TextAlignment::TopLeft, m_cursor_at_low_nibble ? palette().selection_text() : text_color_hex);
            } else {
                painter.draw_text(hex_display_rect_high_nibble, high_nibble, font, Gfx::TextAlignment::TopLeft, text_color_hex);
                painter.draw_text(hex_display_rect_low_nibble, low_nibble, font, Gfx::TextAlignment::TopLeft, text_color_hex);
            }

            Gfx::IntRect text_background_rect {
                static_cast<int>(text_area_text_start_x + column * character_width()),
                row_background_y,
                static_cast<int>(character_width()),
                static_cast<int>(line_height()),
            };

            if (background_color_text.has_value())
                painter.fill_rect(text_background_rect, *background_color_text);

            if (m_edit_mode == EditMode::Text)
                draw_cursor_rect();

            char const character = is_ascii_printable(cell.value) ? cell.value : '.';
            auto character_sv = StringView { &character, 1 };
            if (byte_position == m_position)
                painter.draw_text(text_display_rect, character_sv, font, Gfx::TextAlignment::TopLeft, palette().selection_text());
            else
                painter.draw_text(text_display_rect, character_sv, font, Gfx::TextAlignment::TopLeft, text_color_text);
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
    m_selection.start = start;
    m_selection.end = end;
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
    update();
    update_status();
    did_change();
    return true;
}

GUI::UndoStack& HexEditor::undo_stack()
{
    return m_undo_stack;
}

void HexEditor::show_create_annotation_dialog()
{
    auto result = EditAnnotationDialog::show_create_dialog(window(), *m_document, selection());
    if (result == GUI::Dialog::ExecResult::OK)
        update();
}

void HexEditor::show_edit_annotation_dialog(Annotation& annotation)
{
    auto result = EditAnnotationDialog::show_edit_dialog(window(), *m_document, annotation);
    if (result == GUI::Dialog::ExecResult::OK)
        update();
}

void HexEditor::show_delete_annotation_dialog(Annotation& annotation)
{
    StringBuilder builder;
    builder.append("Delete '"sv);
    Utf8View comments_first_line { annotation.comments.bytes_as_string_view().find_first_split_view('\n') };
    auto const max_annotation_text_length = 40;
    if (comments_first_line.length() <= max_annotation_text_length) {
        builder.append(comments_first_line.as_string());
    } else {
        builder.appendff("{}...", comments_first_line.unicode_substring_view(0, max_annotation_text_length));
    }
    builder.append("'?"sv);

    auto result = GUI::MessageBox::show(window(), builder.string_view(), "Delete annotation?"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
    if (result == GUI::Dialog::ExecResult::Yes) {
        m_document->annotations().delete_annotation(annotation);
        update();
    }
}

}
