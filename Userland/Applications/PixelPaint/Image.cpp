/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
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
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/MappedFile.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <stdio.h>

namespace PixelPaint {

ErrorOr<NonnullRefPtr<Image>> Image::try_create_with_size(Gfx::IntSize const& size)
{
    VERIFY(!size.is_empty());

    if (size.width() > 16384 || size.height() > 16384)
        return Error::from_string_literal("Image size too large"sv);

    return adopt_nonnull_ref_or_enomem(new (nothrow) Image(size));
}

Image::Image(Gfx::IntSize const& size)
    : m_title("Untitled")
    , m_size(size)
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
        painter.draw_scaled_bitmap(target, layer.bitmap(), layer.rect(), (float)layer.opacity_percent() / 100.0f);
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Image::try_decode_bitmap(ReadonlyBytes bitmap_data)
{
    // Spawn a new ImageDecoder service process and connect to it.
    auto client = ImageDecoderClient::Client::construct();

    // FIXME: Find a way to avoid the memory copying here.
    auto maybe_decoded_image = client->decode_image(bitmap_data);
    if (!maybe_decoded_image.has_value())
        return Error::from_string_literal("Image decode failed"sv);

    // FIXME: Support multi-frame images?
    auto decoded_image = maybe_decoded_image.release_value();
    if (decoded_image.frames.is_empty())
        return Error::from_string_literal("Image decode failed (no frames)"sv);
    return decoded_image.frames[0].bitmap.release_nonnull();
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
    auto image = TRY(try_create_with_size({ json.get("width").to_i32(), json.get("height").to_i32() }));

    auto layers_value = json.get("layers");
    for (auto& layer_value : layers_value.as_array().values()) {
        auto& layer_object = layer_value.as_object();
        auto name = layer_object.get("name").as_string();

        auto bitmap_base64_encoded = layer_object.get("bitmap").as_string();
        auto bitmap_data = decode_base64(bitmap_base64_encoded);
        if (!bitmap_data.has_value())
            return Error::from_string_literal("Base64 decode failed"sv);

        auto bitmap = TRY(try_decode_bitmap(bitmap_data.value()));
        auto layer = TRY(Layer::try_create_with_bitmap(*image, move(bitmap), name));

        auto width = layer_object.get("width").to_i32();
        auto height = layer_object.get("height").to_i32();

        if (width != layer->size().width() || height != layer->size().height())
            return Error::from_string_literal("Decoded layer bitmap has wrong size"sv);

        image->add_layer(*layer);

        layer->set_location({ layer_object.get("locationx").to_i32(), layer_object.get("locationy").to_i32() });
        layer->set_opacity_percent(layer_object.get("opacity_percent").to_i32());
        layer->set_visible(layer_object.get("visible").as_bool());
        layer->set_selected(layer_object.get("selected").as_bool());
    }

    return image;
}

void Image::serialize_as_json(JsonObjectSerializer<StringBuilder>& json) const
{
    json.add("width", m_size.width());
    json.add("height", m_size.height());
    {
        auto json_layers = json.add_array("layers");
        for (const auto& layer : m_layers) {
            Gfx::BMPWriter bmp_dumber;
            auto json_layer = json_layers.add_object();
            json_layer.add("width", layer.size().width());
            json_layer.add("height", layer.size().height());
            json_layer.add("name", layer.name());
            json_layer.add("locationx", layer.location().x());
            json_layer.add("locationy", layer.location().y());
            json_layer.add("opacity_percent", layer.opacity_percent());
            json_layer.add("visible", layer.is_visible());
            json_layer.add("selected", layer.is_selected());
            json_layer.add("bitmap", encode_base64(bmp_dumber.dump(layer.bitmap())));
        }
    }
}

ErrorOr<void> Image::write_to_file(const String& file_path) const
{
    StringBuilder builder;
    JsonObjectSerializer json(builder);
    serialize_as_json(json);
    json.finish();

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

ErrorOr<void> Image::export_bmp_to_fd_and_close(int fd, bool preserve_alpha_channel)
{
    auto file = Core::File::construct();
    file->open(fd, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate, Core::File::ShouldCloseFileDescriptor::Yes);
    if (file->has_error())
        return Error::from_errno(file->error());

    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(try_compose_bitmap(bitmap_format));

    Gfx::BMPWriter dumper;
    auto encoded_data = dumper.dump(bitmap);

    if (!file->write(encoded_data.data(), encoded_data.size()))
        return Error::from_errno(file->error());

    return {};
}

ErrorOr<void> Image::export_png_to_fd_and_close(int fd, bool preserve_alpha_channel)
{
    auto file = Core::File::construct();
    file->open(fd, Core::OpenMode::WriteOnly | Core::OpenMode::Truncate, Core::File::ShouldCloseFileDescriptor::Yes);
    if (file->has_error())
        return Error::from_errno(file->error());

    auto bitmap_format = preserve_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;
    auto bitmap = TRY(try_compose_bitmap(bitmap_format));

    auto encoded_data = Gfx::PNGWriter::encode(*bitmap);
    if (!file->write(encoded_data.data(), encoded_data.size()))
        return Error::from_errno(file->error());

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
    for (const auto& layer : m_layers) {
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

    GUI::Painter painter(bottom_layer.bitmap());
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
            GUI::Painter painter(bottom_layer.bitmap());
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
    GUI::Painter painter(layer_below.bitmap());
    painter.draw_scaled_bitmap(rect(), layer.bitmap(), layer.rect(), (float)layer.opacity_percent() / 100.0f);
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

ImageUndoCommand::ImageUndoCommand(Image& image)
    : m_snapshot(image.take_snapshot().release_value_but_fixme_should_propagate_errors())
    , m_image(image)
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

void Image::set_title(String title)
{
    m_title = move(title);
    for (auto* client : m_clients)
        client->image_did_change_title(m_title);
}

void Image::set_path(String path)
{
    m_path = move(path);
    set_title(LexicalPath::basename(m_path));
}

void Image::flip(Gfx::Orientation orientation)
{
    for (auto& layer : m_layers) {
        auto flipped = layer.bitmap().flipped(orientation).release_value_but_fixme_should_propagate_errors();
        layer.set_bitmap(*flipped);
        layer.did_modify_bitmap(rect());
    }

    did_change();
}

void Image::rotate(Gfx::RotationDirection direction)
{
    for (auto& layer : m_layers) {
        auto rotated = layer.bitmap().rotated(direction).release_value_but_fixme_should_propagate_errors();
        layer.set_bitmap(*rotated);
        layer.did_modify_bitmap(rect());
    }

    m_size = { m_size.height(), m_size.width() };
    did_change_rect();
}

void Image::crop(Gfx::IntRect const& cropped_rect)
{
    for (auto& layer : m_layers) {
        auto cropped = layer.bitmap().cropped(cropped_rect).release_value_but_fixme_should_propagate_errors();
        layer.set_bitmap(*cropped);
        layer.did_modify_bitmap(rect());
    }

    m_size = { cropped_rect.width(), cropped_rect.height() };
    did_change_rect(cropped_rect);
}

Color Image::color_at(Gfx::IntPoint const& point) const
{
    Color color;
    for (auto& layer : m_layers) {
        if (!layer.is_visible() || !layer.rect().contains(point))
            continue;

        auto layer_color = layer.bitmap().get_pixel(point);
        float layer_opacity = layer.opacity_percent() / 100.0f;
        layer_color.set_alpha((u8)(layer_color.alpha() * layer_opacity));
        color = color.blend(layer_color);
    }
    return color;
}

}
