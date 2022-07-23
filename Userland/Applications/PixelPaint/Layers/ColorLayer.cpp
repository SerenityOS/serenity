/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 *
 */

#include "ColorLayer.h"

namespace PixelPaint {

ColorLayer::ColorLayer(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, Gfx::Color color, String name, NonnullRefPtr<Gfx::Bitmap> cached_content_bitmap)
    : Layer(LayerType::ColorLayer, image, move(name), move(cached_content_bitmap))
    , m_bitmap(move(bitmap))
    , m_color(color)
{
    set_color(color);
    update_cached_bitmap();
}

ErrorOr<NonnullRefPtr<ColorLayer>> ColorLayer::try_create_with_size(Image& image, Gfx::IntSize const& size, String name)
{
    auto cached_content_bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    auto bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    return adopt_nonnull_ref_or_enomem(new (nothrow) ColorLayer(image, move(bitmap), Gfx::Color::White, move(name), move(cached_content_bitmap)));
}

ErrorOr<NonnullRefPtr<ColorLayer>> ColorLayer::try_create_with_size_and_color(Image& image, Gfx::IntSize const& size, Gfx::Color color, String name)
{
    auto cached_content_bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    auto bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    return adopt_nonnull_ref_or_enomem(new (nothrow) ColorLayer(image, move(bitmap), color, move(name), move(cached_content_bitmap)));
}

bool ColorLayer::is_current_bitmap_editable()
{
    if (edit_mode() == EditMode::Mask)
        return true;
    return false;
}

void ColorLayer::set_color(Gfx::Color color)
{
    m_color = color;
    m_bitmap->fill(color);
    update_cached_bitmap();
}

}
