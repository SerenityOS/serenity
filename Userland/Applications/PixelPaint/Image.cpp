/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Image.h"
#include "Layer.h"
#include <AK/Base64.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BMPWriter.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/PNGWriter.h>
#include <stdio.h>

namespace PixelPaint {

RefPtr<Image> Image::create_with_size(const Gfx::IntSize& size)
{
    if (size.is_empty())
        return nullptr;

    if (size.width() > 16384 || size.height() > 16384)
        return nullptr;

    return adopt(*new Image(size));
}

Image::Image(const Gfx::IntSize& size)
    : m_size(size)
{
}

void Image::paint_into(GUI::Painter& painter, const Gfx::IntRect& dest_rect)
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

RefPtr<Image> Image::create_from_file(const String& file_path)
{
    auto file = fopen(file_path.characters(), "r");
    fseek(file, 0L, SEEK_END);
    auto length = ftell(file);
    rewind(file);

    auto buffer = ByteBuffer::create_uninitialized(length);
    fread(buffer.data(), sizeof(u8), length, file);
    fclose(file);

    auto json_or_error = JsonValue::from_string(String::copy(buffer));
    if (!json_or_error.has_value())
        return nullptr;

    auto json = json_or_error.value().as_object();
    auto image = create_with_size({ json.get("width").to_i32(), json.get("height").to_i32() });
    json.get("layers").as_array().for_each([&](JsonValue json_layer) {
        auto json_layer_object = json_layer.as_object();
        auto width = json_layer_object.get("width").to_i32();
        auto height = json_layer_object.get("height").to_i32();
        auto name = json_layer_object.get("name").as_string();
        auto layer = Layer::create_with_size(*image, { width, height }, name);
        layer->set_location({ json_layer_object.get("locationx").to_i32(), json_layer_object.get("locationy").to_i32() });
        layer->set_opacity_percent(json_layer_object.get("opacity_percent").to_i32());
        layer->set_visible(json_layer_object.get("visible").as_bool());
        layer->set_selected(json_layer_object.get("selected").as_bool());

        auto bitmap_base64_encoded = json_layer_object.get("bitmap").as_string();
        auto bitmap_data = decode_base64(bitmap_base64_encoded);
        auto image_decoder = Gfx::ImageDecoder::create(bitmap_data);
        layer->set_bitmap(*image_decoder->bitmap());
        image->add_layer(*layer);
    });

    return image;
}

void Image::save(const String& file_path) const
{
    // Build json file
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

    // Write json to disk
    auto file = fopen(file_path.characters(), "w");
    auto byte_buffer = builder.to_byte_buffer();
    fwrite(byte_buffer.data(), sizeof(u8), byte_buffer.size(), file);
    fclose(file);
}

void Image::export_bmp(const String& file_path)
{
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, m_size);
    GUI::Painter painter(*bitmap);
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() });

    Gfx::BMPWriter dumper;
    auto bmp = dumper.dump(bitmap);
    auto file = fopen(file_path.characters(), "wb");
    fwrite(bmp.data(), sizeof(u8), bmp.size(), file);
    fclose(file);
}

void Image::export_png(const String& file_path)
{
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, m_size);
    GUI::Painter painter(*bitmap);
    paint_into(painter, { 0, 0, m_size.width(), m_size.height() });

    Gfx::PNGWriter png_writer;
    auto png = png_writer.write(bitmap);
    auto file = fopen(file_path.characters(), "wb");
    fwrite(png.data(), sizeof(u8), png.size(), file);
    fclose(file);
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
    auto snapshot = create_with_size(m_size);
    for (const auto& layer : m_layers)
        snapshot->add_layer(*Layer::create_snapshot(*snapshot, layer));
    return snapshot;
}

void Image::restore_snapshot(const Image& snapshot)
{
    m_layers.clear();
    select_layer(nullptr);
    for (const auto& snapshot_layer : snapshot.m_layers) {
        auto layer = Layer::create_snapshot(*this, snapshot_layer);
        if (layer->is_selected())
            select_layer(layer.ptr());
        add_layer(*layer);
    }

    did_modify_layer_stack();
}

size_t Image::index_of(const Layer& layer) const
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

void Image::layer_did_modify_bitmap(Badge<Layer>, const Layer& layer)
{
    auto layer_index = index_of(layer);
    for (auto* client : m_clients)
        client->image_did_modify_layer(layer_index);

    did_change();
}

void Image::layer_did_modify_properties(Badge<Layer>, const Layer& layer)
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

}
