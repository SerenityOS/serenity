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
#include "Layer.h"
#include <LibGUI/Model.h>
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

    m_gadgets.clear();

    if (m_image) {
        for (size_t layer_index = 0; layer_index < m_image->layer_count(); ++layer_index) {
            m_gadgets.append({ layer_index, {} });
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

    for (auto& gadget : m_gadgets) {
        painter.draw_rect(gadget.rect, Color::Black);
        auto& layer = m_image->layer(gadget.layer_index);

        Gfx::Rect thumbnail_rect { gadget.rect.x(), gadget.rect.y(), gadget.rect.height(), gadget.rect.height() };
        thumbnail_rect.shrink(8, 8);
        painter.draw_scaled_bitmap(thumbnail_rect, layer.bitmap(), layer.bitmap().rect());

        Gfx::Rect text_rect { thumbnail_rect.right() + 10, gadget.rect.y(), gadget.rect.width(), gadget.rect.height() };
        text_rect.intersect(gadget.rect);

        painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft);
    }
}

void LayerListWidget::mousedown_event(GUI::MouseEvent& event)
{
    (void)event;
}

void LayerListWidget::mousemove_event(GUI::MouseEvent& event)
{
    (void)event;
}

void LayerListWidget::mouseup_event(GUI::MouseEvent& event)
{
    (void)event;
}

void LayerListWidget::image_did_add_layer(size_t layer_index)
{
    Gadget gadget { layer_index, {} };
    m_gadgets.insert(layer_index, move(gadget));
    relayout_gadgets();
}

void LayerListWidget::image_did_remove_layer(size_t layer_index)
{
    m_gadgets.remove(layer_index);
    relayout_gadgets();
}

void LayerListWidget::image_did_modify_layer(size_t layer_index)
{
    update(m_gadgets[layer_index].rect);
}

void LayerListWidget::relayout_gadgets()
{
    constexpr int gadget_height = 30;
    constexpr int gadget_spacing = 1;
    int y = 0;

    for (auto& gadget : m_gadgets) {
        gadget.rect = { 0, y, width(), gadget_height };
        y += gadget_height + gadget_spacing;
    }

    update();
}

}
