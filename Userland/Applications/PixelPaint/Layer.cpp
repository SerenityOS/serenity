/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Layer.h"
#include "Image.h"
#include "Selection.h"
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

RefPtr<Layer> Layer::try_create_with_size(Image& image, Gfx::IntSize const& size, String name)
{
    if (size.is_empty())
        return nullptr;

    if (size.width() > 16384 || size.height() > 16384)
        return nullptr;

    auto bitmap = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size);
    if (!bitmap)
        return nullptr;

    return adopt_ref(*new Layer(image, *bitmap, move(name)));
}

RefPtr<Layer> Layer::try_create_with_bitmap(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, String name)
{
    if (bitmap->size().is_empty())
        return nullptr;

    if (bitmap->size().width() > 16384 || bitmap->size().height() > 16384)
        return nullptr;

    return adopt_ref(*new Layer(image, bitmap, move(name)));
}

RefPtr<Layer> Layer::try_create_snapshot(Image& image, Layer const& layer)
{
    auto snapshot = try_create_with_bitmap(image, *layer.bitmap().clone(), layer.name());
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

Layer::Layer(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, String name)
    : m_image(image)
    , m_name(move(name))
    , m_bitmap(move(bitmap))
{
}

void Layer::did_modify_bitmap(Gfx::IntRect const& rect)
{
    m_image.layer_did_modify_bitmap({}, *this, rect);
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

void Layer::set_name(String name)
{
    if (m_name == name)
        return;
    m_name = move(name);
    m_image.layer_did_modify_properties({}, *this);
}

RefPtr<Gfx::Bitmap> Layer::try_copy_bitmap(Selection const& selection) const
{
    if (selection.is_empty()) {
        return {};
    }
    auto selection_rect = selection.bounding_rect();

    auto result = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, selection_rect.size());
    VERIFY(result->has_alpha_channel());

    for (int y = selection_rect.top(); y <= selection_rect.bottom(); y++) {
        for (int x = selection_rect.left(); x <= selection_rect.right(); x++) {

            Gfx::IntPoint image_point { x, y };
            auto layer_point = image_point - m_location;
            auto result_point = image_point - selection_rect.top_left();

            if (!m_bitmap->physical_rect().contains(layer_point)) {
                result->set_pixel(result_point, Gfx::Color::Transparent);
                continue;
            }

            auto pixel = m_bitmap->get_pixel(layer_point);

            // Widen to int before multiplying to avoid overflow issues
            auto pixel_alpha = static_cast<int>(pixel.alpha());
            auto selection_alpha = static_cast<int>(selection.get_selection_alpha(image_point));
            auto new_alpha = (pixel_alpha * selection_alpha) / 0xFF;
            pixel.set_alpha(static_cast<u8>(clamp(new_alpha, 0, 0xFF)));

            result->set_pixel(result_point, pixel);
        }
    }

    return result;
}

}
