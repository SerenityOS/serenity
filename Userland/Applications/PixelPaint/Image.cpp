/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"
#include "Layer.h"
#include "Selection.h"
#include <AK/Base64.h>
#include <AK/JsonObject.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BMPWriter.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/ImageFormats/QOIWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <stdio.h>

namespace PixelPaint {

ErrorOr<NonnullRefPtr<Image>> Image::create_with_size(Gfx::IntSize size)
{
    VERIFY(!size.is_empty());

    if (size.width() > 16384 || size.height() > 16384)
        return Error::from_string_literal("Image size too large");

    return adopt_nonnull_ref_or_enomem(new (nothrow) Image(size));
}

Image::Image(Gfx::IntSize size)
    : m_size(size)
    , m_selection(*this)
{
}

void Image::paint_into(GUI::Painter& painter, Gfx::IntRect const& dest_rect, float scale) const
{
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(dest_rect);
    for (auto const& layer : m_layers) {
        if (!layer->is_visible())
            continue;
        auto target = dest_rect.to_type<float>().translated(layer->location().x() * scale, layer->location().y() * scale);
        target.set_size(layer->size().width() * scale, layer->size().height() * scale);
        painter.draw_scaled_bitmap(target.to_type<int>(), layer->display_bitmap(), layer->rect(), (float)layer->opacity_percent() / 100.0f);
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Image::decode_bitmap(ReadonlyBytes bitmap_data, Optional<StringView> guessed_mime_type)
{
    // Spawn a new ImageDecoder service process and connect to it.
    auto client = TRY(ImageDecoderClient::Client::try_create());
    auto optional_mime_type = guessed_mime_type.map([](auto mime_type) { return mime_type.to_byte_string(); });

    // FIXME: Find a way to avoid the memory copying here.
    // FIXME: Support multi-frame images
    // FIXME: Refactor image decoding to be more async-aware, and don't await this promise
    auto decoded_image = TRY(client->decode_image(bitmap_data, {}, {}, OptionalNone {}, optional_mime_type)->await());

    return decoded_image.frames.first().bitmap;
}

ErrorOr<NonnullRefPtr<Image>> Image::create_from_bitmap(NonnullRefPtr<Gfx::Bitmap> const& bitmap)
{
    auto image = TRY(create_with_size({ bitmap->width(), bitmap->height() }));
    auto layer = TRY(Layer::create_with_bitmap(*image, *bitmap, "Background"));
    image->add_layer(move(layer));
    return image;
}

ErrorOr<NonnullRefPtr<Image>> Image::create_from_pixel_paint_json(JsonObject const& json)
{
    // FIXME: Handle invalid JSON data
    auto image = TRY(create_with_size({ json.get_i32("width"sv).value_or(0), json.get_i32("height"sv).value_or(0) }));

    auto layers_value = json.get_array("layers"sv).value();
    for (auto& layer_value : layers_value.values()) {
        auto const& layer_object = layer_value.as_object();
        auto name = layer_object.get_byte_string("name"sv).value();

        auto bitmap_base64_encoded = layer_object.get_byte_string("bitmap"sv).value();
        auto bitmap_data = TRY(decode_base64(bitmap_base64_encoded));
        auto bitmap = TRY(decode_bitmap(bitmap_data, {}));
        auto layer = TRY(Layer::create_with_bitmap(*image, move(bitmap), name));

        if (auto const& mask_object = layer_object.get_byte_string("mask"sv); mask_object.has_value()) {
            auto mask_base64_encoded = mask_object.value();
            auto mask_data = TRY(decode_base64(mask_base64_encoded));
            auto mask = TRY(decode_bitmap(mask_data, {}));
            TRY(layer->set_bitmaps(layer->content_bitmap(), mask));
        }

        auto width = layer_object.get_i32("width"sv).value_or(0);
        auto height = layer_object.get_i32("height"sv).value_or(0);

        if (width != layer->size().width() || height != layer->size().height())
            return Error::from_string_literal("Decoded layer bitmap has wrong size");

        image->add_layer(*layer);

        layer->set_location({ layer_object.get_i32("locationx"sv).value_or(0), layer_object.get_i32("locationy"sv).value_or(0) });
        layer->set_opacity_percent(layer_object.get_i32("opacity_percent"sv).value());
        layer->set_visible(layer_object.get_bool("visible"sv).value());
        layer->set_selected(layer_object.get_bool("selected"sv).value());
    }

    return image;
}

ErrorOr<void> Image::serialize_as_json(JsonObjectSerializer<StringBuilder>& json) const
{
    TRY(json.add("width"sv, m_size.width()));
    TRY(json.add("height"sv, m_size.height()));
    {
        auto json_layers = TRY(json.add_array("layers"sv));
        for (auto const& layer : m_layers) {
            auto json_layer = TRY(json_layers.add_object());
            TRY(json_layer.add("width"sv, layer->size().width()));
            TRY(json_layer.add("height"sv, layer->size().height()));
            TRY(json_layer.add("name"sv, layer->name()));
            TRY(json_layer.add("locationx"sv, layer->location().x()));
            TRY(json_layer.add("locationy"sv, layer->location().y()));
            TRY(json_layer.add("opacity_percent"sv, layer->opacity_percent()));
            TRY(json_layer.add("visible"sv, layer->is_visible()));
            TRY(json_layer.add("selected"sv, layer->is_selected()));
            TRY(json_layer.add("bitmap"sv, TRY(encode_base64(TRY(Gfx::PNGWriter::encode(layer->content_bitmap()))))));
            if (layer->is_masked())
                TRY(json_layer.add("mask"sv, TRY(encode_base64(TRY(Gfx::PNGWriter::encode(*layer->mask_bitmap()))))));
            TRY(json_layer.finish());
        }

        TRY(json_layers.finish());
    }
    return {};
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Image::compose_bitmap(Gfx::BitmapFormat format) const
{
    auto bitmap = TRY(Gfx::Bitmap::create(format, m_size));
    GUI::Painter painter(bitmap);
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() }, 1.0f);
    return bitmap;
}

RefPtr<Gfx::Bitmap> Image::copy_bitmap(Selection const& selection) const
{
    if (selection.is_empty())
        return {};
    auto selection_rect = selection.bounding_rect();

    // FIXME: Add a way to only compose a certain part of the image
    auto bitmap_or_error = compose_bitmap(Gfx::BitmapFormat::BGRA8888);
    if (bitmap_or_error.is_error())
        return {};
    auto full_bitmap = bitmap_or_error.release_value();

    auto cropped_bitmap_or_error = full_bitmap->cropped(selection_rect);
    if (cropped_bitmap_or_error.is_error())
        return nullptr;
    return cropped_bitmap_or_error.release_value_but_fixme_should_propagate_errors();
}

ErrorOr<void> Image::export_bmp_to_file(NonnullOwnPtr<Stream> stream, bool preserve_alpha_channel) const
{
    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(compose_bitmap(bitmap_format));

    auto encoded_data = TRY(Gfx::BMPWriter::encode(*bitmap));
    TRY(stream->write_until_depleted(encoded_data));
    return {};
}

ErrorOr<void> Image::export_png_to_file(NonnullOwnPtr<Stream> stream, bool preserve_alpha_channel) const
{
    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(compose_bitmap(bitmap_format));

    auto encoded_data = TRY(Gfx::PNGWriter::encode(*bitmap));
    TRY(stream->write_until_depleted(encoded_data));
    return {};
}

ErrorOr<void> Image::export_qoi_to_file(NonnullOwnPtr<Stream> stream) const
{
    auto bitmap = TRY(compose_bitmap(Gfx::BitmapFormat::BGRA8888));

    auto encoded_data = TRY(Gfx::QOIWriter::encode(bitmap));
    TRY(stream->write_until_depleted(encoded_data));
    return {};
}

void Image::insert_layer(NonnullRefPtr<Layer> layer, size_t index)
{
    VERIFY(index <= m_layers.size());

    for (auto& existing_layer : m_layers) {
        VERIFY(existing_layer != layer);
    }

    if (index == m_layers.size())
        m_layers.append(move(layer));
    else
        m_layers.insert(index, move(layer));

    for (auto* client : m_clients)
        client->image_did_add_layer(index);

    did_modify_layer_stack();
}

void Image::add_layer(NonnullRefPtr<Layer> layer)
{
    insert_layer(move(layer), m_layers.size());
}

ErrorOr<NonnullRefPtr<Image>> Image::take_snapshot() const
{
    auto snapshot = TRY(create_with_size(m_size));
    for (auto const& layer : m_layers) {
        auto layer_snapshot = TRY(Layer::create_snapshot(*snapshot, layer));
        snapshot->add_layer(move(layer_snapshot));
    }
    snapshot->m_selection.set_mask(m_selection.mask());
    return snapshot;
}

ErrorOr<void> Image::restore_snapshot(Image const& snapshot)
{
    m_layers.clear();
    select_layer(nullptr);

    bool layer_selected = false;
    for (auto const& snapshot_layer : snapshot.m_layers) {
        auto layer = TRY(Layer::create_snapshot(*this, snapshot_layer));
        if (layer->is_selected()) {
            select_layer(layer.ptr());
            layer_selected = true;
        }
        layer->did_modify_bitmap({}, Layer::NotifyClients::No);
        add_layer(*layer);
    }

    if (!layer_selected)
        select_layer(&layer(0));

    m_size = snapshot.size();

    m_selection.set_mask(snapshot.m_selection.mask());

    did_change_rect();
    did_modify_layer_stack();
    return {};
}

size_t Image::index_of(Layer const& layer) const
{
    for (size_t i = 0; i < m_layers.size(); ++i) {
        if (m_layers[i] == &layer)
            return i;
    }
    VERIFY_NOT_REACHED();
}

void Image::move_layer_to_back(Layer& layer)
{
    NonnullRefPtr<Layer> protector(layer);
    auto index = index_of(layer);
    m_layers.remove(index);
    m_layers.prepend(layer);

    did_modify_layer_stack();
}

void Image::move_layer_to_front(Layer& layer)
{
    NonnullRefPtr<Layer> protector(layer);
    auto index = index_of(layer);
    m_layers.remove(index);
    m_layers.append(layer);

    did_modify_layer_stack();
}

void Image::move_layer_down(Layer& layer)
{
    NonnullRefPtr<Layer> protector(layer);
    auto index = index_of(layer);
    if (!index)
        return;
    m_layers.remove(index);
    m_layers.insert(index - 1, layer);

    did_modify_layer_stack();
}

void Image::move_layer_up(Layer& layer)
{
    NonnullRefPtr<Layer> protector(layer);
    auto index = index_of(layer);
    if (index == m_layers.size() - 1)
        return;
    m_layers.remove(index);
    m_layers.insert(index + 1, layer);

    did_modify_layer_stack();
}

void Image::change_layer_index(size_t old_index, size_t new_index)
{
    VERIFY(old_index < m_layers.size());
    VERIFY(new_index < m_layers.size());
    auto layer = m_layers.take(old_index);
    m_layers.insert(new_index, move(layer));
    did_modify_layer_stack();
}

void Image::did_modify_layer_stack()
{
    for (auto* client : m_clients)
        client->image_did_modify_layer_stack();

    did_change();
}

void Image::remove_layer(Layer& layer)
{
    NonnullRefPtr<Layer> protector(layer);
    auto index = index_of(layer);
    m_layers.remove(index);

    for (auto* client : m_clients)
        client->image_did_remove_layer(index);

    did_modify_layer_stack();
}

ErrorOr<void> Image::flatten_all_layers()
{
    return merge_layers(LayerMergeMode::All);
}

ErrorOr<void> Image::merge_visible_layers()
{
    return merge_layers(LayerMergeMode::VisibleOnly);
}

ErrorOr<void> Image::merge_layers(LayerMergeMode layer_merge_mode)
{
    if (m_layers.size() < 2)
        return {};

    Vector<NonnullRefPtr<Layer>> new_layers;
    Gfx::IntRect merged_layer_bounding_rect = {};
    size_t bottom_layer_index = 0;
    for (auto const& layer : m_layers) {
        if (!layer->is_visible()) {
            if (layer_merge_mode == LayerMergeMode::VisibleOnly)
                TRY(new_layers.try_append(layer));
            if (merged_layer_bounding_rect.is_empty())
                bottom_layer_index++;
            continue;
        }
        merged_layer_bounding_rect = merged_layer_bounding_rect.united(layer->relative_rect());
    }

    if (merged_layer_bounding_rect.is_empty())
        return {};

    NonnullRefPtr<Layer> bottom_layer = m_layers.at(bottom_layer_index);
    NonnullRefPtr<Layer> merged_layer = bottom_layer;
    if (!merged_layer->relative_rect().contains(merged_layer_bounding_rect)) {
        merged_layer = TRY(Layer::create_with_size(*this, merged_layer_bounding_rect.size(), bottom_layer->name()));
        merged_layer->set_location(merged_layer_bounding_rect.location());
    }

    GUI::Painter painter(merged_layer->content_bitmap());
    if (merged_layer.ptr() != bottom_layer.ptr())
        painter.blit(bottom_layer->location() - merged_layer->location(), bottom_layer->display_bitmap(), bottom_layer->rect(), static_cast<float>(bottom_layer->opacity_percent()) / 100.0f);
    for (size_t index = bottom_layer_index + 1; index < m_layers.size(); index++) {
        auto& layer = m_layers.at(index);
        if (!layer->is_visible())
            continue;
        painter.blit(layer->location() - merged_layer->location(), layer->display_bitmap(), layer->rect(), static_cast<float>(layer->opacity_percent()) / 100.0f);
    }

    TRY(new_layers.try_append(merged_layer));
    m_layers = move(new_layers);
    select_layer(merged_layer.ptr());
    did_modify_layer_stack();
    return {};
}

ErrorOr<void> Image::merge_active_layer_up(Layer& layer)
{
    return merge_active_layer(layer, LayerMergeDirection::Up);
}

ErrorOr<void> Image::merge_active_layer_down(Layer& layer)
{
    return merge_active_layer(layer, LayerMergeDirection::Down);
}

ErrorOr<void> Image::merge_active_layer(NonnullRefPtr<Layer> const& layer, LayerMergeDirection layer_merge_direction)
{
    if (m_layers.size() < 2)
        return {};

    if (!layer->is_visible())
        return Error::from_string_literal("Layer must be visible");

    auto layer_index = index_of(layer);
    auto direction = layer_merge_direction == LayerMergeDirection::Up ? 1 : -1;
    ssize_t layer_to_merge_index = layer_index + direction;
    ssize_t layer_count = m_layers.size();

    if (layer_to_merge_index < 0)
        return Error::from_string_literal("Layer is already at the bottom");
    if (layer_to_merge_index >= layer_count)
        return Error::from_string_literal("Layer is already at the top");

    Optional<NonnullRefPtr<Layer>> maybe_adjacent_layer;
    while (layer_to_merge_index >= 0 && layer_to_merge_index < layer_count) {
        auto const& layer = *m_layers[layer_to_merge_index];
        if (layer.is_visible()) {
            maybe_adjacent_layer = layer;
            break;
        }
        layer_to_merge_index += direction;
    }

    if (!maybe_adjacent_layer.has_value()) {
        auto error_message = layer_merge_direction == LayerMergeDirection::Up ? "No visible layers above this layer"sv : "No visible layers below this layer"sv;
        return Error::from_string_view(error_message);
    }

    auto adjacent_layer = maybe_adjacent_layer.value();
    auto bottom_layer = layer_merge_direction == LayerMergeDirection::Down ? adjacent_layer : layer;
    auto top_layer = layer_merge_direction == LayerMergeDirection::Down ? layer : adjacent_layer;
    auto merged_layer_bounding_rect = bottom_layer->relative_rect().united(top_layer->relative_rect());
    auto merged_layer = bottom_layer;
    if (!bottom_layer->relative_rect().contains(top_layer->relative_rect())) {
        merged_layer = TRY(Layer::create_with_size(*this, merged_layer_bounding_rect.size(), adjacent_layer->name()));
        merged_layer->set_location(merged_layer_bounding_rect.location());
    } else if (merged_layer.ptr() != adjacent_layer.ptr()) {
        merged_layer->set_name(adjacent_layer->name());
    }

    GUI::Painter painter(merged_layer->content_bitmap());
    if (merged_layer.ptr() != bottom_layer.ptr())
        painter.blit(bottom_layer->location() - merged_layer->location(), bottom_layer->display_bitmap(), bottom_layer->rect(), static_cast<float>(bottom_layer->opacity_percent()) / 100.0f);
    painter.blit(top_layer->location() - merged_layer->location(), top_layer->display_bitmap(), top_layer->rect(), static_cast<float>(top_layer->opacity_percent()) / 100.0f);

    auto top_layer_index = max(layer_index, layer_to_merge_index);
    auto bottom_layer_index = min(layer_index, layer_to_merge_index);
    m_layers.remove(top_layer_index);
    m_layers.remove(bottom_layer_index);
    m_layers.insert(top_layer_index - 1, merged_layer);
    select_layer(merged_layer);
    did_modify_layer_stack();
    return {};
}

void Image::select_layer(Layer* layer)
{
    for (auto* client : m_clients)
        client->image_select_layer(layer);
}

void Image::add_client(ImageClient& client)
{
    VERIFY(!m_clients.contains(&client));
    m_clients.set(&client);
}

void Image::remove_client(ImageClient& client)
{
    VERIFY(m_clients.contains(&client));
    m_clients.remove(&client);
}

void Image::layer_did_modify_bitmap(Badge<Layer>, Layer const& layer, Gfx::IntRect const& modified_layer_rect)
{
    auto layer_index = index_of(layer);
    for (auto* client : m_clients)
        client->image_did_modify_layer_bitmap(layer_index);

    did_change(modified_layer_rect.translated(layer.location()));
}

void Image::layer_did_modify_properties(Badge<Layer>, Layer const& layer)
{
    auto layer_index = index_of(layer);
    for (auto* client : m_clients)
        client->image_did_modify_layer_properties(layer_index);

    did_change();
}

void Image::did_change(Gfx::IntRect const& a_modified_rect)
{
    auto modified_rect = a_modified_rect.is_empty() ? this->rect() : a_modified_rect;
    for (auto* client : m_clients)
        client->image_did_change(modified_rect);
}

void Image::did_change_rect(Gfx::IntRect const& a_modified_rect)
{
    auto modified_rect = a_modified_rect.is_empty() ? this->rect() : a_modified_rect;
    for (auto* client : m_clients)
        client->image_did_change_rect(modified_rect);
}

ImageUndoCommand::ImageUndoCommand(Image& image, ByteString action_text)
    : m_snapshot(image.take_snapshot().release_value_but_fixme_should_propagate_errors())
    , m_image(image)
    , m_action_text(move(action_text))
{
}

void ImageUndoCommand::undo()
{
    // FIXME: Handle errors.
    (void)m_image.restore_snapshot(*m_snapshot);
}

void ImageUndoCommand::redo()
{
    undo();
}

ErrorOr<void> Image::flip(Gfx::Orientation orientation)
{
    Vector<NonnullRefPtr<Layer>> flipped_layers;
    TRY(flipped_layers.try_ensure_capacity(m_layers.size()));

    VERIFY(m_layers.size() > 0);

    size_t selected_layer_index = 0;
    for (size_t i = 0; i < m_layers.size(); ++i) {
        auto& layer = m_layers[i];
        auto new_layer = TRY(Layer::create_snapshot(*this, layer));

        if (layer->is_selected())
            selected_layer_index = i;

        TRY(new_layer->flip(orientation, Layer::NotifyClients::No));

        flipped_layers.unchecked_append(new_layer);
    }

    m_layers = move(flipped_layers);
    for (auto& layer : m_layers)
        layer->did_modify_bitmap({}, Layer::NotifyClients::No);

    select_layer(m_layers[selected_layer_index]);

    did_change();

    return {};
}

ErrorOr<void> Image::rotate(Gfx::RotationDirection direction)
{
    Vector<NonnullRefPtr<Layer>> rotated_layers;
    TRY(rotated_layers.try_ensure_capacity(m_layers.size()));

    VERIFY(m_layers.size() > 0);

    size_t selected_layer_index = 0;
    for (size_t i = 0; i < m_layers.size(); ++i) {
        auto& layer = m_layers[i];
        auto new_layer = TRY(Layer::create_snapshot(*this, layer));

        if (layer->is_selected())
            selected_layer_index = i;

        TRY(new_layer->rotate(direction, Layer::NotifyClients::No));

        rotated_layers.unchecked_append(new_layer);
    }

    m_layers = move(rotated_layers);
    for (auto& layer : m_layers)
        layer->did_modify_bitmap({}, Layer::NotifyClients::Yes);

    select_layer(m_layers[selected_layer_index]);

    m_size = { m_size.height(), m_size.width() };
    did_change_rect();

    return {};
}

ErrorOr<void> Image::crop(Gfx::IntRect const& cropped_rect)
{
    VERIFY(!cropped_rect.is_empty());

    Vector<NonnullRefPtr<Layer>> cropped_layers;
    TRY(cropped_layers.try_ensure_capacity(m_layers.size()));

    VERIFY(m_layers.size() > 0);

    RefPtr<Layer> selected_layer;
    for (auto const& layer : m_layers) {
        if (layer->is_selected())
            selected_layer = layer;

        auto layer_location = layer->location();
        auto layer_local_crop_rect = layer->relative_rect().intersected(cropped_rect).translated(-layer_location.x(), -layer_location.y());
        if (!layer->rect().intersects(layer_local_crop_rect))
            continue;

        auto new_layer = TRY(Layer::create_snapshot(*this, layer));
        TRY(new_layer->crop(layer_local_crop_rect, Layer::NotifyClients::No));

        auto new_layer_x = max(0, layer_location.x() - cropped_rect.x());
        auto new_layer_y = max(0, layer_location.y() - cropped_rect.y());

        new_layer->set_location({ new_layer_x, new_layer_y });

        cropped_layers.unchecked_append(new_layer);
    }

    if (cropped_layers.is_empty()) {
        auto layer_name = selected_layer ? selected_layer->name() : "Background";
        auto new_layer = TRY(Layer::create_with_size(*this, cropped_rect.size(), layer_name));
        new_layer->set_selected(true);
        cropped_layers.append(new_layer);
    }

    auto new_selected_layer = cropped_layers.last_matching([](auto& layer) {
        return layer->is_selected();
    });
    selected_layer = new_selected_layer.has_value() ? new_selected_layer.release_value() : cropped_layers.first();
    selected_layer->set_selected(true);

    m_layers = move(cropped_layers);

    select_layer(selected_layer);
    did_modify_layer_stack();

    m_size = { cropped_rect.width(), cropped_rect.height() };
    did_change_rect(cropped_rect);

    return {};
}

Optional<Gfx::IntRect> Image::nonempty_content_bounding_rect() const
{
    if (m_layers.is_empty())
        return {};

    Optional<Gfx::IntRect> bounding_rect;
    for (auto const& layer : m_layers) {
        auto layer_content_rect_in_layer_coordinates = layer->nonempty_content_bounding_rect();
        if (!layer_content_rect_in_layer_coordinates.has_value())
            layer_content_rect_in_layer_coordinates = layer->rect();

        auto layer_content_rect_in_image_coordinates = layer_content_rect_in_layer_coordinates->translated(layer->location());
        if (!bounding_rect.has_value())
            bounding_rect = layer_content_rect_in_image_coordinates;
        else
            bounding_rect = bounding_rect->united(layer_content_rect_in_image_coordinates);
    }

    bounding_rect->intersect(rect());
    if (bounding_rect == rect())
        return OptionalNone {};

    return bounding_rect;
}

ErrorOr<void> Image::resize(Gfx::IntSize new_size, Gfx::ScalingMode scaling_mode)
{
    float scale_x = 1.0f;
    float scale_y = 1.0f;

    if (size().width() != 0.0f) {
        scale_x = new_size.width() / static_cast<float>(size().width());
    }

    if (size().height() != 0.0f) {
        scale_y = new_size.height() / static_cast<float>(size().height());
    }

    if (scaling_mode != Gfx::ScalingMode::None) {
        Vector<NonnullRefPtr<Layer>> scaled_layers;
        TRY(scaled_layers.try_ensure_capacity(m_layers.size()));

        VERIFY(m_layers.size() > 0);

        size_t selected_layer_index = 0;
        for (size_t i = 0; i < m_layers.size(); ++i) {
            auto& layer = m_layers[i];
            auto new_layer = TRY(Layer::create_snapshot(*this, layer));

            if (layer->is_selected())
                selected_layer_index = i;

            auto layer_rect = layer->relative_rect().to_type<float>();
            auto scaled_top_left = layer_rect.top_left().scaled(scale_x, scale_y).to_rounded<int>();
            auto scaled_bottom_right = layer_rect.bottom_right().scaled(scale_x, scale_y).to_rounded<int>();
            auto scaled_layer_rect = Gfx::IntRect::from_two_points(scaled_top_left, scaled_bottom_right);
            TRY(new_layer->scale(scaled_layer_rect, scaling_mode, Layer::NotifyClients::No));

            scaled_layers.unchecked_append(new_layer);
        }

        m_layers = move(scaled_layers);
        for (auto& layer : m_layers)
            layer->did_modify_bitmap({}, Layer::NotifyClients::Yes);

        select_layer(m_layers[selected_layer_index]);
    }

    m_size = { new_size.width(), new_size.height() };
    did_change_rect();

    return {};
}

Color Image::color_at(Gfx::IntPoint point) const
{
    Color color;
    for (auto const& layer : m_layers) {
        if (!layer->is_visible() || !layer->rect().contains(point))
            continue;

        auto layer_color = layer->display_bitmap().get_pixel(point);
        float layer_opacity = layer->opacity_percent() / 100.0f;
        layer_color.set_alpha((u8)(layer_color.alpha() * layer_opacity));
        color = color.blend(layer_color);
    }
    return color;
}

}
