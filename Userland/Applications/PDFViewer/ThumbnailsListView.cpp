/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThumbnailsListView.h"
#include "PDFViewerWidget.h"
#include <LibGUI/ModelRole.h>
#include <LibGUI/Window.h>
#include <LibGfx/Color.h>
#include <LibGfx/Palette.h>

void ThumbnailsListView::paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index)
{
    bool is_selected_row = selection().contains_row(row_index);

    int y = painted_item_index * item_height();

    bool row_is_visible = y > vertical_scrollbar().value() - item_height() && y < vertical_scrollbar().value() + visible_content_rect().height() + item_height();

    Color background_color = is_selected_row ? palette().selection() : Gfx::Color::Transparent;

    Gfx::IntRect row_rect(0, y, content_width(), item_height());
    painter.fill_rect(row_rect, background_color);
    auto index = model()->index(row_index, 0);
    auto data = index.data();
    auto font = font_for_index(index);
    if (data.is_bitmap() && row_is_visible) {
        NonnullRefPtr<Gfx::Bitmap> bitmap = data.as_bitmap();
        if (bitmap->width() == 1) {
            auto pdf_viewer_widget = static_cast<PDFViewerWidget*>(this->window()->main_widget());
            bitmap = pdf_viewer_widget->update_thumbnail_for_page(row_index);
        }
        auto bitmap_x = (content_width() / 2) - (bitmap->width() / 2);
        auto bitmap_y = y + (item_height() / 2) - (bitmap->height() / 2) - 4;
        Gfx::Point bitmap_location(bitmap_x, bitmap_y);
        Gfx::IntRect bitmap_outline_rect(bitmap_x, bitmap_y, bitmap->width(), bitmap->height());
        painter.blit(bitmap_location, bitmap, bitmap->rect());
        painter.draw_rect(bitmap_outline_rect, Gfx::Color::Black);
        auto text_rect = row_rect;
        text_rect.translate_by(horizontal_padding(), -4);
        text_rect.set_width(text_rect.width() - horizontal_padding() * 2);
        auto text_alignment = index.data(GUI::ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterLeft);
        draw_item_text(painter, index, is_selected_row, text_rect, String::formatted("{}", row_index + 1).value().to_byte_string(), font, text_alignment, Gfx::TextElision::None);
    }
}

void ThumbnailsListView::select_list_item(int row_index)
{
    if (!(row_index >= 0))
        return;
    set_selection(model()->index(row_index));
    scroll_into_view(model()->index(row_index), false, true);
}
