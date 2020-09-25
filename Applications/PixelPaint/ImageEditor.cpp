/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "ImageEditor.h"
#include "Image.h"
#include "Layer.h"
#include "Tool.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

ImageEditor::ImageEditor()
{
}

ImageEditor::~ImageEditor()
{
    if (m_image)
        m_image->remove_client(*this);
}

void ImageEditor::set_image(RefPtr<Image> image)
{
    if (m_image)
        m_image->remove_client(*this);

    m_image = move(image);
    update();

    if (m_image)
        m_image->add_client(*this);
}

void ImageEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    Gfx::StylePainter::paint_transparency_grid(painter, rect(), palette());

    if (m_image) {
        painter.draw_rect(m_editor_image_rect.inflated(2, 2), Color::Black);
        m_image->paint_into(painter, m_editor_image_rect);
    }

    if (m_active_layer) {
        painter.draw_rect(enclosing_int_rect(image_rect_to_editor_rect(m_active_layer->relative_rect())).inflated(2, 2), Color::Black);
    }
}

Gfx::FloatRect ImageEditor::layer_rect_to_editor_rect(const Layer& layer, const Gfx::IntRect& layer_rect) const
{
    return image_rect_to_editor_rect(layer_rect.translated(layer.location()));
}

Gfx::FloatRect ImageEditor::image_rect_to_editor_rect(const Gfx::IntRect& image_rect) const
{
    Gfx::FloatRect editor_rect;
    editor_rect.set_location(image_position_to_editor_position(image_rect.location()));
    editor_rect.set_width((float)image_rect.width() * m_scale);
    editor_rect.set_height((float)image_rect.height() * m_scale);
    return editor_rect;
}

Gfx::FloatRect ImageEditor::editor_rect_to_image_rect(const Gfx::IntRect& editor_rect) const
{
    Gfx::FloatRect image_rect;
    image_rect.set_location(editor_position_to_image_position(editor_rect.location()));
    image_rect.set_width((float)editor_rect.width() / m_scale);
    image_rect.set_height((float)editor_rect.height() / m_scale);
    return image_rect;
}

Gfx::FloatPoint ImageEditor::layer_position_to_editor_position(const Layer& layer, const Gfx::IntPoint& layer_position) const
{
    return image_position_to_editor_position(layer_position.translated(layer.location()));
}

Gfx::FloatPoint ImageEditor::image_position_to_editor_position(const Gfx::IntPoint& image_position) const
{
    Gfx::FloatPoint editor_position;
    editor_position.set_x(m_editor_image_rect.x() + ((float)image_position.x() * m_scale));
    editor_position.set_y(m_editor_image_rect.y() + ((float)image_position.y() * m_scale));
    return editor_position;
}

Gfx::FloatPoint ImageEditor::editor_position_to_image_position(const Gfx::IntPoint& editor_position) const
{
    Gfx::FloatPoint image_position;
    image_position.set_x(((float)editor_position.x() - m_editor_image_rect.x()) / m_scale);
    image_position.set_y(((float)editor_position.y() - m_editor_image_rect.y()) / m_scale);
    return image_position;
}

void ImageEditor::second_paint_event(GUI::PaintEvent& event)
{
    if (m_active_tool && m_active_layer)
        m_active_tool->on_second_paint(*m_active_layer, event);
}

GUI::MouseEvent ImageEditor::event_with_pan_and_scale_applied(const GUI::MouseEvent& event) const
{
    auto image_position = editor_position_to_image_position(event.position());
    return {
        static_cast<GUI::Event::Type>(event.type()),
        Gfx::IntPoint(image_position.x(), image_position.y()),
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta()
    };
}

GUI::MouseEvent ImageEditor::event_adjusted_for_layer(const GUI::MouseEvent& event, const Layer& layer) const
{
    auto image_position = editor_position_to_image_position(event.position());
    image_position.move_by(-layer.location().x(), -layer.location().y());
    return {
        static_cast<GUI::Event::Type>(event.type()),
        Gfx::IntPoint(image_position.x(), image_position.y()),
        event.buttons(),
        event.button(),
        event.modifiers(),
        event.wheel_delta()
    };
}

void ImageEditor::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Middle) {
        m_click_position = event.position();
        m_saved_pan_origin = m_pan_origin;
        return;
    }

    if (!m_active_tool)
        return;

    if (m_active_tool->is_move_tool()) {
        if (auto* other_layer = layer_at_editor_position(event.position())) {
            set_active_layer(other_layer);
        }
    }

    if (!m_active_layer)
        return;

    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    auto image_event = event_with_pan_and_scale_applied(event);
    m_active_tool->on_mousedown(*m_active_layer, layer_event, image_event);
}

void ImageEditor::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & GUI::MouseButton::Middle) {
        auto delta = event.position() - m_click_position;
        m_pan_origin = m_saved_pan_origin.translated(
            -delta.x() / m_scale,
            -delta.y() / m_scale);

        relayout();
        return;
    }

    if (!m_active_layer || !m_active_tool)
        return;
    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    auto image_event = event_with_pan_and_scale_applied(event);

    m_active_tool->on_mousemove(*m_active_layer, layer_event, image_event);
}

