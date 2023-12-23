/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventReceiver.h>
#include <LibGUI/AbstractTableView.h>
#include <LibGUI/Action.h>
#include <LibGUI/Button.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace GUI {

AbstractTableView::AbstractTableView()
{
    REGISTER_BOOL_PROPERTY("column_headers_visible", column_headers_visible, set_column_headers_visible);

    set_selection_behavior(SelectionBehavior::SelectRows);
    m_corner_button = add<Button>();
    m_corner_button->move_to_back();
    m_corner_button->set_background_role(Gfx::ColorRole::ThreedShadow1);
    m_corner_button->set_fill_with_background_color(true);
    m_column_header = add<HeaderView>(*this, Gfx::Orientation::Horizontal);
    m_column_header->move_to_back();
    m_column_header->on_resize_doubleclick = [this](auto column) {
        auto_resize_column(column);
    };
    m_row_header = add<HeaderView>(*this, Gfx::Orientation::Vertical);
    m_row_header->move_to_back();
    m_row_header->set_visible(false);
    set_should_hide_unnecessary_scrollbars(true);
}

void AbstractTableView::select_all()
{
    selection().clear();
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        auto index = model()->index(item_index);
        selection().add(index);
    }
}

void AbstractTableView::auto_resize_column(int column)
{
    if (!model())
        return;

    if (!column_header().is_section_visible(column))
        return;

    auto& model = *this->model();
    int row_count = model.row_count();

    int header_width = m_column_header->font().width(model.column_name(column).release_value_but_fixme_should_propagate_errors());
    if (column == m_key_column && model.is_column_sortable(column))
        header_width += HeaderView::sorting_arrow_width + HeaderView::sorting_arrow_offset;

    int column_width = header_width;
    bool is_empty = true;
    for (int row = 0; row < row_count; ++row) {
        auto cell_data = model.index(row, column).data();
        int cell_width = 0;
        if (cell_data.is_icon()) {
            cell_width = cell_data.as_icon().bitmap_for_size(16)->width();
        } else if (cell_data.is_bitmap()) {
            cell_width = cell_data.as_bitmap().width();
        } else if (cell_data.is_valid()) {
            cell_width = font().width(cell_data.to_byte_string());
        }
        if (is_empty && cell_width > 0)
            is_empty = false;
        column_width = max(column_width, cell_width);
    }

    auto default_column_size = column_header().default_section_size(column);
    if (is_empty && column_header().is_default_section_size_initialized(column))
        column_header().set_section_size(column, default_column_size);
    else
        column_header().set_section_size(column, column_width);
}
void AbstractTableView::update_column_sizes()
{
    if (!model())
        return;

    auto& model = *this->model();
    int column_count = model.column_count();
    int row_count = model.row_count();

    for (int column = 0; column < column_count; ++column) {
        if (!column_header().is_section_visible(column))
            continue;
        int header_width = m_column_header->font().width(model.column_name(column).release_value_but_fixme_should_propagate_errors());
        if (column == m_key_column && model.is_column_sortable(column))
            header_width += HeaderView::sorting_arrow_width + HeaderView::sorting_arrow_offset;
        int column_width = header_width;
        for (int row = 0; row < row_count; ++row) {
            auto cell_data = model.index(row, column).data();
            int cell_width = 0;
            if (cell_data.is_icon()) {
                if (auto bitmap = cell_data.as_icon().bitmap_for_size(16))
                    cell_width = bitmap->width();
            } else if (cell_data.is_bitmap()) {
                cell_width = cell_data.as_bitmap().width();
            } else if (cell_data.is_valid()) {
                cell_width = font().width(cell_data.to_byte_string());
            }
            column_width = max(column_width, cell_width);
        }
        column_header().set_section_size(column, max(m_column_header->section_size(column), column_width));
    }
}

void AbstractTableView::update_row_sizes()
{
    if (!model())
        return;

    auto& model = *this->model();
    int row_count = model.row_count();

    for (int row = 0; row < row_count; ++row) {
        if (!row_header().is_section_visible(row))
            continue;
        row_header().set_section_size(row, row_height());
    }
}

void AbstractTableView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width = 0;
    int column_count = model()->column_count();

    for (int i = 0; i < column_count; ++i) {
        if (column_header().is_section_visible(i))
            content_width += column_width(i) + horizontal_padding() * 2;
    }
    int content_height = item_count() * row_height();

    set_content_size({ content_width, content_height });
    int row_width = row_header().is_visible() ? row_header().width() : 0;
    int column_height = column_header().is_visible() ? column_header().height() : 0;
    set_size_occupied_by_fixed_elements({ row_width, column_height });
    layout_headers();
}

TableCellPaintingDelegate* AbstractTableView::column_painting_delegate(int column) const
{
    // FIXME: This should return a const pointer I think..
    return const_cast<TableCellPaintingDelegate*>(m_column_painting_delegate.get(column).value_or(nullptr));
}

