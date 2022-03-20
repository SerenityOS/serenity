/*
 * Copyright (c) 2022, Michiel Vrins
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ElementSizePreviewWidget.h"
#include <LibGUI/Painter.h>

namespace Browser {

void ElementSizePreviewWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::White);

    int outer_margin = 10;
    int text_width_padding = 4;
    int text_height_padding = 4;
    int content_width_padding = 8;
    int content_height_padding = 8;

    auto content_size_text = String::formatted("{}x{}", m_node_content_width, m_node_content_height);

    int inner_content_width = max(100, font().width(content_size_text) + 2 * content_width_padding);
    int inner_content_height = max(15, font().glyph_height() + 2 * content_height_padding);

    auto format_size_text = [&](float size) {
        return String::formatted("{:.4f}", size);
    };

    auto compute_text_string_width = [&](float size) {
        return font().width(format_size_text(size)) + 2 * text_width_padding;
    };

    int margin_left_width = max(25, compute_text_string_width(m_node_box_sizing.margin.left));
    int margin_right_width = max(25, compute_text_string_width(m_node_box_sizing.margin.right));

    int border_left_width = max(25, compute_text_string_width(m_node_box_sizing.border.left));
    int border_right_width = max(25, compute_text_string_width(m_node_box_sizing.border.right));

    int padding_left_width = max(25, compute_text_string_width(m_node_box_sizing.padding.left));
    int padding_right_width = max(25, compute_text_string_width(m_node_box_sizing.padding.right));

    // outer rect
    auto margin_rect = to_widget_rect({ outer_margin,
        outer_margin,
        inner_content_width + border_left_width + border_right_width + margin_left_width + margin_right_width + padding_left_width + padding_right_width,
        inner_content_height * 7 });

    Gfx::IntSize content_size { margin_rect.width() + 2 * outer_margin, margin_rect.height() + 2 * outer_margin };
    set_content_size(content_size);
    auto border_rect = margin_rect;
    border_rect.take_from_left(margin_left_width);
    border_rect.take_from_right(margin_right_width);
    border_rect.shrink({ 0, inner_content_height * 2 });
    auto padding_rect = border_rect;
    padding_rect.take_from_left(border_left_width);
    padding_rect.take_from_right(border_right_width);
    padding_rect.shrink({ 0, inner_content_height * 2 });
    auto content_rect = padding_rect;
    content_rect.take_from_left(padding_left_width);
    content_rect.take_from_right(padding_right_width);
    content_rect.shrink({ 0, inner_content_height * 2 });

    auto draw_borders = [&](Gfx::IntRect rect, Color color) {
        painter.fill_rect(rect.take_from_top(1), color);
        painter.fill_rect(rect.take_from_right(1), color);
        painter.fill_rect(rect.take_from_bottom(1), color);
        painter.fill_rect(rect.take_from_left(1), color);
    };

    auto draw_size_texts = [&](Gfx::IntRect rect, Color color, Web::Layout::PixelBox box) {
        painter.draw_text(rect, format_size_text(box.top), font(), Gfx::TextAlignment::TopCenter, color);
        painter.draw_text(rect, format_size_text(box.right), font(), Gfx::TextAlignment::CenterRight, color);
        painter.draw_text(rect, format_size_text(box.bottom), font(), Gfx::TextAlignment::BottomCenter, color);
        painter.draw_text(rect, format_size_text(box.left), font(), Gfx::TextAlignment::CenterLeft, color);
    };

    // paint margin box
    painter.fill_rect(margin_rect, Color(249, 204, 157));
    draw_borders(margin_rect, Color::Black);
    margin_rect.shrink(1, 1, 1, 1);
    margin_rect.shrink(text_height_padding, text_width_padding, text_height_padding, text_width_padding);
    painter.draw_text(margin_rect, "margin", font(), Gfx::TextAlignment::TopLeft, Color::Black);
    draw_size_texts(margin_rect, Color::Black, m_node_box_sizing.margin);

    // paint border box
    painter.fill_rect(border_rect, Color(253, 221, 155));
    draw_borders(border_rect, Color::Black);
    border_rect.shrink(1, 1, 1, 1);
    border_rect.shrink(text_height_padding, text_width_padding, text_height_padding, text_width_padding);
    painter.draw_text(border_rect, "border", font(), Gfx::TextAlignment::TopLeft, Color::Black);
    draw_size_texts(border_rect, Color::Black, m_node_box_sizing.border);

    // paint padding box
    painter.fill_rect(padding_rect, Color(195, 208, 139));
    draw_borders(padding_rect, Color::Black);
    padding_rect.shrink(1, 1, 1, 1);
    padding_rect.shrink(text_height_padding, text_width_padding, text_height_padding, text_width_padding);
    painter.draw_text(padding_rect, "padding", font(), Gfx::TextAlignment::TopLeft, Color::Black);
    draw_size_texts(padding_rect, Color::Black, m_node_box_sizing.padding);

    // paint content box
    painter.fill_rect(content_rect, Color(140, 182, 192));
    draw_borders(content_rect, Color::Black);
    content_rect.shrink(1, 1, 1, 1);
    painter.draw_text(content_rect, content_size_text, font(), Gfx::TextAlignment::Center, Color::Black);
}

}
