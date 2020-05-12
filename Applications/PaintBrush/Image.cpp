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

#include "Image.h"
#include "Layer.h"
#include "LayerModel.h"
#include <LibGUI/Painter.h>

//#define PAINT_DEBUG

namespace PaintBrush {

RefPtr<Image> Image::create_with_size(const Gfx::Size& size)
{
    if (size.is_empty())
        return nullptr;

    if (size.width() > 16384 || size.height() > 16384)
        return nullptr;

    return adopt(*new Image(size));
}

Image::Image(const Gfx::Size& size)
    : m_size(size)
{
}

void Image::paint_into(GUI::Painter& painter, const Gfx::Rect& dest_rect, const Gfx::Rect& src_rect)
{
    for (auto& layer : m_layers) {
        auto target = dest_rect.translated(layer.location());
#ifdef IMAGE_DEBUG
        dbg() << "Composite layer " << layer.name() << " target: " << target << ", src_rect: " << src_rect;
#endif
        painter.draw_scaled_bitmap(target, layer.bitmap(), src_rect);
    }
}

void Image::add_layer(NonnullRefPtr<Layer> layer)
{
    for (auto& existing_layer : m_layers) {
        ASSERT(&existing_layer != layer.ptr());
    }
    m_layers.append(move(layer));
}

GUI::Model& Image::layer_model()
{
    if (!m_layer_model)
        m_layer_model = LayerModel::create(*this);
    return *m_layer_model;
}

}
