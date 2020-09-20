/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/AbstractTableView.h>
#include <LibGUI/Action.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

static constexpr int minimum_column_size = 2;

HeaderView::HeaderView(AbstractTableView& table_view, Gfx::Orientation orientation)
    : m_table_view(table_view)
    , m_orientation(orientation)
{
    set_font(Gfx::Font::default_bold_font());

    if (m_orientation == Gfx::Orientation::Horizontal) {
        set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        set_preferred_size(0, 16);
    } else {
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        set_preferred_size(40, 0);
    }
}

HeaderView::~HeaderView()
{
}

void HeaderView::set_section_size(int section, int size)
{
    auto& data = section_data(section);
    if (data.size == size)
        return;
    data.size = size;
    data.has_initialized_size = true;
    data.size = size;
    m_table_view.header_did_change_section_size({}, m_orientation, section, size);
}

int HeaderView::section_size(int section) const
{
    return section_data(section).size;
}

HeaderView::SectionData& HeaderView::section_data(int section) const
{
    if (static_cast<size_t>(section) >= m_section_data.size())
        m_section_data.resize(section + 1);
    return m_section_data.at(section);
}

Gfx::IntRect HeaderView::section_rect(int section) const
{
    if (!model())
        return {};
    auto& data = section_data(section);
    if (!data.visibility)
        return {};
    int offset = 0;
    for (int i = 0; i < section; ++i) {
        if (!is_section_visible(i))
            continue;
        offset += section_data(i).size;
        if (orientation() == Gfx::Orientation::Horizontal)
            offset += horizontal_padding() * 2;
    }
    if (orientation() == Gfx::Orientation::Horizontal)
        return { offset, 0, section_size(section) + horizontal_padding() * 2, height() };
    return { 0, offset, width(), section_size(section) };
}

Gfx::IntRect HeaderView::section_resize_grabbable_rect(int section) const
{
    if (!model())
        return {};
    // FIXME: Support resizable rows.
    if (m_orientation == Gfx::Orientation::Vertical)
        return {};
    auto rect = section_rect(section);
    return { rect.right() - 1, rect.top(), 4, rect.height() };
}

int HeaderView::section_count() const
{
    if (!model())
        return 0;
    return m_orientation == Gfx::Orientation::Horizontal ? model()->column_count() : model()->row_count();
}

void HeaderView::mousedown_event(MouseEvent& event)
{
    if (!model())
        return;

    auto& model = *this->model();
    int section_count = this->section_count();

    for (int i = 0; i < section_count; ++i) {
        if (section_resize_grabbable_rect(i).contains(event.position())) {
            m_resizing_section = i;
            m_in_section_resize = true;
            m_section_resize_original_width = section_size(i);
            m_section_resize_origin = event.position();
            return;
        }
        auto rect = this->section_rect(i);
        if (rect.contains(event.position()) && model.is_column_sortable(i)) {
            m_pressed_section = i;
            m_pressed_section_is_pressed = true;
            update();
            return;
        }
    }
}

void HeaderView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return;

    if (m_in_section_resize) {
        auto delta = event.position() - m_section_resize_origin;
        int new_size = m_section_resize_original_width + delta.primary_offset_for_orientation(m_orientation);
        if (new_size <= minimum_column_size)
            new_size = minimum_column_size;
        ASSERT(m_resizing_section >= 0 && m_resizing_section < model()->column_count());
        set_section_size(m_resizing_section, new_size);
        return;
    }

    if (m_pressed_section != -1) {
        auto header_rect = this->section_rect(m_pressed_section);
        if (header_rect.contains(event.position())) {
            set_hovered_section(m_pressed_section);
            if (!m_pressed_section_is_pressed)
                update();
            m_pressed_section_is_pressed = true;
        } else {
            set_hovered_section(-1);
            if (m_pressed_section_is_pressed)
                update();
            m_pressed_section_is_pressed = false;
        }
        return;
    }

    if (event.buttons() == 0) {
        int section_count = this->section_count();
        bool found_hovered_header = false;
        for (int i = 0; i < section_count; ++i) {
            if (section_resize_grabbable_rect(i).contains(event.position())) {
                set_override_cursor(Gfx::StandardCursor::ResizeColumn);
                set_hovered_section(-1);
                return;
            }
            if (section_rect(i).contains(event.position())) {
                set_hovered_section(i);
                found_hovered_header = true;
            }
        }
        if (!found_hovered_header)
            set_hovered_section(-1);
    }
    set_override_cursor(Gfx::StandardCursor::None);
}

void HeaderView::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left) {
        if (m_in_section_resize) {
            if (!section_resize_grabbable_rect(m_resizing_section).contains(event.position()))
                set_override_cursor(Gfx::StandardCursor::None);
            m_in_section_resize = false;
            return;
        }
        if (m_pressed_section != -1) {
            if (m_orientation == Gfx::Orientation::Horizontal && section_rect(m_pressed_section).contains(event.position())) {
                auto new_sort_order = m_table_view.sort_order();
                if (m_table_view.key_column() == m_pressed_section)
                    new_sort_order = m_table_view.sort_order() == SortOrder::Ascending
                        ? SortOrder::Descending
                        : SortOrder::Ascending;
                m_table_view.set_key_column_and_sort_order(m_pressed_section, new_sort_order);
            }
            m_pressed_section = -1;
            m_pressed_section_is_pressed = false;
            update();
            return;
        }
    }
}

