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
    m_cursor = NonnullRefPtr<Gfx::Bitmap const> { Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/bucket.png"sv).release_value_but_fixme_should_propagate_errors() };
}

static void flood_fill(Gfx::Bitmap& bitmap, Gfx::IntPoint start_position, Color fill_color, int threshold)
{
    VERIFY(bitmap.bpp() == 32);

    if (!bitmap.rect().contains(start_position))
        return;

    auto pixel_visited = [&](Gfx::IntPoint location) {
        bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(location.x(), location.y(), fill_color);
    };

    bitmap.flood_visit_from_point(start_position, threshold, move(pixel_visited));
}

void BucketTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!layer->rect().contains(layer_event.position()))
        return;

    if (auto selection = layer->image().selection(); !selection.is_empty() && !selection.is_selected(event.image_event().position()))
        return;

    GUI::Painter painter(layer->get_scratch_edited_bitmap());

    flood_fill(layer->get_scratch_edited_bitmap(), layer_event.position(), m_editor->color_for(layer_event), m_threshold);

    layer->did_modify_bitmap(layer->get_scratch_edited_bitmap().rect());
    m_editor->did_complete_action(tool_name());
}

NonnullRefPtr<GUI::Widget> BucketTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& threshold_container = properties_widget->add<GUI::Widget>();
        threshold_container.set_fixed_height(20);
        threshold_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& threshold_label = threshold_container.add<GUI::Label>("Threshold:"_string);
        threshold_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        threshold_label.set_fixed_size(80, 20);

        auto& threshold_slider = threshold_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        threshold_slider.set_range(0, 100);
        threshold_slider.set_value(m_threshold);

        threshold_slider.on_change = [this](int value) {
            m_threshold = value;
        };
        set_primary_slider(&threshold_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
