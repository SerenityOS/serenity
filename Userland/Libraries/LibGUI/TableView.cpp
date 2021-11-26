/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/HeaderView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, TableView)

namespace GUI {

TableView::TableView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
}

TableView::~TableView()
{
}

void TableView::paint_event(PaintEvent& event)
{
    Color widget_background_color = palette().color(background_role());
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    if (fill_with_background_color())
        painter.fill_rect(event.rect(), widget_background_color);
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    if (!model())
        return;

    auto selection_color = is_focused() ? palette().selection() : palette().inactive_selection();

    int exposed_width = max(content_size().width(), width());
    int x_offset = row_header().is_visible() ? row_header().width() : 0;
    int y_offset = column_header().is_visible() ? column_header().height() : 0;

    bool dummy;
    int first_visible_row = index_at_event_position(frame_inner_rect().top_left().translated(x_offset, y_offset), dummy).row();
    int last_visible_row = index_at_event_position(frame_inner_rect().bottom_right().translated(x_offset, y_offset), dummy).row();

    if (first_visible_row == -1)
        first_visible_row = 0;
    if (last_visible_row == -1)
        last_visible_row = model()->row_count() - 1;

    int painted_item_index = first_visible_row;

    for (int row_index = first_visible_row; row_index <= last_visible_row; ++row_index) {
        bool is_selected_row = selection().contains_row(row_index);
        int y = y_offset + painted_item_index * row_height();

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row && highlight_selected_rows()) {
            background_color = selection_color;
            key_column_background_color = selection_color;
        } else {
            if (alternating_row_colors() && (painted_item_index % 2)) {
                background_color = widget_background_color.darkened(0.8f);
                key_column_background_color = widget_background_color.darkened(0.7f);
            } else {
                background_color = widget_background_color;
                key_column_background_color = widget_background_color.darkened(0.9f);
            }
        }

        auto row_rect = this->row_rect(painted_item_index);
        painter.fill_rect(row_rect, background_color);

        int x = x_offset;
        for (int column_index = 0; column_index < model()->column_count(); ++column_index) {
            if (!column_header().is_section_visible(column_index))
                continue;
            int column_width = this->column_width(column_index);
            bool is_key_column = m_key_column == column_index;
            Gfx::IntRect cell_rect(horizontal_padding() + x, y, column_width, row_height());
            auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
            if (is_key_column && is_key_column_highlighted())
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            auto cell_index = model()->index(row_index, column_index);

            if (auto* delegate = column_painting_delegate(column_index)) {
                delegate->paint(painter, cell_rect, palette(), cell_index);
            } else {
                auto data = cell_index.data();
                if (data.is_bitmap()) {
                    auto cell_constrained_bitmap_rect = data.as_bitmap().rect();
                    if (data.as_bitmap().rect().width() > column_width)
                        cell_constrained_bitmap_rect.set_width(column_width);
                    if (data.as_bitmap().rect().height() > row_height())
                        cell_constrained_bitmap_rect.set_height(row_height());
                    cell_rect.set_y(cell_rect.y() + (row_height() - cell_constrained_bitmap_rect.height()) / 2);
                    cell_rect.set_x(cell_rect.x() + (column_width - cell_constrained_bitmap_rect.width()) / 2);
                    painter.blit(cell_rect.location(), data.as_bitmap(), cell_constrained_bitmap_rect);
                } else if (data.is_icon()) {
                    if (auto bitmap = data.as_icon().bitmap_for_size(16)) {
                        cell_rect.set_y(cell_rect.y() + (row_height() - bitmap->height()) / 2);
                        if (is_selected_row) {
                            auto tint = selection_color.with_alpha(100);
                            painter.blit_filtered(cell_rect.location(), *bitmap, bitmap->rect(), [&](auto src) { return src.blend(tint); });
                        } else if (m_hovered_index.is_valid() && cell_index.row() == m_hovered_index.row()) {
                            painter.blit_brightened(cell_rect.location(), *bitmap, bitmap->rect());
                        } else {
                            auto opacity = cell_index.data(ModelRole::IconOpacity).as_float_or(1.0f);
                            painter.blit(cell_rect.location(), *bitmap, bitmap->rect(), opacity);
                        }
                    }
                } else {
                    if (!is_selected_row) {
                        auto cell_background_color = cell_index.data(ModelRole::BackgroundColor);
                        if (cell_background_color.is_valid())
                            painter.fill_rect(cell_rect_for_fill, cell_background_color.to_color(background_color));
                    }

                    auto text_alignment = cell_index.data(ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterLeft);
                    draw_item_text(painter, cell_index, is_selected_row, cell_rect, data.to_string(), font_for_index(cell_index), text_alignment, Gfx::TextElision::Right);
                }
            }

            if (m_grid_style == GridStyle::Horizontal || m_grid_style == GridStyle::Both)
                painter.draw_line(cell_rect_for_fill.bottom_left(), cell_rect_for_fill.bottom_right(), palette().ruler());
            if (m_grid_style == GridStyle::Vertical || m_grid_style == GridStyle::Both)
                painter.draw_line(cell_rect_for_fill.top_right(), cell_rect_for_fill.bottom_right(), palette().ruler());

            if (selection_behavior() == SelectionBehavior::SelectItems && cell_index == cursor_index())
                painter.draw_rect(cell_rect_for_fill, palette().text_cursor());

            x += column_width + horizontal_padding() * 2;
        }

        if (is_focused() && selection_behavior() == SelectionBehavior::SelectRows && row_index == cursor_index().row()) {
            painter.draw_rect(row_rect, widget_background_color);
            painter.draw_focus_rect(row_rect, palette().focus_outline());
        }

        if (has_pending_drop() && selection_behavior() == SelectionBehavior::SelectRows && row_index == drop_candidate_index().row()) {
            painter.draw_rect(row_rect, palette().selection(), true);
        }
        ++painted_item_index;
    };

    Gfx::IntRect unpainted_rect(0, column_header().height() + painted_item_index * row_height(), exposed_width, height());
    if (fill_with_background_color())
        painter.fill_rect(unpainted_rect, widget_background_color);
}

