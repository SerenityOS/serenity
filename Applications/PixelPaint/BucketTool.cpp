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

#include "BucketTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <AK/Queue.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

BucketTool::BucketTool()
{
}

BucketTool::~BucketTool()
{
}

static float color_distance_squared(const Gfx::Color& lhs, const Gfx::Color& rhs)
{
    int a = rhs.red() - lhs.red();
    int b = rhs.green() - lhs.green();
    int c = rhs.blue() - lhs.blue();
    return (a * a + b * b + c * c) / (255.0f * 255.0f);
}

static void flood_fill(Gfx::Bitmap& bitmap, const Gfx::IntPoint& start_position, Color target_color, Color fill_color, int threshold)
{
    ASSERT(bitmap.bpp() == 32);

    if (target_color == fill_color)
        return;

    if (!bitmap.rect().contains(start_position))
        return;

    float threshold_normalized_squared = (threshold / 100.0f) * (threshold / 100.0f);

    Queue<Gfx::IntPoint> queue;
    queue.enqueue(start_position);
    while (!queue.is_empty()) {
        auto position = queue.dequeue();

        auto pixel_color = bitmap.get_pixel<Gfx::StorageFormat::RGBA32>(position.x(), position.y());
        if (color_distance_squared(pixel_color, target_color) > threshold_normalized_squared)
            continue;

        bitmap.set_pixel<Gfx::StorageFormat::RGBA32>(position.x(), position.y(), fill_color);

        if (position.x() != 0)
            queue.enqueue(position.translated(-1, 0));

        if (position.x() != bitmap.width() - 1)
            queue.enqueue(position.translated(1, 0));

        if (position.y() != 0)
            queue.enqueue(position.translated(0, -1));

        if (position.y() != bitmap.height() - 1)
            queue.enqueue(position.translated(0, 1));
    }
}

void BucketTool::on_mousedown(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (!layer.rect().contains(event.position()))
        return;

    GUI::Painter painter(layer.bitmap());
    auto target_color = layer.bitmap().get_pixel(event.x(), event.y());

    flood_fill(layer.bitmap(), event.position(), target_color, m_editor->color_for(event), m_threshold);

    layer.did_modify_bitmap(*m_editor->image());
}

GUI::Widget* BucketTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& threshold_container = m_properties_widget->add<GUI::Widget>();
        threshold_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        threshold_container.set_preferred_size(0, 20);
        threshold_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& threshold_label = threshold_container.add<GUI::Label>("Threshold:");
        threshold_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        threshold_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        threshold_label.set_preferred_size(80, 20);

        auto& threshold_slider = threshold_container.add<GUI::HorizontalSlider>();
        threshold_slider.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        threshold_slider.set_preferred_size(0, 20);
        threshold_slider.set_range(0, 100);
        threshold_slider.set_value(m_threshold);
        threshold_slider.on_value_changed = [this](int value) {
            m_threshold = value;
        };
    }

    return m_properties_widget.ptr();
}

}
