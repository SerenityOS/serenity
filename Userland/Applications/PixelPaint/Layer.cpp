/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
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

ErrorOr<NonnullRefPtr<Layer>> Layer::try_create_with_size(Image& image, Gfx::IntSize size, DeprecatedString name)
{
    VERIFY(!size.is_empty());

    if (size.width() > 16384 || size.height() > 16384)
        return Error::from_string_literal("Layer size too large");

    auto bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size));
    return adopt_nonnull_ref_or_enomem(new (nothrow) Layer(image, move(bitmap), move(name)));
}

ErrorOr<NonnullRefPtr<Layer>> Layer::try_create_with_bitmap(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, DeprecatedString name)
{
    VERIFY(!bitmap->size().is_empty());

    if (bitmap->size().width() > 16384 || bitmap->size().height() > 16384)
        return Error::from_string_literal("Layer size too large");

    return adopt_nonnull_ref_or_enomem(new (nothrow) Layer(image, bitmap, move(name)));
}

ErrorOr<NonnullRefPtr<Layer>> Layer::try_create_snapshot(Image& image, Layer const& layer)
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

Layer::Layer(Image& image, NonnullRefPtr<Gfx::Bitmap> bitmap, DeprecatedString name)
    : m_image(image)
    , m_name(move(name))
    , m_content_bitmap(move(bitmap))
    , m_cached_display_bitmap(m_content_bitmap)
{
}

void Layer::did_modify_bitmap(Gfx::IntRect const& rect, NotifyClients notify_clients)
{
    if (!m_scratch_edited_bitmap.is_null()) {
        for (int y = 0; y < rect.height(); ++y) {
            for (int x = 0; x < rect.width(); ++x) {
                Gfx::IntPoint next_point = { rect.left() + x, rect.top() + y };
                if (!m_scratch_edited_bitmap->rect().contains(next_point))
                    continue;

                if (this->image().selection().is_selected(next_point.translated(this->location())))
                    currently_edited_bitmap().set_pixel(next_point, m_scratch_edited_bitmap->get_pixel(next_point));
                else
                    m_scratch_edited_bitmap->set_pixel(next_point, currently_edited_bitmap().get_pixel(next_point));
            }
        }
    }

    // NOTE: If NotifyClients::NO is passed to this function the caller should handle notifying
    //       the clients of any bitmap changes.
    if (notify_clients == NotifyClients::Yes)
        m_image.layer_did_modify_bitmap({}, *this, rect);
    update_cached_bitmap();
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

void Layer::set_name(DeprecatedString name)
{
    if (m_name == name)
        return;
    m_name = move(name);
    m_image.layer_did_modify_properties({}, *this);
}

Gfx::Bitmap& Layer::get_scratch_edited_bitmap()
{
    if (this->image().selection().is_empty()) {
        m_scratch_edited_bitmap = nullptr;
        return currently_edited_bitmap();
    }

    if (!m_scratch_edited_bitmap.is_null())
        return *m_scratch_edited_bitmap;

    m_scratch_edited_bitmap = MUST(currently_edited_bitmap().clone());

    return *m_scratch_edited_bitmap;
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

    for (int y = selection_rect.top(); y <= selection_rect.bottom(); y++) {
        for (int x = selection_rect.left(); x <= selection_rect.right(); x++) {

            Gfx::IntPoint image_point { x, y };
            auto layer_point = image_point - m_location;
            auto result_point = image_point - selection_rect.top_left();

            if (!m_content_bitmap->physical_rect().contains(layer_point)) {
                result->set_pixel(result_point, Gfx::Color::Transparent);
                continue;
            }

            auto pixel = m_content_bitmap->get_pixel(layer_point);

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
    auto const image_and_selection_intersection = m_image.rect().intersected(selection.bounding_rect());
    auto const translated_to_layer_space = image_and_selection_intersection.translated(-location());

    for (int y = translated_to_layer_space.top(); y < translated_to_layer_space.top() + translated_to_layer_space.height(); ++y) {
        for (int x = translated_to_layer_space.left(); x < translated_to_layer_space.left() + translated_to_layer_space.width(); ++x) {

            // Selection is still in pre-translated coordinates, account for this by adding the layer's relative location
            if (content_bitmap().rect().contains(x, y) && selection.is_selected(x + location().x(), y + location().y())) {
                content_bitmap().set_pixel(x, y, Color::Transparent);
            }
        }
    }

    did_modify_bitmap(translated_to_layer_space);
}

ErrorOr<void> Layer::try_set_bitmaps(NonnullRefPtr<Gfx::Bitmap> content, RefPtr<Gfx::Bitmap> mask)
{
    if (mask && content->size() != mask->size())
        return Error::from_string_literal("Layer content and mask must be same size");

    m_content_bitmap = move(content);
    m_mask_bitmap = move(mask);
    m_scratch_edited_bitmap = nullptr;
    update_cached_bitmap();
    return {};
}

ErrorOr<void> Layer::flip(Gfx::Orientation orientation, NotifyClients notify_clients)
{
    auto flipped_content_bitmap = TRY(m_content_bitmap->flipped(orientation));
    if (m_mask_bitmap)
        m_mask_bitmap = *TRY(m_mask_bitmap->flipped(orientation));

    m_content_bitmap = move(flipped_content_bitmap);
    did_modify_bitmap({}, notify_clients);

    return {};
}

ErrorOr<void> Layer::rotate(Gfx::RotationDirection direction, NotifyClients notify_clients)
{
    auto rotated_content_bitmap = TRY(m_content_bitmap->rotated(direction));
    if (m_mask_bitmap)
        m_mask_bitmap = *TRY(m_mask_bitmap->rotated(direction));

    m_content_bitmap = move(rotated_content_bitmap);
    did_modify_bitmap({}, notify_clients);

    return {};
}

ErrorOr<void> Layer::crop(Gfx::IntRect const& rect, NotifyClients notify_clients)
{
    auto cropped_content_bitmap = TRY(m_content_bitmap->cropped(rect));
    if (m_mask_bitmap)
        m_mask_bitmap = *TRY(m_mask_bitmap->cropped(rect));

    m_content_bitmap = move(cropped_content_bitmap);
    did_modify_bitmap({}, notify_clients);

    return {};
}

ErrorOr<void> Layer::resize(Gfx::IntSize new_size, Gfx::IntPoint new_location, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients)
{
    auto src_rect = Gfx::IntRect(Gfx::IntPoint(0, 0), size());
    auto dst_rect = Gfx::IntRect(Gfx::IntPoint(0, 0), new_size);

    auto resized_content_bitmap = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, new_size));
    {
        Gfx::Painter painter(resized_content_bitmap);

        if (scaling_mode == Gfx::Painter::ScalingMode::None) {
            painter.blit(src_rect.top_left(), *m_content_bitmap, src_rect, 1.0f);
        } else {
            painter.draw_scaled_bitmap(dst_rect, *m_content_bitmap, src_rect, 1.0f, scaling_mode);
        }
    }

    if (m_mask_bitmap) {
        auto dst = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, new_size));
        Gfx::Painter painter(dst);

        if (scaling_mode == Gfx::Painter::ScalingMode::None) {
            painter.blit(src_rect.top_left(), *m_content_bitmap, src_rect, 1.0f);
        } else {
            painter.draw_scaled_bitmap(dst_rect, *m_mask_bitmap, src_rect, 1.0f, scaling_mode);
        }

        m_mask_bitmap = move(dst);
    }

    m_content_bitmap = move(resized_content_bitmap);

    set_location(new_location);
    did_modify_bitmap({}, notify_clients);

    return {};
}

