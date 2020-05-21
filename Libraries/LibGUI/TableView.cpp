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
#include <Kernel/KeyCode.h>
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

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

    int exposed_width = max(content_size().width(), width());
    int y_offset = header_height();

    bool dummy;
    int first_visible_row = index_at_event_position(frame_inner_rect().top_left(), dummy).row();
    int last_visible_row = index_at_event_position(frame_inner_rect().bottom_right(), dummy).row();

    if (first_visible_row == -1)
        first_visible_row = 0;
    if (last_visible_row == -1)
        last_visible_row = model()->row_count() - 1;

    int painted_item_index = first_visible_row;

    for (int row_index = first_visible_row; row_index <= last_visible_row; ++row_index) {
        bool is_selected_row = selection().contains_row(row_index);
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row) {
            background_color = is_focused() ? palette().selection() : palette().inactive_selection();
            key_column_background_color = is_focused() ? palette().selection() : palette().inactive_selection();
        } else {
            if (alternating_row_colors() && (painted_item_index % 2)) {
                background_color = widget_background_color.darkened(0.8f);
                key_column_background_color = widget_background_color.darkened(0.7f);
            } else {
                background_color = widget_background_color;
                key_column_background_color = widget_background_color.darkened(0.9f);
            }
        }
        painter.fill_rect(row_rect(painted_item_index), background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < model()->column_count(); ++column_index) {
            if (is_column_hidden(column_index))
                continue;
            int column_width = this->column_width(column_index);
            bool is_key_column = model()->key_column() == column_index;
            Gfx::Rect cell_rect(horizontal_padding() + x_offset, y, column_width, item_height());
            auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
            if (is_key_column)
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            auto cell_index = model()->index(row_index, column_index);

            if (auto* delegate = column_data(column_index).cell_painting_delegate.ptr()) {
                delegate->paint(painter, cell_rect, palette(), *model(), cell_index);
            } else {
                auto data = model()->data(cell_index);
                if (data.is_bitmap()) {
                    painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
                } else if (data.is_icon()) {
                    if (auto bitmap = data.as_icon().bitmap_for_size(16)) {
                        if (m_hovered_index.is_valid() && cell_index.row() == m_hovered_index.row())
                            painter.blit_brightened(cell_rect.location(), *bitmap, bitmap->rect());
                        else
                            painter.blit(cell_rect.location(), *bitmap, bitmap->rect());
                    }
                } else {
                    Color text_color;
                    if (is_selected_row)
                        text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
                    else
                        text_color = model()->data(cell_index, Model::Role::ForegroundColor).to_color(palette().color(foreground_role()));
                    if (!is_selected_row) {
                        auto cell_background_color = model()->data(cell_index, Model::Role::BackgroundColor);
                        if (cell_background_color.is_valid())
                            painter.fill_rect(cell_rect_for_fill, cell_background_color.to_color(background_color));
                    }
                    auto text_alignment = model()->data(cell_index, Model::Role::TextAlignment).to_text_alignment(Gfx::TextAlignment::Center);
                    painter.draw_text(cell_rect, data.to_string(), font_for_index(cell_index), text_alignment, text_color, Gfx::TextElision::Right);
                }
            }
            x_offset += column_width + horizontal_padding() * 2;
        }
        ++painted_item_index;
    };

    Gfx::Rect unpainted_rect(0, header_height() + painted_item_index * item_height(), exposed_width, height());
    if (fill_with_background_color())
        painter.fill_rect(unpainted_rect, widget_background_color);

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, vertical_scrollbar().value());
    if (headers_visible())
        paint_headers(painter);
}

void TableView::keydown_event(KeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        activate_selected();
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        move_selection(-1);
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        move_selection(1);
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

}
