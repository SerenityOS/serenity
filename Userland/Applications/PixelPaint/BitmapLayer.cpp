/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 *
 */

#include "BitmapLayer.h"
#include "Image.h"
#include "Selection.h"

namespace PixelPaint {

void BitmapLayer::did_modify(Gfx::IntRect const& rect)
{
    Layer::did_modify(rect);
    update_cached_bitmap();
}

BitmapLayer::BitmapLayer(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, String name, NonnullRefPtr<Gfx::Bitmap> cached_display_bitmap)
    : Layer(LayerType::BitmapLayer, image, move(name), move(cached_display_bitmap))
    , m_content_bitmap(move(bitmap))
{
}

ErrorOr<NonnullRefPtr<BitmapLayer>> BitmapLayer::try_create_with_size(Image& image, Gfx::IntSize const& size, String name)
{
    VERIFY(!size.is_empty());

    if (size.width() > 16384 || size.height() > 16384)
        return Error::from_string_literal("Layer size too large");

    auto bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapLayer(image, bitmap, move(name), bitmap));
}

ErrorOr<NonnullRefPtr<BitmapLayer>> BitmapLayer::try_create_with_bitmap(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, String name)
{
    VERIFY(!bitmap->size().is_empty());

    if (bitmap->size().width() > 16384 || bitmap->size().height() > 16384)
        return Error::from_string_literal("Layer size too large");

    return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapLayer(image, bitmap, move(name), bitmap));
}

ErrorOr<NonnullRefPtr<BitmapLayer>> BitmapLayer::try_create_snapshot(Image& image, Layer const& layer)
{
    auto bitmap = TRY(layer.content_bitmap().clone());
    auto snapshot = TRY(try_create_with_bitmap(image, move(bitmap), layer.name()));

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

ErrorOr<void> BitmapLayer::try_set_bitmaps(NonnullRefPtr<Gfx::Bitmap> content, RefPtr<Gfx::Bitmap> mask)
{
    if (mask && content->size() != mask->size())
        return Error::from_string_literal("Layer content and mask must be same size");

    m_content_bitmap = move(content);
    m_mask_bitmap = move(mask);
    update_cached_bitmap();
    return {};
}

void BitmapLayer::flip(Gfx::Orientation orientation)
{
    m_content_bitmap = *m_content_bitmap->flipped(orientation).release_value_but_fixme_should_propagate_errors();
    if (m_mask_bitmap)
        m_mask_bitmap = *m_mask_bitmap->flipped(orientation).release_value_but_fixme_should_propagate_errors();

    did_modify();
}

void BitmapLayer::rotate(Gfx::RotationDirection direction)
{
    m_content_bitmap = *m_content_bitmap->rotated(direction).release_value_but_fixme_should_propagate_errors();
    if (m_mask_bitmap)
        m_mask_bitmap = *m_mask_bitmap->rotated(direction).release_value_but_fixme_should_propagate_errors();

    did_modify();
}

void BitmapLayer::crop(Gfx::IntRect const& rect)
{
    m_content_bitmap = *m_content_bitmap->cropped(rect).release_value_but_fixme_should_propagate_errors();
    if (m_mask_bitmap)
        m_mask_bitmap = *m_mask_bitmap->cropped(rect).release_value_but_fixme_should_propagate_errors();

    did_modify();
}

void BitmapLayer::resize(Gfx::IntSize const& new_size, Gfx::IntPoint const& new_location, Gfx::Painter::ScalingMode scaling_mode)
{
    auto src_rect = Gfx::IntRect(Gfx::IntPoint(0, 0), size());
    auto dst_rect = Gfx::IntRect(Gfx::IntPoint(0, 0), new_size);

    {
        auto dst = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, new_size).release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(dst);

        painter.draw_scaled_bitmap(dst_rect, *m_content_bitmap, src_rect, 1.0f, scaling_mode);

        m_content_bitmap = move(dst);
    }

    if (m_mask_bitmap) {
        auto dst = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, new_size).release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(dst);

        painter.draw_scaled_bitmap(dst_rect, *m_mask_bitmap, src_rect, 1.0f, scaling_mode);
        m_mask_bitmap = move(dst);
    }

    set_location(new_location);
    did_modify();
}

void BitmapLayer::erase_selection(Selection const& selection)
{
    Layer::erase_selection(selection);
    auto const image_and_selection_intersection = m_image.rect().intersected(selection.bounding_rect());
    auto const translated_to_layer_space = image_and_selection_intersection.translated(-location());
    did_modify(translated_to_layer_space);
}

}
