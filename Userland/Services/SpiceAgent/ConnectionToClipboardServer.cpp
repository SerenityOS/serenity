/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionToClipboardServer.h"
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <LibGfx/Bitmap.h>

// Copied from LibGUI/Clipboard.cpp
RefPtr<Gfx::Bitmap> ConnectionToClipboardServer::get_bitmap()
{
    auto clipping = get_clipboard_data();

    if (clipping.mime_type() != "image/x-serenityos")
        return nullptr;

    HashMap<String, String> const& metadata = clipping.metadata().entries();
    auto width = metadata.get("width").value_or("0").to_uint();
    if (!width.has_value() || width.value() == 0)
        return nullptr;

    auto height = metadata.get("height").value_or("0").to_uint();
    if (!height.has_value() || height.value() == 0)
        return nullptr;

    auto scale = metadata.get("scale").value_or("0").to_uint();
    if (!scale.has_value() || scale.value() == 0)
        return nullptr;

    auto pitch = metadata.get("pitch").value_or("0").to_uint();
    if (!pitch.has_value() || pitch.value() == 0)
        return nullptr;

    auto format = metadata.get("format").value_or("0").to_uint();
    if (!format.has_value() || format.value() == 0)
        return nullptr;

    auto data = clipping.data().data<void>();
    auto clipping_bitmap_or_error = Gfx::Bitmap::try_create_wrapper((Gfx::BitmapFormat)format.value(), { (int)width.value(), (int)height.value() }, scale.value(), pitch.value(), const_cast<void*>(data));
    if (clipping_bitmap_or_error.is_error())
        return nullptr;
    auto clipping_bitmap = clipping_bitmap_or_error.release_value();
    auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, { (int)width.value(), (int)height.value() }, scale.value());
    if (bitmap_or_error.is_error())
        return nullptr;
    auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
    for (int y = 0; y < clipping_bitmap->physical_height(); ++y) {
        for (int x = 0; x < clipping_bitmap->physical_width(); ++x) {
            auto pixel = clipping_bitmap->get_pixel(x, y);
            bitmap->set_pixel(x, y, pixel);
        }
    }

    return bitmap;
}

// Copied from LibGUI/Clipboard.cpp
void ConnectionToClipboardServer::set_bitmap(Gfx::Bitmap const& bitmap)
{
    HashMap<String, String> metadata;
    metadata.set("width", String::number(bitmap.width()));
    metadata.set("height", String::number(bitmap.height()));
    metadata.set("scale", String::number(bitmap.scale()));
    metadata.set("format", String::number((int)bitmap.format()));
    metadata.set("pitch", String::number(bitmap.pitch()));
    ReadonlyBytes data { bitmap.scanline(0), bitmap.size_in_bytes() };
    auto buffer_or_error = Core::AnonymousBuffer::create_with_size(bitmap.size_in_bytes());
    VERIFY(!buffer_or_error.is_error());
    auto buffer = buffer_or_error.release_value();
    memcpy(buffer.data<u8>(), data.data(), data.size());
    this->async_set_clipboard_data(buffer, "image/x-serenityos", metadata);
}
