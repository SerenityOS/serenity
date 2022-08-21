/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Aaron Yoder <aaronjyoder@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BucketTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/HashTable.h>
#include <AK/Queue.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

BucketTool::BucketTool()
{
    m_cursor = Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/bucket.png"sv).release_value_but_fixme_should_propagate_errors();
}

static float color_distance_squared(Gfx::Color const& lhs, Gfx::Color const& rhs)
{
    int a = rhs.red() - lhs.red();
    int b = rhs.green() - lhs.green();
    int c = rhs.blue() - lhs.blue();
    return (a * a + b * b + c * c) / (3.0f * 255.0f * 255.0f);
}

static bool can_paint(int x, int y, Gfx::Bitmap& bitmap, Gfx::Color const& target_color, float threshold_normalized_squared)
{
    auto pixel_color = bitmap.get_pixel<Gfx::StorageFormat::BGRA8888>(x, y);
    return color_distance_squared(pixel_color, target_color) <= threshold_normalized_squared;
}

static void fill_core(int, int, Gfx::Bitmap&, Gfx::Color const&, Gfx::Color const&, float);

static void fill_start(int x, int y, Gfx::Bitmap& bitmap, Gfx::Color const& target_color, Gfx::Color const& fill_color, float threshold_normalized_squared)
{
    // Move as far up and to the left as we can first and then start filling
    while (true) {
        int previous_x = x;
        int previous_y = y;
        while (y != 0 && can_paint(x, y - 1, bitmap, target_color, threshold_normalized_squared))
            y--;
        while (x != 0 && can_paint(x - 1, y, bitmap, target_color, threshold_normalized_squared))
            x--;
        if (x == previous_x && y == previous_y)
            break;
    }
    fill_core(x, y, bitmap, target_color, fill_color, threshold_normalized_squared);
}

static void fill_core(int x, int y, Gfx::Bitmap& bitmap, Gfx::Color const& target_color, Gfx::Color const& fill_color, float threshold_normalized_squared)
{
    int prev_row_length = 0;
    do {
        int row_length = 0;
        int start_x = x;
        // To handle the case where the previous row overhangs the current row without recursion, move x to the right until we can paint.
        // If we can't fill any more without extending beyond the previous row, return.
        // The else branch handles the opposite case, and moves x to the left and checks above it for a spot to start filling from.
        if (prev_row_length != 0 && !can_paint(x, y, bitmap, target_color, threshold_normalized_squared)) {
            do {
                if (--prev_row_length == 0)
                    return;
            } while (!can_paint(++x, y, bitmap, target_color, threshold_normalized_squared));
            start_x = x;
        } else {
            for (; x != 0 && can_paint(x - 1, y, bitmap, target_color, threshold_normalized_squared); row_length++, prev_row_length++) {
                bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(--x, y, fill_color);
                if (y != 0 && can_paint(x, y - 1, bitmap, target_color, threshold_normalized_squared))
                    fill_start(x, y - 1, bitmap, target_color, fill_color, threshold_normalized_squared);
            }
        }

        // Fill this row to the right as much as we can
        for (; start_x < bitmap.width() && can_paint(start_x, y, bitmap, target_color, threshold_normalized_squared); row_length++, start_x++) {
            bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(start_x, y, fill_color);
        }

        // If the previous row was longer than this row, look beyond the end of this row for pixels to fill until reaching the end of the previous row.
        // The else branch handles the opposite case, where the previous row was shorter. Look beyond the end of the previous row up into it for pixels to fill.
        if (row_length < prev_row_length) {
            for (int prev_row_end = x + prev_row_length; ++start_x < prev_row_end;) {
                if (can_paint(start_x, y, bitmap, target_color, threshold_normalized_squared))
                    fill_core(start_x, y, bitmap, target_color, fill_color, threshold_normalized_squared);
            }
        } else if (row_length > prev_row_length && y != 0) {
            for (int upward_x = x + prev_row_length; ++upward_x < start_x;) {
                if (can_paint(upward_x, y - 1, bitmap, target_color, threshold_normalized_squared))
                    fill_start(upward_x, y - 1, bitmap, target_color, fill_color, threshold_normalized_squared);
            }
        }

        prev_row_length = row_length;
    } while (prev_row_length != 0 && ++y < bitmap.height());
}

static void flood_fill(Gfx::Bitmap& bitmap, Gfx::IntPoint const& start_position, Color target_color, Color fill_color, int threshold)
{
    VERIFY(bitmap.bpp() == 32);

    if (target_color == fill_color)
        return;

    if (!bitmap.rect().contains(start_position))
        return;

    float threshold_normalized_squared = (threshold / 100.0f) * (threshold / 100.0f);

    if (can_paint(start_position.x(), start_position.y(), bitmap, target_color, threshold_normalized_squared))
        fill_start(start_position.x(), start_position.y(), bitmap, target_color, fill_color, threshold_normalized_squared);
}

void BucketTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!layer->rect().contains(layer_event.position()))
        return;

    GUI::Painter painter(layer->currently_edited_bitmap());
    auto target_color = layer->currently_edited_bitmap().get_pixel(layer_event.x(), layer_event.y());

    flood_fill(layer->currently_edited_bitmap(), layer_event.position(), target_color, m_editor->color_for(layer_event), m_threshold);

    layer->did_modify_bitmap();
    m_editor->did_complete_action(tool_name());
}

GUI::Widget* BucketTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& threshold_container = m_properties_widget->add<GUI::Widget>();
        threshold_container.set_fixed_height(20);
        threshold_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& threshold_label = threshold_container.add<GUI::Label>("Threshold:");
        threshold_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        threshold_label.set_fixed_size(80, 20);

        auto& threshold_slider = threshold_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%");
        threshold_slider.set_range(0, 100);
        threshold_slider.set_value(m_threshold);

        threshold_slider.on_change = [&](int value) {
            m_threshold = value;
        };
        set_primary_slider(&threshold_slider);
    }

    return m_properties_widget.ptr();
}

}
