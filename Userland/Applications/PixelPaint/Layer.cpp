/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    return adopt_ref(*new Layer(image, size, name));
}

RefPtr<Layer> Layer::create_with_bitmap(Image& image, const Gfx::Bitmap& bitmap, const String& name)
{
    if (bitmap.size().is_empty())
        return nullptr;

    if (bitmap.size().width() > 16384 || bitmap.size().height() > 16384)
        return nullptr;

    return adopt_ref(*new Layer(image, bitmap, name));
}

RefPtr<Layer> Layer::create_snapshot(Image& image, const Layer& layer)
{
    auto snapshot = create_with_bitmap(image, *layer.bitmap().clone(), layer.name());
    /*
        We set these properties directly because calling the setters might 
        notify the image of an update on the newly created layer, but this 
        layer has not yet been added to the image.
    */
    snapshot->m_opacity_percent = layer.opacity_percent();
    snapshot->m_visible = layer.is_visible();

    snapshot->set_selected(layer.is_selected());
    snapshot->set_location(layer.location());

    return snapshot;
}

Layer::Layer(Image& image, const Gfx::IntSize& size, const String& name)
    : m_image(image)
    , m_name(name)
{
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size);
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
