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

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/AbstractTableView.h>
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace GUI {

static const int minimum_column_width = 2;

AbstractTableView::AbstractTableView()
{
    set_should_hide_unnecessary_scrollbars(true);
}

AbstractTableView::~AbstractTableView()
{
}

void AbstractTableView::update_column_sizes()
{
    if (!m_size_columns_to_fit_content)
        return;

    if (!model())
        return;

    auto& model = *this->model();
    int column_count = model.column_count();
    int row_count = model.row_count();
    int key_column = model.key_column();

    for (int column = 0; column < column_count; ++column) {
        if (is_column_hidden(column))
            continue;
        int header_width = header_font().width(model.column_name(column));
        if (column == key_column)
            header_width += font().width(" \xc3\xb6");
        int column_width = header_width;
        for (int row = 0; row < row_count; ++row) {
            auto cell_data = model.data(model.index(row, column));
            int cell_width = 0;
            if (cell_data.is_bitmap()) {
                cell_width = cell_data.as_bitmap().width();
            } else {
                cell_width = font().width(cell_data.to_string());
            }
            column_width = max(column_width, cell_width);
        }
        auto& column_data = this->column_data(column);
        column_data.width = max(column_data.width, column_width);
        column_data.has_initialized_width = true;
    }
}

void AbstractTableView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width = 0;
    int column_count = model()->column_count();
    for (int i = 0; i < column_count; ++i) {
        if (!is_column_hidden(i))
            content_width += column_width(i) + horizontal_padding() * 2;
    }
    int content_height = item_count() * item_height();

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ 0, header_height() });
}

Gfx::Rect AbstractTableView::header_rect(int column_index) const
{
    if (!model())
        return {};
    if (is_column_hidden(column_index))
        return {};
    int x_offset = 0;
    for (int i = 0; i < column_index; ++i) {
        if (is_column_hidden(i))
            continue;
        x_offset += column_width(i) + horizontal_padding() * 2;
    }
    return { x_offset, 0, column_width(column_index) + horizontal_padding() * 2, header_height() };
}

void AbstractTableView::set_hovered_header_index(int index)
{
    if (m_hovered_column_header_index == index)
        return;
    m_hovered_column_header_index = index;
    update_headers();
}

void AbstractTableView::paint_headers(Painter& painter)
{
    if (!headers_visible())
        return;
    int exposed_width = max(content_size().width(), width());
    painter.fill_rect({ 0, 0, exposed_width, header_height() }, palette().button());
    painter.draw_line({ 0, 0 }, { exposed_width - 1, 0 }, palette().threed_highlight());
    painter.draw_line({ 0, header_height() - 1 }, { exposed_width - 1, header_height() - 1 }, palette().threed_shadow1());
    int x_offset = 0;
    int column_count = model()->column_count();
    for (int column_index = 0; column_index < column_count; ++column_index) {
        if (is_column_hidden(column_index))
            continue;
        int column_width = this->column_width(column_index);
        bool is_key_column = model()->key_column() == column_index;
        Gfx::Rect cell_rect(x_offset, 0, column_width + horizontal_padding() * 2, header_height());
        bool pressed = column_index == m_pressed_column_header_index && m_pressed_column_header_is_pressed;
        bool hovered = column_index == m_hovered_column_header_index && model()->column_metadata(column_index).sortable == Model::ColumnMetadata::Sortable::True;
        Gfx::StylePainter::paint_button(painter, cell_rect, palette(), Gfx::ButtonStyle::Normal, pressed, hovered);
        String text;
        if (is_key_column) {
            StringBuilder builder;
            builder.append(model()->column_name(column_index));
            auto sort_order = model()->sort_order();
            if (sort_order == SortOrder::Ascending)
                builder.append(" \xc3\xb6");
            else if (sort_order == SortOrder::Descending)
                builder.append(" \xc3\xb7");
            text = builder.to_string();
        } else {
            text = model()->column_name(column_index);
        }
        auto text_rect = cell_rect.translated(horizontal_padding(), 0);
        if (pressed)
            text_rect.move_by(1, 1);
        painter.draw_text(text_rect, text, header_font(), Gfx::TextAlignment::CenterLeft, palette().button_text());
        x_offset += column_width + horizontal_padding() * 2;
    }
}

bool AbstractTableView::is_column_hidden(int column) const
{
    return !column_data(column).visibility;
}

void AbstractTableView::set_column_hidden(int column, bool hidden)
{
    auto& column_data = this->column_data(column);
    if (column_data.visibility == !hidden)
        return;
    column_data.visibility = !hidden;
    update_content_size();
    update();
}

