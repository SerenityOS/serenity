/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Aaron Yoder <aaronjyoder@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BucketTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include "../Mask.h"
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
    int d = rhs.alpha() - lhs.alpha();
    return (a * a + b * b + c * c + d * d) / (4.0f * 255.0f * 255.0f);
}

static bool can_paint(Gfx::IntPoint point, Gfx::Bitmap& bitmap, Gfx::Color const& target_color, float threshold_normalized_squared)
{
    auto pixel_color = bitmap.get_pixel<Gfx::StorageFormat::BGRA8888>(point.x(), point.y());
    return color_distance_squared(pixel_color, target_color) <= threshold_normalized_squared;
}

static void flood_fill(Gfx::Bitmap& bitmap, Gfx::IntPoint const& start_position, Color target_color, Color fill_color, int threshold)
{
    VERIFY(bitmap.bpp() == 32);

    if (target_color == fill_color)
        return;

    if (!bitmap.rect().contains(start_position))
        return;

    float threshold_normalized_squared = (threshold / 100.0f) * (threshold / 100.0f);

    // Create Mask which will track already-colored pixels
    Mask flood_mask = Mask::empty(bitmap.rect());

    Queue<Gfx::IntPoint> points_to_visit = Queue<Gfx::IntPoint>();

    points_to_visit.enqueue({ start_position.x(), start_position.y() });
    bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(start_position.x(), start_position.y(), fill_color);
    flood_mask.set(start_position.x(), start_position.y(), 1);

    // This implements a non-recursive flood fill. This is a breadth-first search of paintable neighbors
    // As we find neighbors that are paintable we update their pixel, add them to the queue, and mark them in the mask
    while (!points_to_visit.is_empty()) {
        auto current_point = points_to_visit.dequeue();
        auto candidate_points = Array {
            current_point.moved_left(1),
            current_point.moved_right(1),
            current_point.moved_up(1),
            current_point.moved_down(1)
        };
        for (auto candidate_point : candidate_points) {
            if (!bitmap.rect().contains(candidate_point))
                continue;
            if (flood_mask.get(candidate_point.x(), candidate_point.y()) == 0 && can_paint(candidate_point, bitmap, target_color, threshold_normalized_squared)) {
                points_to_visit.enqueue(candidate_point);
                bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(candidate_point.x(), candidate_point.y(), fill_color);
            }
            flood_mask.set(candidate_point.x(), candidate_point.y(), 0xFF);
        }
    }
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
