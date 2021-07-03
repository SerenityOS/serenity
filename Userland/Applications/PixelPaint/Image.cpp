/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"
#include "Layer.h"
#include <AK/Base64.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/PNGWriter.h>
#include <LibImageDecoderClient/Client.h>
#include <stdio.h>

namespace PixelPaint {

static RefPtr<Gfx::Bitmap> try_decode_bitmap(ByteBuffer const& bitmap_data)
{
    // Spawn a new ImageDecoder service process and connect to it.
    auto client = ImageDecoderClient::Client::construct();

    // FIXME: Find a way to avoid the memory copying here.
    auto decoded_image_or_error = client->decode_image(bitmap_data);
    if (!decoded_image_or_error.has_value())
        return nullptr;

    // FIXME: Support multi-frame images?
    auto decoded_image = decoded_image_or_error.release_value();
    if (decoded_image.frames.is_empty())
        return nullptr;
    return move(decoded_image.frames[0].bitmap);
}

RefPtr<Image> Image::try_create_with_size(Gfx::IntSize const& size)
{
    if (size.is_empty())
        return nullptr;

    if (size.width() > 16384 || size.height() > 16384)
        return nullptr;

    return adopt_ref(*new Image(size));
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

RefPtr<Image> Image::try_create_from_bitmap(NonnullRefPtr<Gfx::Bitmap> bitmap)
{
    auto image = try_create_with_size({ bitmap->width(), bitmap->height() });
    if (!image)
        return nullptr;

    auto layer = Layer::try_create_with_bitmap(*image, *bitmap, "Background");
    if (!layer)
        return nullptr;

    image->add_layer(layer.release_nonnull());
    return image;
}

Result<NonnullRefPtr<Image>, String> Image::try_create_from_pixel_paint_file(String const& file_path)
{
    auto file_or_error = Core::File::open(file_path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error())
        return file_or_error.error();

    auto& file = *file_or_error.value();
    auto contents = file.read_all();

    auto json_or_error = JsonValue::from_string(contents);
    if (!json_or_error.has_value())
        return String { "Not a valid PP file"sv };

    auto& json = json_or_error.value().as_object();
    auto image = try_create_with_size({ json.get("width").to_i32(), json.get("height").to_i32() });
    if (!image)
        return String { "Image memory allocation failed" };

    auto layers_value = json.get("layers");
    for (auto& layer_value : layers_value.as_array().values()) {
        auto& layer_object = layer_value.as_object();
        auto name = layer_object.get("name").as_string();

        auto bitmap_base64_encoded = layer_object.get("bitmap").as_string();
        auto bitmap_data = decode_base64(bitmap_base64_encoded);

        auto bitmap = try_decode_bitmap(bitmap_data);
        if (!bitmap)
            return String { "Layer bitmap decode failed"sv };

        auto layer = Layer::try_create_with_bitmap(*image, bitmap.release_nonnull(), name);
        if (!layer)
            return String { "Layer allocation failed"sv };

        auto width = layer_object.get("width").to_i32();
        auto height = layer_object.get("height").to_i32();

        if (width != layer->size().width() || height != layer->size().height())
            return String { "Decoded layer bitmap has wrong size"sv };

        layer->set_location({ layer_object.get("locationx").to_i32(), layer_object.get("locationy").to_i32() });
        layer->set_opacity_percent(layer_object.get("opacity_percent").to_i32());
        layer->set_visible(layer_object.get("visible").as_bool());
        layer->set_selected(layer_object.get("selected").as_bool());

        image->add_layer(*layer);
    }

    image->set_path(file_path);
    return image.release_nonnull();
}

Result<NonnullRefPtr<Image>, String> Image::try_create_from_file(String const& file_path)
{
    auto image_or_error = try_create_from_pixel_paint_file(file_path);
    if (!image_or_error.is_error())
        return image_or_error.release_value();

    auto file_or_error = MappedFile::map(file_path);
    if (file_or_error.is_error())
        return String { "Unable to mmap file"sv };

    auto& mapped_file = *file_or_error.value();
    // FIXME: Find a way to avoid the memory copy here.
    auto bitmap = try_decode_bitmap(ByteBuffer::copy(mapped_file.bytes()));
    if (!bitmap)
        return String { "Unable to decode image"sv };
    auto image = Image::try_create_from_bitmap(bitmap.release_nonnull());
    if (!image)
        return String { "Unable to allocate Image"sv };
    image->set_path(file_path);
    return image.release_nonnull();
}

Result<void, String> Image::write_to_file(const String& file_path) const
{
    StringBuilder builder;
    JsonObjectSerializer json(builder);
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
    json.finish();

    auto file_or_error = Core::File::open(file_path, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
    if (file_or_error.is_error())
        return file_or_error.error();

    if (!file_or_error.value()->write(builder.string_view()))
        return String { file_or_error.value()->error_string() };
    return {};
}

RefPtr<Gfx::Bitmap> Image::try_compose_bitmap() const
{
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, m_size);
    if (!bitmap)
        return nullptr;
    GUI::Painter painter(*bitmap);
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() });
    return bitmap;
}