void AbstractTableView::set_column_painting_delegate(int column, OwnPtr<TableCellPaintingDelegate> delegate)
{
    if (!delegate)
        m_column_painting_delegate.remove(column);
    else
        m_column_painting_delegate.set(column, move(delegate));
}

int AbstractTableView::column_width(int column_index) const
{
    if (!model())
        return 0;
    return m_column_header->section_size(column_index);
}

void AbstractTableView::set_column_width(int column, int width)
{
    column_header().set_section_size(column, width);
}

int AbstractTableView::minimum_column_width(int)
{
    return 2;
}

int AbstractTableView::minimum_row_height(int)
{
    return 2;
}

Gfx::TextAlignment AbstractTableView::column_header_alignment(int column_index) const
{
    if (!model())
        return Gfx::TextAlignment::CenterLeft;
    return m_column_header->section_alignment(column_index);
}

void AbstractTableView::set_column_header_alignment(int column, Gfx::TextAlignment alignment)
{
    column_header().set_section_alignment(column, alignment);
}

void AbstractTableView::mousedown_event(MouseEvent& event)
{
    m_tab_moves = 0;
    if (!model())
        return AbstractView::mousedown_event(event);

    if (event.button() != MouseButton::Primary)
        return AbstractView::mousedown_event(event);

    bool is_toggle;
    auto index = index_at_event_position(event.position(), is_toggle);

    if (index.is_valid() && is_toggle && model()->row_count(index)) {
        toggle_index(index);
        return;
    }

    AbstractView::mousedown_event(event);
}

ModelIndex AbstractTableView::index_at_event_position(Gfx::IntPoint position, bool& is_toggle) const
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

ModelIndex AbstractTableView::index_at_event_position(Gfx::IntPoint position) const
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

void AbstractTableView::move_cursor_relative(int vertical_steps, int horizontal_steps, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();
    ModelIndex new_index;
    if (cursor_index().is_valid()) {
        new_index = model.index(cursor_index().row() + vertical_steps, cursor_index().column() + horizontal_steps);
    } else {
        new_index = model.index(0, 0);
    }
    if (new_index.is_valid()) {
        set_cursor(new_index, selection_update);
    }
}

void AbstractTableView::scroll_into_view(ModelIndex const& index, bool scroll_horizontally, bool scroll_vertically)
{
    Gfx::IntRect rect;
    switch (selection_behavior()) {
    case SelectionBehavior::SelectItems:
        rect = content_rect(index);
        if (row_header().is_visible())
            rect.set_left(rect.left() - row_header().width());
        break;
    case SelectionBehavior::SelectRows:
        rect = row_rect(index.row());
        break;
    }
    if (column_header().is_visible())
        rect.set_top(rect.top() - column_header().height());
    AbstractScrollableWidget::scroll_into_view(rect, scroll_horizontally, scroll_vertically);
}

