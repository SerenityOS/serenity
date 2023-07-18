/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrushTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

void BrushTool::set_size(int size)
{
    if (size == m_size)
        return;
    m_size = size;
    refresh_editor_cursor();
}

void BrushTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    // Shift+Click draws a line from the last position to current one.
    if (layer_event.shift() && m_has_clicked) {
        draw_line(layer->get_scratch_edited_bitmap(), color_for(layer_event), m_last_position, layer_event.position());
        auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);
        layer->did_modify_bitmap(modified_rect);
        m_last_position = layer_event.position();
        return;
    }

    draw_point(layer->get_scratch_edited_bitmap(), color_for(layer_event), layer_event.position());

    layer->did_modify_bitmap(Gfx::IntRect::centered_on(layer_event.position(), Gfx::IntSize { m_size * 2, m_size * 2 }));
    m_last_position = layer_event.position();
    m_has_clicked = true;
    m_was_drawing = true;
}

void BrushTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!(layer_event.buttons() & GUI::MouseButton::Primary || layer_event.buttons() & GUI::MouseButton::Secondary))
        return;

    draw_line(layer->get_scratch_edited_bitmap(), color_for(layer_event), m_last_position, layer_event.position());

    auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);

    layer->did_modify_bitmap(modified_rect);
    m_last_position = layer_event.position();
    m_was_drawing = true;
}

void BrushTool::on_mouseup(Layer*, MouseEvent&)
{
    if (m_was_drawing) {
        m_editor->did_complete_action(tool_name());
        m_was_drawing = false;
    }
}

Color BrushTool::color_for(GUI::MouseEvent const& event)
{
    return m_editor->color_for(event);
}

void BrushTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point)
{
    constexpr auto flow_scale = 10;
    for (int y = point.y() - size(); y < point.y() + size(); y++) {
        for (int x = point.x() - size(); x < point.x() + size(); x++) {
            auto distance = point.distance_from({ x, y });
            if (x < 0 || x >= bitmap.width() || y < 0 || y >= bitmap.height())
                continue;
            if (distance >= size())
                continue;

            auto falloff = get_falloff(distance) * flow_scale;
            auto pixel_color = color;
            pixel_color.set_alpha(AK::min(falloff * 255, 255));
            set_pixel_with_possible_mask(x, y, bitmap.get_pixel(x, y).blend(pixel_color), bitmap);
        }
    }
}

void BrushTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end)
{
    int length_x = end.x() - start.x();
    int length_y = end.y() - start.y();
    float y_step = length_y == 0 ? 0 : (float)(length_y) / (float)(length_x);
    if (y_step > abs(length_y))
        y_step = abs(length_y);
    if (y_step < -abs(length_y))
        y_step = -abs(length_y);
    if (y_step == 0 && start.x() == end.x())
        return;

    int start_x = start.x();
    int end_x = end.x();
    int start_y = start.y();
    int end_y = end.y();
    if (start_x > end_x) {
        swap(start_x, end_x);
        swap(start_y, end_y);
    }

    float y = start_y;
    for (int x = start_x; x <= end_x; x++) {
        int start_step_y = y;
        int end_step_y = y + y_step;
        if (start_step_y > end_step_y)
            swap(start_step_y, end_step_y);
        for (int i = start_step_y; i <= end_step_y; i++)
            draw_point(bitmap, color, { x, i });
        y += y_step;
    }
}

