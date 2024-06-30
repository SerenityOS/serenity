/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/AbstractTableView.h>
#include <LibGUI/Action.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

HeaderView::HeaderView(AbstractTableView& table_view, Gfx::Orientation orientation)
    : m_table_view(table_view)
    , m_orientation(orientation)
{
    set_font(Gfx::FontDatabase::default_font().bold_variant());

    if (m_orientation == Gfx::Orientation::Horizontal) {
        set_fixed_height(16);
    } else {
        set_fixed_width(40);
    }
}

HeaderView::~HeaderView() = default;

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
    VERIFY(model());
    if (static_cast<size_t>(section) >= m_section_data.size()) {
        m_section_data.resize(section_count());
    }
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
            offset += m_table_view.horizontal_padding() * 2;
    }
    if (orientation() == Gfx::Orientation::Horizontal)
        return { offset, 0, section_size(section) + m_table_view.horizontal_padding() * 2, height() };
    return { 0, offset, width(), section_size(section) };
}

HeaderView::VisibleSectionRange HeaderView::visible_section_range() const
{
    auto section_count = this->section_count();
    auto is_horizontal = m_orientation == Orientation::Horizontal;
    auto rect = m_table_view.visible_content_rect();
    auto start = is_horizontal ? rect.left() : rect.top();
    auto end = is_horizontal ? (rect.left() + m_table_view.content_width()) : rect.bottom() - 1;
    auto offset = 0;
    VisibleSectionRange range;
    for (; range.end < section_count; ++range.end) {
        auto& section = section_data(range.end);
        int section_size = section.size;
        if (orientation() == Gfx::Orientation::Horizontal)
            section_size += m_table_view.horizontal_padding() * 2;
        if (offset + section_size < start) {
            if (section.visibility)
                offset += section_size;
            ++range.start;
            range.start_offset = offset;
            continue;
        }
        if (offset >= end)
            break;
        if (section.visibility)
            offset += section_size;
    }
    return range;
}

Gfx::IntRect HeaderView::section_resize_grabbable_rect(int section) const
{
    if (!model())
        return {};
    // FIXME: Support resizable rows.
    if (m_orientation == Gfx::Orientation::Vertical)
        return {};
    auto rect = section_rect(section);
    return { rect.right() - 2, rect.top(), 4, rect.height() };
}

int HeaderView::section_count() const
{
    if (!model())
        return 0;
    return m_orientation == Gfx::Orientation::Horizontal ? model()->column_count() : model()->row_count();
}

void HeaderView::doubleclick_event(MouseEvent& event)
{
    if (!model())
        return;

    auto range = visible_section_range();
    for (int i = range.start; i < range.end; ++i) {
        if (section_resize_grabbable_rect(i).contains(event.position())) {
            if (on_resize_doubleclick)
                on_resize_doubleclick(i);
        }
    }
}

