/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Layer.h"
#include "Image.h"
#include "Selection.h"
#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>

namespace PixelPaint {

Layer::Layer(LayerType type, Image& image, String name, NonnullRefPtr<Gfx::Bitmap> cached_display_bitmap)
    : m_image(image)
    , m_cached_display_bitmap(move(cached_display_bitmap))
    , m_name(move(name))
    , m_layer_type(type)
{
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

    auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, selection_rect.size());
    if (bitmap_or_error.is_error())
        return nullptr;
    auto result = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    VERIFY(result->has_alpha_channel());

    auto const& bitmap = content_bitmap();

    for (int y = selection_rect.top(); y <= selection_rect.bottom(); y++) {
        for (int x = selection_rect.left(); x <= selection_rect.right(); x++) {

            Gfx::IntPoint image_point { x, y };
            auto layer_point = image_point - m_location;
            auto result_point = image_point - selection_rect.top_left();

            if (!bitmap.physical_rect().contains(layer_point)) {
                result->set_pixel(result_point, Gfx::Color::Transparent);
                continue;
            }

            auto pixel = bitmap.get_pixel(layer_point);

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

void Layer::erase_selection(Selection const& selection)
{
    Gfx::Painter painter { content_bitmap() };
    auto const image_and_selection_intersection = m_image.rect().intersected(selection.bounding_rect());
    auto const translated_to_layer_space = image_and_selection_intersection.translated(-location());
    painter.clear_rect(translated_to_layer_space, Color::Transparent);
}

void Layer::resize(Gfx::IntRect const& new_rect, Gfx::Painter::ScalingMode scaling_mode)
{
    resize(new_rect.size(), new_rect.location(), scaling_mode);
}

void Layer::resize(Gfx::IntSize const& new_size, Gfx::Painter::ScalingMode scaling_mode)
{
    resize(new_size, location(), scaling_mode);
}

void Layer::create_mask()
{
    m_mask_bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, size()));
    m_mask_bitmap->fill(Gfx::Color::White);
    update_cached_bitmap();
}

Gfx::Bitmap& Layer::currently_edited_bitmap()
{
    switch (edit_mode()) {
    case EditMode::Mask:
        if (is_masked())
            return *mask_bitmap();
        [[fallthrough]];
    case EditMode::Content:
        return content_bitmap();
    }
    VERIFY_NOT_REACHED();
}

void Layer::set_edit_mode(Layer::EditMode mode)
{
    if (m_edit_mode == mode)
        return;

    m_edit_mode = mode;
}

void Layer::did_modify(Gfx::IntRect const& rect)
{
    m_image.layer_did_modify_bitmap({}, *this, rect);
    update_cached_bitmap();
}

void Layer::update_cached_bitmap()
{
    auto& bitmap_content = content_bitmap();

    if (!is_masked()) {
        if (bitmap_content == m_cached_display_bitmap)
            return;
        m_cached_display_bitmap = bitmap_content;
        return;
    }

    if (m_cached_display_bitmap == bitmap_content || m_cached_display_bitmap->size() != size()) {
        m_cached_display_bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size()));
    }

    // FIXME: This can probably be done nicer
    m_cached_display_bitmap->fill(Color::Transparent);
    for (int y = 0; y < size().height(); ++y) {
        for (int x = 0; x < size().width(); ++x) {
            auto opacity_multiplier = (float)m_mask_bitmap->get_pixel(x, y).to_grayscale().red() / 255;
            auto content_color = bitmap_content.get_pixel(x, y);
            content_color.set_alpha(content_color.alpha() * opacity_multiplier);
            m_cached_display_bitmap->set_pixel(x, y, content_color);
        }
    }
}

}