ErrorOr<GUI::Widget*> BrushTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = TRY(GUI::Widget::try_create());
        (void)TRY(properties_widget->try_set_layout<GUI::VerticalBoxLayout>());

        auto size_container = TRY(properties_widget->try_add<GUI::Widget>());
        size_container->set_fixed_height(20);
        (void)TRY(size_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto size_label = TRY(size_container->try_add<GUI::Label>("Size:"_short_string));
        size_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label->set_fixed_size(80, 20);

        auto size_slider = TRY(size_container->try_add<GUI::ValueSlider>(Orientation::Horizontal, "px"_short_string));
        size_slider->set_range(1, 100);
        size_slider->set_value(m_size);
        size_slider->set_override_cursor(cursor());

        size_slider->on_change = [this, size_slider](int value) {
            set_size(value);
            // Update cursor to provide an instant preview for the selected size.
            size_slider->set_override_cursor(cursor());
        };
        set_primary_slider(size_slider);

        auto hardness_container = TRY(properties_widget->try_add<GUI::Widget>());
        hardness_container->set_fixed_height(20);
        (void)TRY(hardness_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto hardness_label = TRY(hardness_container->try_add<GUI::Label>(TRY("Hardness:"_string)));
        hardness_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label->set_fixed_size(80, 20);

        auto hardness_slider = TRY(hardness_container->try_add<GUI::ValueSlider>(Orientation::Horizontal, "%"_short_string));
        hardness_slider->set_range(1, 100);
        hardness_slider->set_value(m_hardness);

        hardness_slider->on_change = [this](int value) {
            set_hardness(value);
        };
        set_secondary_slider(hardness_slider);
        m_properties_widget = properties_widget;
    }

    return m_properties_widget.ptr();
}

NonnullRefPtr<Gfx::Bitmap> BrushTool::build_cursor()
{
    auto new_scale = m_editor ? m_editor->scale() : 1;
    auto scaled_size = size() * new_scale;
    auto containing_box_size = 2 * scaled_size;
    bool draw_ellipse = true;

    // If we have an ImageEditor scaled_size should not excede diagonal length of the ImageEditor
    if (m_editor) {
        // FIXME: The ImageEditor diagonal size could be saved on the ImageEditor and exposed to optimize.
        //        It would be nice if that was recalculated when the ImageEditor was resized
        if (m_scale_last_created_cursor != new_scale) {
            if (m_editor_previous_size != m_editor->size()) {
                m_editor_previous_size = m_editor->size();
                max_scaled_size = sqrt(pow(m_editor->width(), 2) + pow(m_editor->height(), 2));
            }
        }

        if (scaled_size > max_scaled_size) {
            scaled_size = max_scaled_size;
            containing_box_size = scaled_size * 2;
            // Due to performance issues when the cursor is too large we choose to draw it here.
            draw_ellipse = false;
        }
    }

    scaled_size = max(scaled_size, 1);
    containing_box_size = max(containing_box_size, 1);

    NonnullRefPtr<Gfx::Bitmap> new_cursor = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(containing_box_size, containing_box_size)).release_value_but_fixme_should_propagate_errors();

    Gfx::Painter painter { new_cursor };
    Gfx::AntiAliasingPainter aa_painter { painter };

    painter.draw_line({ scaled_size - 5, scaled_size }, { scaled_size + 5, scaled_size }, Color::LightGray, 3);
    painter.draw_line({ scaled_size, scaled_size - 5 }, { scaled_size, scaled_size + 5 }, Color::LightGray, 3);
    painter.draw_line({ scaled_size - 5, scaled_size }, { scaled_size + 5, scaled_size }, Color::MidGray, 1);
    painter.draw_line({ scaled_size, scaled_size - 5 }, { scaled_size, scaled_size + 5 }, Color::MidGray, 1);

    // If no ImageEditor is present, we cannot bind the ellipse within the editor. Then we will just draw the ellipse.
    if (!m_editor) {
        aa_painter.draw_ellipse(Gfx::IntRect(0, 0, containing_box_size, containing_box_size), Color::LightGray, 1);
    } else if (draw_ellipse) {
        Color color = m_editor->color_for(GUI::MouseButton::Primary);
        aa_painter.fill_ellipse(Gfx::IntRect(0, 0, containing_box_size, containing_box_size), color.with_alpha(100));
    }

    m_scale_last_created_cursor = new_scale;

    return new_cursor;
}

void BrushTool::refresh_editor_cursor()
{
    // FIXME: reverting the change to only build_cursor on m_editor scale size or tool size causes slow behavior when zoomed to max with the BrushTool
    m_cursor = build_cursor();
    m_size_previous = size();
    if (m_editor)
        m_editor->update_tool_cursor();
}

void BrushTool::set_current_position(Gfx::IntPoint cursor_position)
{
    m_current_tool_position = cursor_position;
}

}