void HeaderView::mousedown_event(MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    if (!model())
        return;

    auto& model = *this->model();
    auto range = visible_section_range();

    for (int i = range.start; i < range.end; ++i) {
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

        auto minimum_size = orientation() == Orientation::Horizontal
            ? m_table_view.minimum_column_width(m_resizing_section)
            : m_table_view.minimum_row_height(m_resizing_section);

        if (new_size <= minimum_size)
            new_size = minimum_size;
        VERIFY(m_resizing_section >= 0 && m_resizing_section < model()->column_count());
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
        bool found_hovered_header = false;
        auto range = visible_section_range();
        for (int i = range.start; i < range.end; ++i) {
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
    if (event.button() == MouseButton::Primary) {
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
    painter.draw_line({ 0, 0 }, { rect().right() - 1, 0 }, palette().threed_highlight());
    painter.draw_line({ 0, rect().bottom() - 1 }, { rect().right() - 1, rect().bottom() - 1 }, palette().threed_shadow1());
    auto range = visible_section_range();
    int x_offset = range.start_offset;
    for (int section = range.start; section < range.end; ++section) {
        auto& section_data = this->section_data(section);
        if (!section_data.visibility)
            continue;
        int section_width = section_data.size;
        bool is_key_column = m_table_view.key_column() == section;
        Gfx::IntRect cell_rect(x_offset, 0, section_width + m_table_view.horizontal_padding() * 2, height());
        bool pressed = section == m_pressed_section && m_pressed_section_is_pressed;
        bool hovered = section == m_hovered_section && model()->is_column_sortable(section);
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, pressed, hovered);

        auto text = model()->column_name(section).release_value_but_fixme_should_propagate_errors();
        auto text_rect = cell_rect.shrunken(m_table_view.horizontal_padding() * 2, 0);
        if (pressed)
            text_rect.translate_by(1, 1);
        painter.draw_text(text_rect, text, font(), section_data.alignment, palette().button_text());

        if (is_key_column && (m_table_view.sort_order() != SortOrder::None)) {
            Gfx::IntPoint offset { text_rect.x() + font().width_rounded_up(text) + sorting_arrow_offset, sorting_arrow_offset };
            auto coordinates = m_table_view.sort_order() == SortOrder::Ascending
                ? ascending_arrow_coordinates.span()
                : descending_arrow_coordinates.span();

            painter.draw_triangle(offset, coordinates, palette().button_text());
        }

        x_offset += section_width + m_table_view.horizontal_padding() * 2;
    }

    if (x_offset < rect().right() - 1) {
        Gfx::IntRect cell_rect(x_offset, 0, width() - x_offset, height());
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, false, false);
    }
}

void HeaderView::paint_vertical(Painter& painter)
{
    painter.draw_line(rect().top_left(), rect().bottom_left().moved_up(1), palette().threed_highlight());
    painter.draw_line(rect().top_right().moved_left(1), rect().bottom_right().translated(-1), palette().threed_shadow1());
    auto range = visible_section_range();
    int y_offset = range.start_offset;
    for (int section = range.start; section < range.end; ++section) {
        auto& section_data = this->section_data(section);
        if (!section_data.visibility)
            continue;
        int section_size = section_data.size;
        Gfx::IntRect cell_rect(0, y_offset, width(), section_size);
        bool pressed = section == m_pressed_section && m_pressed_section_is_pressed;
        bool hovered = false;
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, pressed, hovered);
        ByteString text = ByteString::number(section);
        auto text_rect = cell_rect.shrunken(m_table_view.horizontal_padding() * 2, 0);
        if (pressed)
            text_rect.translate_by(1, 1);
        painter.draw_text(text_rect, text, font(), section_data.alignment, palette().button_text());
        y_offset += section_size;
    }

    if (y_offset < rect().bottom() - 1) {
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

void HeaderView::set_section_selectable(int section, bool selectable)
{
    auto& data = section_data(section);
    if (data.selectable == selectable)
        return;
    data.selectable = selectable;
    if (m_context_menu)
        m_context_menu = nullptr;
}

Menu& HeaderView::ensure_context_menu()
{
    // FIXME: This menu needs to be rebuilt if the model is swapped out,
    //        or if the column count/names change.
    if (!m_context_menu) {
        VERIFY(model());
        m_context_menu = Menu::construct();

        if (m_orientation == Gfx::Orientation::Vertical) {
            dbgln("FIXME: Support context menus for vertical GUI::HeaderView");
            return *m_context_menu;
        }

        int section_count = this->section_count();
        for (int section = 0; section < section_count; ++section) {
            auto& column_data = this->section_data(section);
            if (!column_data.selectable)
                continue;
            auto name = model()->column_name(section).release_value_but_fixme_should_propagate_errors().to_byte_string();
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

void HeaderView::set_default_section_size(int section, int size)
{
    auto minimum_column_width = m_table_view.minimum_column_width(section);

    if (orientation() == Gfx::Orientation::Horizontal && size < minimum_column_width)
        size = minimum_column_width;

    auto& data = section_data(section);
    if (data.default_size == size)
        return;
    data.default_size = size;
    data.has_initialized_default_size = true;
}

int HeaderView::default_section_size(int section) const
{
    return section_data(section).default_size;
}

bool HeaderView::is_default_section_size_initialized(int section) const
{
    return section_data(section).has_initialized_default_size;
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

Model const* HeaderView::model() const
{
    return m_table_view.model();
}

}