Menu& AbstractTableView::ensure_header_context_menu()
{
    // FIXME: This menu needs to be rebuilt if the model is swapped out,
    //        or if the column count/names change.
    if (!m_header_context_menu) {
        ASSERT(model());
        m_header_context_menu = Menu::construct();

        for (int column = 0; column < model()->column_count(); ++column) {
            auto& column_data = this->column_data(column);
            auto name = model()->column_name(column);
            column_data.visibility_action = Action::create(name, [this, column](Action& action) {
                action.set_checked(!action.is_checked());
                set_column_hidden(column, !action.is_checked());
            });
            column_data.visibility_action->set_checkable(true);
            column_data.visibility_action->set_checked(true);

            m_header_context_menu->add_action(*column_data.visibility_action);
        }
    }
    return *m_header_context_menu;
}

const Gfx::Font& AbstractTableView::header_font()
{
    return Gfx::Font::default_bold_font();
}

void AbstractTableView::set_cell_painting_delegate(int column, OwnPtr<TableCellPaintingDelegate>&& delegate)
{
    column_data(column).cell_painting_delegate = move(delegate);
}

void AbstractTableView::update_headers()
{
    Gfx::Rect rect { 0, 0, frame_inner_rect().width(), header_height() };
    rect.move_by(frame_thickness(), frame_thickness());
    update(rect);
}

AbstractTableView::ColumnData& AbstractTableView::column_data(int column) const
{
    if (column >= m_column_data.size())
        m_column_data.resize(column + 1);
    return m_column_data.at(column);
}

Gfx::Rect AbstractTableView::column_resize_grabbable_rect(int column) const
{
    if (!model())
        return {};
    auto header_rect = this->header_rect(column);
    return { header_rect.right() - 1, header_rect.top(), 4, header_rect.height() };
}

int AbstractTableView::column_width(int column_index) const
{
    if (!model())
        return 0;
    auto& column_data = this->column_data(column_index);
    if (!column_data.has_initialized_width) {
        ASSERT(!m_size_columns_to_fit_content);
        column_data.has_initialized_width = true;
        column_data.width = model()->column_metadata(column_index).preferred_width;
    }
    return column_data.width;
}

void AbstractTableView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousemove_event(event);

    auto adjusted_position = this->adjusted_position(event.position());

    if (m_in_column_resize) {
        auto delta = adjusted_position - m_column_resize_origin;
        int new_width = m_column_resize_original_width + delta.x();
        if (new_width <= minimum_column_width)
            new_width = minimum_column_width;
        ASSERT(m_resizing_column >= 0 && m_resizing_column < model()->column_count());
        auto& column_data = this->column_data(m_resizing_column);
        if (column_data.width != new_width) {
            column_data.width = new_width;
            update_content_size();
            update();
        }
        return;
    }

    if (m_pressed_column_header_index != -1) {
        auto header_rect = this->header_rect(m_pressed_column_header_index);
        if (header_rect.contains(adjusted_position)) {
            set_hovered_header_index(m_pressed_column_header_index);
            if (!m_pressed_column_header_is_pressed)
                update_headers();
            m_pressed_column_header_is_pressed = true;
        } else {
            set_hovered_header_index(-1);
            if (m_pressed_column_header_is_pressed)
                update_headers();
            m_pressed_column_header_is_pressed = false;
        }
        return;
    }

    if (event.buttons() == 0) {
        int column_count = model()->column_count();
        bool found_hovered_header = false;
        for (int i = 0; i < column_count; ++i) {
            if (column_resize_grabbable_rect(i).contains(adjusted_position)) {
                window()->set_override_cursor(StandardCursor::ResizeHorizontal);
                set_hovered_header_index(-1);
                return;
            }
            if (header_rect(i).contains(adjusted_position)) {
                set_hovered_header_index(i);
                found_hovered_header = true;
            }
        }
        if (!found_hovered_header)
            set_hovered_header_index(-1);
    }
    window()->set_override_cursor(StandardCursor::None);

    AbstractView::mousemove_event(event);
}

void AbstractTableView::mouseup_event(MouseEvent& event)
{
    auto adjusted_position = this->adjusted_position(event.position());
    if (event.button() == MouseButton::Left) {
        if (m_in_column_resize) {
            if (!column_resize_grabbable_rect(m_resizing_column).contains(adjusted_position))
                window()->set_override_cursor(StandardCursor::None);
            m_in_column_resize = false;
            return;
        }
        if (m_pressed_column_header_index != -1) {
            auto header_rect = this->header_rect(m_pressed_column_header_index);
            if (header_rect.contains(adjusted_position)) {
                auto new_sort_order = SortOrder::Ascending;
                if (model()->key_column() == m_pressed_column_header_index)
                    new_sort_order = model()->sort_order() == SortOrder::Ascending
                        ? SortOrder::Descending
                        : SortOrder::Ascending;
                model()->set_key_column_and_sort_order(m_pressed_column_header_index, new_sort_order);
            }
            m_pressed_column_header_index = -1;
            m_pressed_column_header_is_pressed = false;
            update_headers();
            return;
        }
    }

    AbstractView::mouseup_event(event);
}

