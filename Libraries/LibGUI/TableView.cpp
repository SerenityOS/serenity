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
#include <LibGfx/Palette.h>
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace GUI {

TableView::TableView(Widget* parent)
    : AbstractTableView(parent)
{
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
            auto column_metadata = model()->column_metadata(column_index);
            int column_width = this->column_width(column_index);
            const Gfx::Font& font = column_metadata.font ? *column_metadata.font : this->font();
            bool is_key_column = model()->key_column() == column_index;
            Gfx::Rect cell_rect(horizontal_padding() + x_offset, y, column_width, item_height());
            if (is_key_column) {
                auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            }
            auto cell_index = model()->index(row_index, column_index);

            if (auto* delegate = column_data(column_index).cell_painting_delegate.ptr()) {
                delegate->paint(painter, cell_rect, palette(), *model(), cell_index);
            } else {
                auto data = model()->data(cell_index);
                if (data.is_bitmap()) {
                    painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
                } else if (data.is_icon()) {
                    if (auto bitmap = data.as_icon().bitmap_for_size(16))
                        painter.blit(cell_rect.location(), *bitmap, bitmap->rect());
                } else {
                    Color text_color;
                    if (is_selected_row)
                        text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
                    else
                        text_color = model()->data(cell_index, Model::Role::ForegroundColor).to_color(palette().color(foreground_role()));
                    painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color, Gfx::TextElision::Right);
                }
            }
            x_offset += column_width + horizontal_padding() * 2;
        }
        ++painted_item_index;
    };

    Gfx::Rect unpainted_rect(0, header_height() + painted_item_index * item_height(), exposed_width, height());
    painter.fill_rect(unpainted_rect, widget_background_color);

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, vertical_scrollbar().value());
    if (headers_visible())
        paint_headers(painter);
}

}
