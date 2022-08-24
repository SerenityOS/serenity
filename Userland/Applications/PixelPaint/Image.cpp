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
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/MappedFile.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/QOIWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <stdio.h>

namespace PixelPaint {

ErrorOr<NonnullRefPtr<Image>> Image::try_create_with_size(Gfx::IntSize const& size)
{
    VERIFY(!size.is_empty());

    if (size.width() > 16384 || size.height() > 16384)
        return Error::from_string_literal("Image size too large");

    return adopt_nonnull_ref_or_enomem(new (nothrow) Image(size));
}

Image::Image(Gfx::IntSize const& size)
    : m_size(size)
{
}

void Image::paint_into(GUI::Painter& painter, Gfx::IntRect const& dest_rect) const
{
    float scale = (float)dest_rect.width() / (float)rect().width();
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(dest_rect);
    for (auto& layer : m_layers) {
        if (!layer.is_visible())
            continue;
        auto target = dest_rect.translated(layer.location().x() * scale, layer.location().y() * scale);
        target.set_size(layer.size().width() * scale, layer.size().height() * scale);
        painter.draw_scaled_bitmap(target, layer.display_bitmap(), layer.rect(), (float)layer.opacity_percent() / 100.0f);
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Image::try_decode_bitmap(ReadonlyBytes bitmap_data)
{
    // Spawn a new ImageDecoder service process and connect to it.
    auto client = TRY(ImageDecoderClient::Client::try_create());

    // FIXME: Find a way to avoid the memory copying here.
    auto maybe_decoded_image = client->decode_image(bitmap_data);
    if (!maybe_decoded_image.has_value())
        return Error::from_string_literal("Image decode failed");

    // FIXME: Support multi-frame images?
    auto decoded_image = maybe_decoded_image.release_value();
    if (decoded_image.frames.is_empty())
        return Error::from_string_literal("Image decode failed (no frames)");

    auto decoded_bitmap = decoded_image.frames.first().bitmap;
    if (decoded_bitmap.is_null())
        return Error::from_string_literal("Image decode failed (no bitmap for frame)");
    return decoded_bitmap.release_nonnull();
}

ErrorOr<NonnullRefPtr<Image>> Image::try_create_from_bitmap(NonnullRefPtr<Gfx::Bitmap> bitmap)
{
    auto image = TRY(try_create_with_size({ bitmap->width(), bitmap->height() }));
    auto layer = TRY(Layer::try_create_with_bitmap(*image, *bitmap, "Background"));
    image->add_layer(move(layer));
    return image;
}

ErrorOr<NonnullRefPtr<Image>> Image::try_create_from_pixel_paint_json(JsonObject const& json)
{
    auto image = TRY(try_create_with_size({ json.get("width"sv).to_i32(), json.get("height"sv).to_i32() }));

    auto layers_value = json.get("layers"sv);
    for (auto& layer_value : layers_value.as_array().values()) {
        auto& layer_object = layer_value.as_object();
        auto name = layer_object.get("name"sv).as_string();

        auto bitmap_base64_encoded = layer_object.get("bitmap"sv).as_string();
        auto bitmap_data = TRY(decode_base64(bitmap_base64_encoded));
        auto bitmap = TRY(try_decode_bitmap(bitmap_data));
        auto layer = TRY(Layer::try_create_with_bitmap(*image, move(bitmap), name));

        if (auto mask_object = layer_object.get("mask"sv); !mask_object.is_null()) {
            auto mask_base64_encoded = mask_object.as_string();
            auto mask_data = TRY(decode_base64(mask_base64_encoded));
            auto mask = TRY(try_decode_bitmap(mask_data));
            TRY(layer->try_set_bitmaps(layer->content_bitmap(), mask));
        }

        auto width = layer_object.get("width"sv).to_i32();
        auto height = layer_object.get("height"sv).to_i32();

        if (width != layer->size().width() || height != layer->size().height())
            return Error::from_string_literal("Decoded layer bitmap has wrong size");

        image->add_layer(*layer);

        layer->set_location({ layer_object.get("locationx"sv).to_i32(), layer_object.get("locationy"sv).to_i32() });
        layer->set_opacity_percent(layer_object.get("opacity_percent"sv).to_i32());
        layer->set_visible(layer_object.get("visible"sv).as_bool());
        layer->set_selected(layer_object.get("selected"sv).as_bool());
    }

    return image;
}

void Image::serialize_as_json(JsonObjectSerializer<StringBuilder>& json) const
{
    MUST(json.add("width"sv, m_size.width()));
    MUST(json.add("height"sv, m_size.height()));
    {
        auto json_layers = MUST(json.add_array("layers"sv));
        for (auto const& layer : m_layers) {
            Gfx::BMPWriter bmp_writer;
            auto json_layer = MUST(json_layers.add_object());
            MUST(json_layer.add("width"sv, layer.size().width()));
            MUST(json_layer.add("height"sv, layer.size().height()));
            MUST(json_layer.add("name"sv, layer.name()));
            MUST(json_layer.add("locationx"sv, layer.location().x()));
            MUST(json_layer.add("locationy"sv, layer.location().y()));
            MUST(json_layer.add("opacity_percent"sv, layer.opacity_percent()));
            MUST(json_layer.add("visible"sv, layer.is_visible()));
            MUST(json_layer.add("selected"sv, layer.is_selected()));
            MUST(json_layer.add("bitmap"sv, encode_base64(bmp_writer.dump(layer.content_bitmap()))));
            if (layer.is_masked())
                MUST(json_layer.add("mask"sv, encode_base64(bmp_writer.dump(*layer.mask_bitmap()))));
            MUST(json_layer.finish());
        }

        MUST(json_layers.finish());
    }
}

ErrorOr<void> Image::write_to_file(String const& file_path) const
{
    StringBuilder builder;
    auto json = MUST(JsonObjectSerializer<>::try_create(builder));
    serialize_as_json(json);
    MUST(json.finish());

    auto file = TRY(Core::File::open(file_path, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate)));
    if (!file->write(builder.string_view()))
        return Error::from_errno(file->error());
    return {};
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Image::try_compose_bitmap(Gfx::BitmapFormat format) const
{
    auto bitmap = TRY(Gfx::Bitmap::try_create(format, m_size));
    GUI::Painter painter(bitmap);
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() });
    return bitmap;
}

RefPtr<Gfx::Bitmap> Image::try_copy_bitmap(Selection const& selection) const
{
    if (selection.is_empty())
        return {};
    auto selection_rect = selection.bounding_rect();

    // FIXME: Add a way to only compose a certain part of the image
    auto bitmap_or_error = try_compose_bitmap(Gfx::BitmapFormat::BGRA8888);
    if (bitmap_or_error.is_error())
        return {};
    auto full_bitmap = bitmap_or_error.release_value();

    auto cropped_bitmap_or_error = full_bitmap->cropped(selection_rect);
    if (cropped_bitmap_or_error.is_error())
        return nullptr;
    return cropped_bitmap_or_error.release_value_but_fixme_should_propagate_errors();
}

ErrorOr<void> Image::export_bmp_to_file(Core::File& file, bool preserve_alpha_channel)
{
    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(try_compose_bitmap(bitmap_format));

    Gfx::BMPWriter dumper;
    auto encoded_data = dumper.dump(bitmap);

    if (!file.write(encoded_data.data(), encoded_data.size()))
        return Error::from_errno(file.error());

    return {};
}

ErrorOr<void> Image::export_png_to_file(Core::File& file, bool preserve_alpha_channel)
{
    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(try_compose_bitmap(bitmap_format));

    auto encoded_data = Gfx::PNGWriter::encode(*bitmap);
    if (!file.write(encoded_data.data(), encoded_data.size()))
        return Error::from_errno(file.error());

    return {};
}

ErrorOr<void> Image::export_qoi_to_file(Core::File& file) const
{
    auto bitmap = TRY(try_compose_bitmap(Gfx::BitmapFormat::BGRA8888));

    auto encoded_data = Gfx::QOIWriter::encode(bitmap);
    if (!file.write(encoded_data.data(), encoded_data.size()))
        return Error::from_errno(file.error());

    return {};
}

void Image::add_layer(NonnullRefPtr<Layer> layer)
{
    for (auto& existing_layer : m_layers) {
        VERIFY(&existing_layer != layer.ptr());
    }
    m_layers.append(move(layer));

    for (auto* client : m_clients)
        client->image_did_add_layer(m_layers.size() - 1);

    did_modify_layer_stack();
}

ErrorOr<NonnullRefPtr<Image>> Image::take_snapshot() const
{
    auto snapshot = TRY(try_create_with_size(m_size));
    for (auto const& layer : m_layers) {
        auto layer_snapshot = TRY(Layer::try_create_snapshot(*snapshot, layer));
        snapshot->add_layer(move(layer_snapshot));
    }
    return snapshot;
}

ErrorOr<void> Image::restore_snapshot(Image const& snapshot)
{
    m_layers.clear();
    select_layer(nullptr);

    bool layer_selected = false;
    for (auto const& snapshot_layer : snapshot.m_layers) {
        auto layer = TRY(Layer::try_create_snapshot(*this, snapshot_layer));
        if (layer->is_selected()) {
            select_layer(layer.ptr());
            layer_selected = true;
        }
        add_layer(*layer);
    }

    if (!layer_selected)
        select_layer(&layer(0));

    m_size = snapshot.size();
    did_change_rect();
    did_modify_layer_stack();
    return {};
}

size_t Image::index_of(Layer const& layer) const
{
    for (size_t i = 0; i < m_layers.size(); ++i) {
        if (&m_layers.at(i) == &layer)
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

void Image::flatten_all_layers()
{
    if (m_layers.size() < 2)
        return;

    auto& bottom_layer = m_layers.at(0);

    GUI::Painter painter(bottom_layer.content_bitmap());
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() });

    for (size_t index = m_layers.size() - 1; index > 0; index--) {
        auto& layer = m_layers.at(index);
        remove_layer(layer);
    }
    bottom_layer.set_name("Background");
    select_layer(&bottom_layer);
}

void Image::merge_visible_layers()
{
    if (m_layers.size() < 2)
        return;

    size_t index = 0;

    while (index < m_layers.size()) {
        if (m_layers.at(index).is_visible()) {
            auto& bottom_layer = m_layers.at(index);
            GUI::Painter painter(bottom_layer.content_bitmap());
            paint_into(painter, { 0, 0, m_size.width(), m_size.height() });
            select_layer(&bottom_layer);
            index++;
            break;
        }
        index++;
    }
    while (index < m_layers.size()) {
        if (m_layers.at(index).is_visible()) {
            auto& layer = m_layers.at(index);
            remove_layer(layer);
        } else {
            index++;
        }
    }
}

void Image::merge_active_layer_up(Layer& layer)
{
    if (m_layers.size() < 2)
        return;
    size_t layer_index = this->index_of(layer);
    if ((layer_index + 1) == m_layers.size()) {
        dbgln("Cannot merge layer up: layer is already at the top");
        return; // FIXME: Notify user of error properly.
    }

    auto& layer_above = m_layers.at(layer_index + 1);
    GUI::Painter painter(layer_above.content_bitmap());
    painter.draw_scaled_bitmap(rect(), layer.display_bitmap(), layer.rect(), (float)layer.opacity_percent() / 100.0f);
    remove_layer(layer);
    select_layer(&layer_above);
}

void Image::merge_active_layer_down(Layer& layer)
{
    if (m_layers.size() < 2)
        return;
    int layer_index = this->index_of(layer);
    if (layer_index == 0) {
        dbgln("Cannot merge layer down: layer is already at the bottom");
        return; // FIXME: Notify user of error properly.
    }

    auto& layer_below = m_layers.at(layer_index - 1);
    GUI::Painter painter(layer_below.content_bitmap());
    painter.draw_scaled_bitmap(rect(), layer.display_bitmap(), layer.rect(), (float)layer.opacity_percent() / 100.0f);
    remove_layer(layer);
    select_layer(&layer_below);
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

ImageUndoCommand::ImageUndoCommand(Image& image, String action_text)
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

void Image::flip(Gfx::Orientation orientation)
{
    for (auto& layer : m_layers) {
        layer.flip(orientation);
    }

    did_change();
}

void Image::rotate(Gfx::RotationDirection direction)
{
    for (auto& layer : m_layers) {
        layer.rotate(direction);
    }

    m_size = { m_size.height(), m_size.width() };
    did_change_rect();
}

void Image::crop(Gfx::IntRect const& cropped_rect)
{
    for (auto& layer : m_layers) {
        layer.crop(cropped_rect);
    }

    m_size = { cropped_rect.width(), cropped_rect.height() };
    did_change_rect(cropped_rect);
}

Optional<Gfx::IntRect> Image::nonempty_content_bounding_rect() const
{
    if (m_layers.is_empty())
        return {};

    Optional<Gfx::IntRect> bounding_rect;
    for (auto& layer : m_layers) {
        auto layer_content_rect_in_layer_coordinates = layer.nonempty_content_bounding_rect();
        if (!layer_content_rect_in_layer_coordinates.has_value())
            continue;
        auto layer_content_rect_in_image_coordinates = layer_content_rect_in_layer_coordinates->translated(layer.location());
        if (!bounding_rect.has_value())
            bounding_rect = layer_content_rect_in_image_coordinates;
        else
            bounding_rect = bounding_rect->united(layer_content_rect_in_image_coordinates);
    }

    return bounding_rect;
}

void Image::resize(Gfx::IntSize const& new_size, Gfx::Painter::ScalingMode scaling_mode)
{
    float scale_x = 1.0f;
    float scale_y = 1.0f;

    if (size().width() != 0.0f) {
        scale_x = new_size.width() / static_cast<float>(size().width());
    }

    if (size().height() != 0.0f) {
        scale_y = new_size.height() / static_cast<float>(size().height());
    }

    for (auto& layer : m_layers) {
        Gfx::IntPoint new_location(scale_x * layer.location().x(), scale_y * layer.location().y());
        layer.resize(new_size, new_location, scaling_mode);
    }

    m_size = { new_size.width(), new_size.height() };
    did_change_rect();
}

Color Image::color_at(Gfx::IntPoint const& point) const
{
    Color color;
    for (auto& layer : m_layers) {
        if (!layer.is_visible() || !layer.rect().contains(point))
            continue;

        auto layer_color = layer.display_bitmap().get_pixel(point);
        float layer_opacity = layer.opacity_percent() / 100.0f;
        layer_color.set_alpha((u8)(layer_color.alpha() * layer_opacity));
        color = color.blend(layer_color);
    }
    return color;
}

}