void AbstractTableView::mousedown_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousedown_event(event);

    if (event.button() != MouseButton::Left)
        return AbstractView::mousedown_event(event);

    auto adjusted_position = this->adjusted_position(event.position());

    if (event.y() < header_height()) {
        int column_count = model()->column_count();
        for (int i = 0; i < column_count; ++i) {
            if (column_resize_grabbable_rect(i).contains(adjusted_position)) {
                m_resizing_column = i;
                m_in_column_resize = true;
                m_column_resize_original_width = column_width(i);
                m_column_resize_origin = adjusted_position;
                return;
            }
            auto header_rect = this->header_rect(i);
            auto column_metadata = model()->column_metadata(i);
            if (header_rect.contains(adjusted_position) && column_metadata.sortable == Model::ColumnMetadata::Sortable::True) {
                m_pressed_column_header_index = i;
                m_pressed_column_header_is_pressed = true;
                update_headers();
                return;
            }
        }
        return;
    }

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);

    if (index.is_valid() && is_toggle && model()->row_count(index)) {
        toggle_index(index);
        return;
    }

    AbstractView::mousedown_event(event);
}

ModelIndex AbstractTableView::index_at_event_position(const Gfx::Point& position, bool& is_toggle) const
{
    is_toggle = false;
    if (!model())
        return {};

    auto adjusted_position = this->adjusted_position(position);
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        if (!row_rect(row).contains(adjusted_position))
            continue;
        for (int column = 0, column_count = model()->column_count(); column < column_count; ++column) {
            if (!content_rect(row, column).contains(adjusted_position))
                continue;
            return model()->index(row, column);
        }
        return model()->index(row, 0);
    }
    return {};
}

ModelIndex AbstractTableView::index_at_event_position(const Gfx::Point& position) const
{
    bool is_toggle;
    auto index = index_at_event_position(position, is_toggle);
    return is_toggle ? ModelIndex() : index;
}

int AbstractTableView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void AbstractTableView::keydown_event(KeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        activate_selected();
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = visible_content_rect().height() / item_height();
        auto old_index = selection().first();
        auto new_index = model.index(max(0, old_index.row() - items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = visible_content_rect().height() / item_height();
        auto old_index = selection().first();
        auto new_index = model.index(min(model.row_count() - 1, old_index.row() + items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return Widget::keydown_event(event);
}

void AbstractTableView::scroll_into_view(const ModelIndex& index, Orientation orientation)
{
    auto rect = row_rect(index.row()).translated(0, -header_height());
    ScrollableWidget::scroll_into_view(rect, orientation);
}

void AbstractTableView::doubleclick_event(MouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == MouseButton::Left) {
        if (event.y() < header_height())
            return;
        if (!selection().is_empty()) {
            if (is_editable())
                begin_editing(selection().first());
            else
                activate_selected();
        }
    }
}

void AbstractTableView::context_menu_event(ContextMenuEvent& event)
{
    if (!model())
        return;
    if (event.position().y() < header_height()) {
        ensure_header_context_menu().popup(event.screen_position());
        return;
    }

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);
    if (index.is_valid()) {
        if (!selection().contains(index))
            selection().set(index);
    } else {
        selection().clear();
    }
    if (on_context_menu_request)
        on_context_menu_request(index, event);
}

void AbstractTableView::leave_event(Core::Event&)
{
    window()->set_override_cursor(StandardCursor::None);
    set_hovered_header_index(-1);
}

Gfx::Rect AbstractTableView::content_rect(int row, int column) const
{
    auto row_rect = this->row_rect(row);
    int x = 0;
    for (int i = 0; i < column; ++i)
        x += column_width(i) + horizontal_padding() * 2;

    return { row_rect.x() + x, row_rect.y(), column_width(column) + horizontal_padding() * 2, item_height() };
}

Gfx::Rect AbstractTableView::content_rect(const ModelIndex& index) const
{
    return content_rect(index.row(), index.column());
}

Gfx::Rect AbstractTableView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), max(content_size().width(), width()), item_height() };
}

Gfx::Point AbstractTableView::adjusted_position(const Gfx::Point& position) const
{
    return position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
}

void AbstractTableView::did_update_model()
{
    AbstractView::did_update_model();
    update_column_sizes();
    update_content_size();
    update();
}

}