Result<void, String> Image::export_bmp_to_file(String const& file_path)
{
    auto file_or_error = Core::File::open(file_path, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
    if (file_or_error.is_error())
        return file_or_error.error();

    auto bitmap = try_compose_bitmap();
    if (!bitmap)
        return String { "Failed to allocate bitmap for encoding"sv };

    Gfx::BMPWriter dumper;
    auto encoded_data = dumper.dump(bitmap);

    auto& file = *file_or_error.value();
    if (!file.write(encoded_data.data(), encoded_data.size()))
        return String { "Failed to write encoded BMP data to file"sv };

    return {};
}

Result<void, String> Image::export_png_to_file(String const& file_path)
{
    auto file_or_error = Core::File::open(file_path, (Core::OpenMode)(Core::OpenMode::WriteOnly | Core::OpenMode::Truncate));
    if (file_or_error.is_error())
        return file_or_error.error();

    auto bitmap = try_compose_bitmap();
    if (!bitmap)
        return String { "Failed to allocate bitmap for encoding"sv };

    auto encoded_data = Gfx::PNGWriter::encode(*bitmap);
    auto& file = *file_or_error.value();
    if (!file.write(encoded_data.data(), encoded_data.size()))
        return String { "Failed to write encoded PNG data to file"sv };

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

RefPtr<Image> Image::take_snapshot() const
{
    auto snapshot = try_create_with_size(m_size);
    if (!snapshot)
        return nullptr;
    for (const auto& layer : m_layers) {
        auto layer_snapshot = Layer::try_create_snapshot(*snapshot, layer);
        if (!layer_snapshot)
            return nullptr;
        snapshot->add_layer(layer_snapshot.release_nonnull());
    }
    return snapshot;
}

void Image::restore_snapshot(Image const& snapshot)
{
    m_layers.clear();
    select_layer(nullptr);
    for (const auto& snapshot_layer : snapshot.m_layers) {
        auto layer = Layer::try_create_snapshot(*this, snapshot_layer);
        VERIFY(layer);
        if (layer->is_selected())
            select_layer(layer.ptr());
        add_layer(*layer);
    }

    did_modify_layer_stack();
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

void Image::layer_did_modify_bitmap(Badge<Layer>, Layer const& layer)
{
    auto layer_index = index_of(layer);
    for (auto* client : m_clients)
        client->image_did_modify_layer(layer_index);

    did_change();
}

void Image::layer_did_modify_properties(Badge<Layer>, Layer const& layer)
{
    auto layer_index = index_of(layer);
    for (auto* client : m_clients)
        client->image_did_modify_layer(layer_index);

    did_change();
}

void Image::did_change()
{
    for (auto* client : m_clients)
        client->image_did_change();
}

ImageUndoCommand::ImageUndoCommand(Image& image)
    : m_snapshot(image.take_snapshot())
    , m_image(image)
{
}

void ImageUndoCommand::undo()
{
    m_image.restore_snapshot(*m_snapshot);
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
    set_title(LexicalPath(m_path).basename());
}

}