ErrorOr<void> Layer::resize(Gfx::IntRect const& new_rect, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients)
{
    return resize(new_rect.size(), new_rect.location(), scaling_mode, notify_clients);
}

ErrorOr<void> Layer::resize(Gfx::IntSize new_size, Gfx::Painter::ScalingMode scaling_mode, NotifyClients notify_clients)
{
    return resize(new_size, location(), scaling_mode, notify_clients);
}

void Layer::update_cached_bitmap()
{
    if (!is_masked()) {
        if (m_content_bitmap.ptr() == m_cached_display_bitmap.ptr())
            return;
        m_cached_display_bitmap = m_content_bitmap;
        return;
    }

    if (m_cached_display_bitmap.ptr() == m_content_bitmap.ptr() || m_cached_display_bitmap->size() != size()) {
        m_cached_display_bitmap = MUST(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, size()));
    }

    // FIXME: This can probably be done nicer
    m_cached_display_bitmap->fill(Color::Transparent);
    for (int y = 0; y < size().height(); ++y) {
        for (int x = 0; x < size().width(); ++x) {
            auto opacity_multiplier = (float)m_mask_bitmap->get_pixel(x, y).to_grayscale().red() / 255;
            auto content_color = m_content_bitmap->get_pixel(x, y);
            content_color.set_alpha(content_color.alpha() * opacity_multiplier);
            m_cached_display_bitmap->set_pixel(x, y, content_color);
        }
    }
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
    m_scratch_edited_bitmap = nullptr;
    m_edit_mode = mode;
}

Optional<Gfx::IntRect> Layer::nonempty_content_bounding_rect() const
{
    Optional<int> min_content_y;
    Optional<int> min_content_x;
    Optional<int> max_content_y;
    Optional<int> max_content_x;

    for (int y = 0; y < m_content_bitmap->height(); ++y) {
        for (int x = 0; x < m_content_bitmap->width(); ++x) {
            auto color = m_content_bitmap->get_pixel(x, y);
            if (color.alpha() == 0)
                continue;
            min_content_x = min(min_content_x.value_or(x), x);
            min_content_y = min(min_content_y.value_or(y), y);
            max_content_x = max(max_content_x.value_or(x), x);
            max_content_y = max(max_content_y.value_or(y), y);
        }
    }

    if (!min_content_x.has_value())
        return {};

    return Gfx::IntRect {
        *min_content_x,
        *min_content_y,
        *max_content_x - *min_content_x + 1,
        *max_content_y - *min_content_y + 1
    };
}

}