void TableView::keydown_event(KeyEvent& event)
{
    if (!model())
        return AbstractTableView::keydown_event(event);

    AbstractTableView::keydown_event(event);

    if (event.is_accepted())
        return;

    auto is_delete = event.key() == Key_Delete;
    auto is_backspace = event.key() == Key_Backspace;
    auto is_clear = is_delete || is_backspace;
    if (is_editable() && edit_triggers() & EditTrigger::AnyKeyPressed && (event.code_point() != 0 || is_clear)) {
        begin_editing(cursor_index());
        if (m_editing_delegate) {
            if (is_delete)
                m_editing_delegate->set_value(String {});
            else if (is_backspace)
                m_editing_delegate->set_value(String::empty());
            else
                m_editing_delegate->set_value(event.text(), ModelEditingDelegate::SelectionBehavior::DoNotSelect);
        }
    }
}

void TableView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();
    switch (movement) {
    case CursorMovement::Left:
        move_cursor_relative(0, -1, selection_update);
        break;
    case CursorMovement::Right:
        move_cursor_relative(0, 1, selection_update);
        break;
    case CursorMovement::Up:
        move_cursor_relative(-1, 0, selection_update);
        break;
    case CursorMovement::Down:
        move_cursor_relative(1, 0, selection_update);
        break;
    case CursorMovement::Home: {
        auto index = model.index(0, 0);
        set_cursor(index, selection_update);
        break;
    }
    case CursorMovement::End: {
        auto index = model.index(model.row_count() - 1, 0);
        set_cursor(index, selection_update);
        break;
    }
    case CursorMovement::PageUp: {
        int items_per_page = visible_content_rect().height() / row_height();
        auto old_index = selection().first();
        auto new_index = model.index(max(0, old_index.row() - items_per_page), old_index.column());
        if (model.is_within_range(new_index))
            set_cursor(new_index, selection_update);
        break;
    }
    case CursorMovement::PageDown: {
        int items_per_page = visible_content_rect().height() / row_height();
        auto old_index = selection().first();
        auto new_index = model.index(min(model.row_count() - 1, old_index.row() + items_per_page), old_index.column());
        if (model.is_within_range(new_index))
            set_cursor(new_index, selection_update);
        break;
    }
    }
}

void TableView::set_grid_style(GridStyle style)
{
    if (m_grid_style == style)
        return;
    m_grid_style = style;
    update();
}

}
