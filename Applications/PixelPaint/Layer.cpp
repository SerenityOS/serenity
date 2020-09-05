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

#include "Layer.h"
#include "Image.h"
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

RefPtr<Layer> Layer::create_with_size(Image& image, const Gfx::IntSize& size, const String& name)
{
    if (size.is_empty())
        return nullptr;

    if (size.width() > 16384 || size.height() > 16384)
        return nullptr;

    return adopt(*new Layer(image, size, name));
}

RefPtr<Layer> Layer::create_with_bitmap(Image& image, const Gfx::Bitmap& bitmap, const String& name)
{
    if (bitmap.size().is_empty())
        return nullptr;

    if (bitmap.size().width() > 16384 || bitmap.size().height() > 16384)
        return nullptr;

    return adopt(*new Layer(image, bitmap, name));
}

Layer::Layer(Image& image, const Gfx::IntSize& size, const String& name)
    : m_image(image)
    , m_name(name)
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, size);
}

Layer::Layer(Image& image, const Gfx::Bitmap& bitmap, const String& name)
    : m_image(image)
    , m_name(name)
    , m_bitmap(bitmap)
{
}

void Layer::did_modify_bitmap(Image& image)
{
    image.layer_did_modify_bitmap({}, *this);
}

void Layer::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    m_image.layer_did_modify_properties({}, *this);
}

void Layer::set_opacity_percent(int opacity_percent)
{
    if (m_opacity_percent == opacity_percent)
        return;
    m_opacity_percent = opacity_percent;
    m_image.layer_did_modify_properties({}, *this);
}

void Layer::set_name(const String& name)
{
    if (m_name == name)
        return;
    m_name = name;
    m_image.layer_did_modify_properties({}, *this);
}

}