void AbstractTableView::context_menu_event(ContextMenuEvent& event)
{
    if (!model())
        return;

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

Gfx::IntRect AbstractTableView::paint_invalidation_rect(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    return row_rect(index.row());
}

Gfx::IntRect AbstractTableView::content_rect(int row, int column) const
{
    auto row_rect = this->row_rect(row);
    int x = 0;
    for (int i = 0; i < column; ++i)
        x += column_width(i) + horizontal_padding() * 2;

    return { row_rect.x() + x, row_rect.y(), column_width(column) + horizontal_padding() * 2, row_height() };
}

Gfx::IntRect AbstractTableView::content_rect(ModelIndex const& index) const
{
    return content_rect(index.row(), index.column());
}

Gfx::IntRect AbstractTableView::content_rect_minus_scrollbars(ModelIndex const& index) const
{
    auto naive_content_rect = content_rect(index.row(), index.column());
    return { naive_content_rect.x() - horizontal_scrollbar().value(), naive_content_rect.y() - vertical_scrollbar().value(), naive_content_rect.width(), naive_content_rect.height() };
}

Gfx::IntRect AbstractTableView::row_rect(int item_index) const
{
    return { row_header().is_visible() ? row_header().width() : 0,
        (column_header().is_visible() ? column_header().height() : 0) + (item_index * row_height()),
        max(content_size().width(), width()),
        row_height() };
}

Gfx::IntPoint AbstractTableView::adjusted_position(Gfx::IntPoint position) const
{
    return position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
}

void AbstractTableView::model_did_update(unsigned flags)
{
    AbstractView::model_did_update(flags);
    update_row_sizes();
    if (!(flags & Model::UpdateFlag::DontResizeColumns))
        update_column_sizes();

    update_content_size();
    update();
}

void AbstractTableView::resize_event(ResizeEvent& event)
{
    AbstractView::resize_event(event);
    layout_headers();
}

void AbstractTableView::header_did_change_section_size(Badge<HeaderView>, Gfx::Orientation, int, int)
{
    update_content_size();
    update();
}

void AbstractTableView::header_did_change_section_visibility(Badge<HeaderView>, Gfx::Orientation, int, bool)
{
    update_content_size();
    update();

    if (on_visible_columns_changed)
        on_visible_columns_changed();
}

void AbstractTableView::set_default_column_width(int column, int width)
{
    column_header().set_default_section_size(column, width);
}

void AbstractTableView::set_column_visible(int column, bool visible)
{
    column_header().set_section_visible(column, visible);
}

ErrorOr<String> AbstractTableView::get_visible_columns() const
{
    StringBuilder builder;

    bool first = true;
    for (int column = 0; column < model()->column_count(); ++column) {
        if (!column_header().is_section_visible(column))
            continue;

        if (first) {
            TRY(builder.try_appendff("{}", column));
            first = false;
        } else {
            TRY(builder.try_appendff(",{}", column));
        }
    }

    return builder.to_string();
}

void AbstractTableView::set_visible_columns(StringView column_names)
{
    for (int column = 0; column < model()->column_count(); ++column)
        column_header().set_section_visible(column, false);

    column_names.for_each_split_view(',', SplitBehavior::Nothing, [&, this](StringView column_id_string) {
        if (auto column = column_id_string.to_number<int>(); column.has_value()) {
            column_header().set_section_visible(column.value(), true);
        }
    });
}

void AbstractTableView::set_column_headers_visible(bool visible)
{
    column_header().set_visible(visible);
}

bool AbstractTableView::column_headers_visible() const
{
    return column_header().is_visible();
}

void AbstractTableView::did_scroll()
{
    AbstractView::did_scroll();
    layout_headers();
}

void AbstractTableView::layout_headers()
{
    if (column_header().is_visible()) {
        int row_header_width = row_header().is_visible() ? row_header().width() : 0;
        int vertical_scrollbar_width = vertical_scrollbar().is_visible() ? vertical_scrollbar().width() : 0;

        int x = frame_thickness() + row_header_width - horizontal_scrollbar().value();
        int y = frame_thickness();
        int width = max(content_width(), rect().width() - frame_thickness() * 2 - row_header_width - vertical_scrollbar_width);

        column_header().set_relative_rect(x, y, width, column_header().effective_min_size().height().as_int());
    }

    if (row_header().is_visible()) {
        int column_header_height = column_header().is_visible() ? column_header().height() : 0;
        int horizontal_scrollbar_height = horizontal_scrollbar().is_visible() ? horizontal_scrollbar().height() : 0;

        int x = frame_thickness();
        int y = frame_thickness() + column_header_height - vertical_scrollbar().value();
        int height = max(content_height(), rect().height() - frame_thickness() * 2 - column_header_height - horizontal_scrollbar_height);

        row_header().set_relative_rect(x, y, row_header().effective_min_size().width().as_int(), height);
    }

    if (row_header().is_visible() && column_header().is_visible()) {
        m_corner_button->set_relative_rect(frame_thickness(), frame_thickness(), row_header().width(), column_header().height());
        m_corner_button->set_visible(true);
    } else {
        m_corner_button->set_visible(false);
    }
}

void AbstractTableView::keydown_event(KeyEvent& event)
{
    if (is_tab_key_navigation_enabled()) {
        if (!event.modifiers() && event.key() == KeyCode::Key_Tab) {
            move_cursor(CursorMovement::Right, SelectionUpdate::Set);
            event.accept();
            ++m_tab_moves;
            return;
        } else if (is_navigation(event)) {
            if (event.key() == KeyCode::Key_Return) {
                move_cursor_relative(0, -m_tab_moves, SelectionUpdate::Set);
            }
            m_tab_moves = 0;
        }

        if (event.modifiers() == KeyModifier::Mod_Shift && event.key() == KeyCode::Key_Tab) {
            move_cursor(CursorMovement::Left, SelectionUpdate::Set);
            event.accept();
            return;
        }
    }

    AbstractView::keydown_event(event);
}

bool AbstractTableView::is_navigation(GUI::KeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_Tab:
    case KeyCode::Key_Left:
    case KeyCode::Key_Right:
    case KeyCode::Key_Up:
    case KeyCode::Key_Down:
    case KeyCode::Key_Return:
    case KeyCode::Key_Home:
    case KeyCode::Key_End:
    case KeyCode::Key_PageUp:
    case KeyCode::Key_PageDown:
        return true;
    default:
        return false;
    }
}

Gfx::IntPoint AbstractTableView::automatic_scroll_delta_from_position(Gfx::IntPoint pos) const
{
    if (pos.y() > column_header().height() + autoscroll_threshold())
        return AbstractScrollableWidget::automatic_scroll_delta_from_position(pos);

    Gfx::IntPoint position_excluding_header = { pos.x(), pos.y() - column_header().height() };
    return AbstractScrollableWidget::automatic_scroll_delta_from_position(position_excluding_header);
}

}
