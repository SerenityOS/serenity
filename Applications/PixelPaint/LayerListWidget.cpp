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

#include "LayerListWidget.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

namespace PixelPaint {

LayerListWidget::LayerListWidget()
{
}

LayerListWidget::~LayerListWidget()
{
    if (m_image)
        m_image->remove_client(*this);
}

void LayerListWidget::set_image(Image* image)
{
    if (m_image == image)
        return;
    if (m_image)
        m_image->remove_client(*this);
    m_image = image;
    if (m_image)
        m_image->add_client(*this);

    rebuild_gadgets();
}

void LayerListWidget::rebuild_gadgets()
{
    m_gadgets.clear();
    if (m_image) {
        for (size_t layer_index = 0; layer_index < m_image->layer_count(); ++layer_index) {
            m_gadgets.append({ layer_index, {}, {}, false, {} });
        }
    }
    relayout_gadgets();
}

void LayerListWidget::resize_event(GUI::ResizeEvent& event)
{
    Widget::resize_event(event);
    relayout_gadgets();
}

void LayerListWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), palette().button());

    if (!m_image)
        return;

    painter.fill_rect(event.rect(), palette().button());

    auto paint_gadget = [&](auto& gadget) {
        auto& layer = m_image->layer(gadget.layer_index);

        auto adjusted_rect = gadget.rect;

        if (gadget.is_moving) {
            adjusted_rect.move_by(0, gadget.movement_delta.y());
        }

        if (gadget.is_moving) {
            painter.fill_rect(adjusted_rect, palette().selection().lightened(1.5f));
        } else if (layer.is_selected()) {
            painter.fill_rect(adjusted_rect, palette().selection());
        }

        painter.draw_rect(adjusted_rect, Color::Black);

        Gfx::IntRect thumbnail_rect { adjusted_rect.x(), adjusted_rect.y(), adjusted_rect.height(), adjusted_rect.height() };
        thumbnail_rect.shrink(8, 8);
        painter.draw_scaled_bitmap(thumbnail_rect, layer.bitmap(), layer.bitmap().rect());

        Gfx::IntRect text_rect { thumbnail_rect.right() + 10, adjusted_rect.y(), adjusted_rect.width(), adjusted_rect.height() };
        text_rect.intersect(adjusted_rect);

        painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft, layer.is_selected() ? palette().selection_text() : palette().button_text());
    };

    for (auto& gadget : m_gadgets) {
        if (!gadget.is_moving)
            paint_gadget(gadget);
    }

    if (m_moving_gadget_index.has_value())
        paint_gadget(m_gadgets[m_moving_gadget_index.value()]);
}

Optional<size_t> LayerListWidget::gadget_at(const Gfx::IntPoint& position)
{
    for (size_t i = 0; i < m_gadgets.size(); ++i) {
        if (m_gadgets[i].rect.contains(position))
            return i;
    }
    return {};
}

void LayerListWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Left)
        return;
    auto gadget_index = gadget_at(event.position());
    if (!gadget_index.has_value()) {
        if (on_layer_select)
            on_layer_select(nullptr);
        return;
    }
    m_moving_gadget_index = gadget_index;
    m_moving_event_origin = event.position();
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    auto& layer = m_image->layer(gadget_index.value());
    set_selected_layer(&layer);
    gadget.is_moving = true;
    gadget.movement_delta = {};
    update();
}

void LayerListWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (!m_moving_gadget_index.has_value())
        return;

    auto delta = event.position() - m_moving_event_origin;
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    ASSERT(gadget.is_moving);
    gadget.movement_delta = delta;
    relayout_gadgets();
}

void LayerListWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Left)
        return;
    if (!m_moving_gadget_index.has_value())
        return;

    size_t old_index = m_moving_gadget_index.value();
    size_t new_index = hole_index_during_move();
    if (new_index >= m_image->layer_count())
        new_index = m_image->layer_count() - 1;

    m_moving_gadget_index = {};
    m_image->change_layer_index(old_index, new_index);
}

void LayerListWidget::image_did_add_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    Gadget gadget { layer_index, {}, {}, false, {} };
    m_gadgets.insert(layer_index, move(gadget));
    relayout_gadgets();
}

void LayerListWidget::image_did_remove_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    m_gadgets.remove(layer_index);
    relayout_gadgets();
}

void LayerListWidget::image_did_modify_layer(size_t layer_index)
{
    update(m_gadgets[layer_index].rect);
}

void LayerListWidget::image_did_modify_layer_stack()
{
    rebuild_gadgets();
}

static constexpr int gadget_height = 30;
static constexpr int gadget_spacing = 1;
static constexpr int vertical_step = gadget_height + gadget_spacing;

size_t LayerListWidget::hole_index_during_move() const
{
    ASSERT(is_moving_gadget());
    auto& moving_gadget = m_gadgets[m_moving_gadget_index.value()];
    int center_y_of_moving_gadget = moving_gadget.rect.translated(0, moving_gadget.movement_delta.y()).center().y();
    return center_y_of_moving_gadget / vertical_step;
}

void LayerListWidget::select_bottom_layer()
{
    if (!m_image || !m_image->layer_count())
        return;
    set_selected_layer(&m_image->layer(0));
}

void LayerListWidget::select_top_layer()
{
    if (!m_image || !m_image->layer_count())
        return;
    set_selected_layer(&m_image->layer(m_image->layer_count() - 1));
}

void LayerListWidget::move_selection(int delta)
{
    if (!m_image || !m_image->layer_count())
        return;
    int new_layer_index = min(max(0, (int)m_image->layer_count() + delta), (int)m_image->layer_count() - 1);
    set_selected_layer(&m_image->layer(new_layer_index));
}

void LayerListWidget::relayout_gadgets()
{
    int y = 0;

    Optional<size_t> hole_index;
    if (is_moving_gadget())
        hole_index = hole_index_during_move();

    size_t index = 0;
    for (auto& gadget : m_gadgets) {
        if (gadget.is_moving)
            continue;
        if (hole_index.has_value() && index == hole_index.value())
            y += vertical_step;
        gadget.rect = { 0, y, width(), gadget_height };
        y += vertical_step;
        ++index;
    }

    update();
}

void LayerListWidget::set_selected_layer(Layer* layer)
{
    if (!m_image)
        return;
    for (size_t i = 0; i < m_image->layer_count(); ++i)
        m_image->layer(i).set_selected(layer == &m_image->layer(i));
    if (on_layer_select)
        on_layer_select(layer);
    update();
}

}