void HeaderView::paint_horizontal(Painter& painter)
{
    painter.draw_line({ 0, 0 }, { rect().right(), 0 }, palette().threed_highlight());
    painter.draw_line({ 0, rect().bottom() }, { rect().right(), rect().bottom() }, palette().threed_shadow1());
    int x_offset = 0;
    int section_count = this->section_count();
    for (int section = 0; section < section_count; ++section) {
        if (!is_section_visible(section))
            continue;
        int section_width = section_size(section);
        bool is_key_column = m_table_view.key_column() == section;
        Gfx::IntRect cell_rect(x_offset, 0, section_width + horizontal_padding() * 2, height());
        bool pressed = section == m_pressed_section && m_pressed_section_is_pressed;
        bool hovered = section == m_hovered_section && model()->is_column_sortable(section);
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, pressed, hovered);
        String text;
        if (is_key_column) {
            StringBuilder builder;
            builder.append(model()->column_name(section));
            if (m_table_view.sort_order() == SortOrder::Ascending)
                builder.append(" \xE2\xAC\x86"); // UPWARDS BLACK ARROW
            else if (m_table_view.sort_order() == SortOrder::Descending)
                builder.append(" \xE2\xAC\x87"); // DOWNWARDS BLACK ARROW
            text = builder.to_string();
        } else {
            text = model()->column_name(section);
        }
        auto text_rect = cell_rect.shrunken(horizontal_padding() * 2, 0);
        if (pressed)
            text_rect.move_by(1, 1);
        painter.draw_text(text_rect, text, font(), section_alignment(section), palette().button_text());
        x_offset += section_width + horizontal_padding() * 2;
    }

    if (x_offset < rect().right()) {
        Gfx::IntRect cell_rect(x_offset, 0, width() - x_offset, height());
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, false, false);
    }
}

void HeaderView::paint_vertical(Painter& painter)
{
    painter.draw_line(rect().top_left(), rect().bottom_left(), palette().threed_highlight());
    painter.draw_line(rect().top_right(), rect().bottom_right(), palette().threed_shadow1());
    int y_offset = 0;
    int section_count = this->section_count();
    for (int section = 0; section < section_count; ++section) {
        if (!is_section_visible(section))
            continue;
        int section_size = this->section_size(section);
        Gfx::IntRect cell_rect(0, y_offset, width(), section_size);
        bool pressed = section == m_pressed_section && m_pressed_section_is_pressed;
        bool hovered = false;
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, pressed, hovered);
        String text = String::format("%d", section);
        auto text_rect = cell_rect.shrunken(horizontal_padding() * 2, 0);
        if (pressed)
            text_rect.move_by(1, 1);
        painter.draw_text(text_rect, text, font(), section_alignment(section), palette().button_text());
        y_offset += section_size;
    }

    if (y_offset < rect().bottom()) {
        Gfx::IntRect cell_rect(0, y_offset, width(), height() - y_offset);
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, false, false);
    }
}

void HeaderView::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().button());
    if (orientation() == Gfx::Orientation::Horizontal)
        paint_horizontal(painter);
    else
        paint_vertical(painter);
}

void HeaderView::set_section_visible(int section, bool visible)
{
    auto& data = section_data(section);
    if (data.visibility == visible)
        return;
    data.visibility = visible;
    if (data.visibility_action) {
        data.visibility_action->set_checked(visible);
    }
    m_table_view.header_did_change_section_visibility({}, m_orientation, section, visible);
    update();
}

Menu& HeaderView::ensure_context_menu()
{
    // FIXME: This menu needs to be rebuilt if the model is swapped out,
    //        or if the column count/names change.
    if (!m_context_menu) {
        ASSERT(model());
        m_context_menu = Menu::construct();

        int section_count = this->section_count();
        for (int section = 0; section < section_count; ++section) {
            auto& column_data = this->section_data(section);
            // FIXME: Vertical support
            ASSERT(m_orientation == Gfx::Orientation::Horizontal);
            auto name = model()->column_name(section);
            column_data.visibility_action = Action::create_checkable(name, [this, section](auto& action) {
                set_section_visible(section, action.is_checked());
            });
            column_data.visibility_action->set_checked(column_data.visibility);

            m_context_menu->add_action(*column_data.visibility_action);
        }
    }
    return *m_context_menu;
}

void HeaderView::context_menu_event(ContextMenuEvent& event)
{
    ensure_context_menu().popup(event.screen_position());
}

void HeaderView::leave_event(Core::Event& event)
{
    Widget::leave_event(event);
    set_hovered_section(-1);
}

Gfx::TextAlignment HeaderView::section_alignment(int section) const
{
    return section_data(section).alignment;
}

void HeaderView::set_section_alignment(int section, Gfx::TextAlignment alignment)
{
    section_data(section).alignment = alignment;
}

bool HeaderView::is_section_visible(int section) const
{
    return section_data(section).visibility;
}

void HeaderView::set_hovered_section(int section)
{
    if (m_hovered_section == section)
        return;
    m_hovered_section = section;
    update();
}

Model* HeaderView::model()
{
    return m_table_view.model();
}

const Model* HeaderView::model() const
{
    return m_table_view.model();
}

}