void ImageEditor::mouseup_event(GUI::MouseEvent& event)
{
    if (!m_active_layer || !m_active_tool)
        return;
    auto layer_event = event_adjusted_for_layer(event, *m_active_layer);
    auto image_event = event_with_pan_and_scale_applied(event);
    m_active_tool->on_mouseup(*m_active_layer, layer_event, image_event);
}

void ImageEditor::mousewheel_event(GUI::MouseEvent& event)
{
    auto old_scale = m_scale;

    m_scale += -event.wheel_delta() * 0.1f;
    if (m_scale < 0.1f)
        m_scale = 0.1f;
    if (m_scale > 100.0f)
        m_scale = 100.0f;

    auto focus_point = Gfx::FloatPoint(
        m_pan_origin.x() - ((float)event.x() - (float)width() / 2.0) / old_scale,
        m_pan_origin.y() - ((float)event.y() - (float)height() / 2.0) / old_scale);

    m_pan_origin = Gfx::FloatPoint(
        focus_point.x() - m_scale / old_scale * (focus_point.x() - m_pan_origin.x()),
        focus_point.y() - m_scale / old_scale * (focus_point.y() - m_pan_origin.y()));

    if (old_scale != m_scale)
        relayout();
}

void ImageEditor::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_active_layer || !m_active_tool)
        return;
    m_active_tool->on_context_menu(*m_active_layer, event);
}

void ImageEditor::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Frame::resize_event(event);
}

void ImageEditor::keydown_event(GUI::KeyEvent& event)
{
    if (m_active_tool)
        m_active_tool->on_keydown(event);
}

void ImageEditor::keyup_event(GUI::KeyEvent& event)
{
    if (m_active_tool)
        m_active_tool->on_keyup(event);
}

void ImageEditor::set_active_layer(Layer* layer)
{
    if (m_active_layer == layer)
        return;
    m_active_layer = layer;

    if (m_active_layer) {
        size_t index = 0;
        for (; index < m_image->layer_count(); ++index) {
            if (&m_image->layer(index) == layer)
                break;
        }
        if (on_active_layer_change)
            on_active_layer_change(layer);
    } else {
        if (on_active_layer_change)
            on_active_layer_change({});
    }

    layers_did_change();
}

void ImageEditor::set_active_tool(Tool* tool)
{
    if (m_active_tool == tool)
        return;

    if (m_active_tool)
        m_active_tool->clear();

    m_active_tool = tool;

    if (m_active_tool)
        m_active_tool->setup(*this);
}

void ImageEditor::layers_did_change()
{
    update();
}

Color ImageEditor::color_for(GUI::MouseButton button) const
{
    if (button == GUI::MouseButton::Left)
        return m_primary_color;
    if (button == GUI::MouseButton::Right)
        return m_secondary_color;
    ASSERT_NOT_REACHED();
}

Color ImageEditor::color_for(const GUI::MouseEvent& event) const
{
    if (event.buttons() & GUI::MouseButton::Left)
        return m_primary_color;
    if (event.buttons() & GUI::MouseButton::Right)
        return m_secondary_color;
    ASSERT_NOT_REACHED();
}

void ImageEditor::set_primary_color(Color color)
{
    if (m_primary_color == color)
        return;
    m_primary_color = color;
    if (on_primary_color_change)
        on_primary_color_change(color);
}

void ImageEditor::set_secondary_color(Color color)
{
    if (m_secondary_color == color)
        return;
    m_secondary_color = color;
    if (on_secondary_color_change)
        on_secondary_color_change(color);
}

Layer* ImageEditor::layer_at_editor_position(const Gfx::IntPoint& editor_position)
{
    if (!m_image)
        return nullptr;
    auto image_position = editor_position_to_image_position(editor_position);
    for (ssize_t i = m_image->layer_count() - 1; i >= 0; --i) {
        auto& layer = m_image->layer(i);
        if (!layer.is_visible())
            continue;
        if (layer.relative_rect().contains(Gfx::IntPoint(image_position.x(), image_position.y())))
            return const_cast<Layer*>(&layer);
    }
    return nullptr;
}

void ImageEditor::relayout()
{
    if (!image())
        return;
    auto& image = *this->image();

    Gfx::IntSize new_size;
    new_size.set_width(image.size().width() * m_scale);
    new_size.set_height(image.size().height() * m_scale);
    m_editor_image_rect.set_size(new_size);

    Gfx::IntPoint new_location;
    new_location.set_x((width() / 2) - (new_size.width() / 2) - (m_pan_origin.x() * m_scale));
    new_location.set_y((height() / 2) - (new_size.height() / 2) - (m_pan_origin.y() * m_scale));
    m_editor_image_rect.set_location(new_location);

    update();
}

void ImageEditor::image_did_change()
{
    update();
}

}
