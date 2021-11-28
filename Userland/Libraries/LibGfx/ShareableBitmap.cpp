/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/Size.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>

namespace Gfx {

ShareableBitmap::ShareableBitmap(NonnullRefPtr<Bitmap> bitmap, Tag)
    : m_bitmap(move(bitmap))
{
}

}

namespace IPC {

bool encode(Encoder& encoder, const Gfx::ShareableBitmap& shareable_bitmap)
{
    encoder << shareable_bitmap.is_valid();
    if (!shareable_bitmap.is_valid())
        return true;
    auto& bitmap = *shareable_bitmap.bitmap();
    encoder << IPC::File(bitmap.anonymous_buffer().fd());
    encoder << bitmap.size();
    encoder << bitmap.scale();
    encoder << (u32)bitmap.format();
    if (bitmap.is_indexed()) {
        auto palette = bitmap.palette_to_vector();
        encoder << palette;
    }
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::ShareableBitmap& shareable_bitmap)
{
    bool valid = false;
    TRY(decoder.decode(valid));
    if (!valid) {
        shareable_bitmap = {};
        return {};
    }
    IPC::File anon_file;
    TRY(decoder.decode(anon_file));
    Gfx::IntSize size;
    TRY(decoder.decode(size));
    u32 scale;
    TRY(decoder.decode(scale));
    u32 raw_bitmap_format;
    TRY(decoder.decode(raw_bitmap_format));
    if (!Gfx::is_valid_bitmap_format(raw_bitmap_format))
        return Error::from_string_literal("IPC: Invalid Gfx::ShareableBitmap format"sv);
    auto bitmap_format = (Gfx::BitmapFormat)raw_bitmap_format;
    Vector<Gfx::RGBA32> palette;
    if (Gfx::Bitmap::is_indexed(bitmap_format)) {
        TRY(decoder.decode(palette));
    }
    auto buffer = TRY(Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), Gfx::Bitmap::size_in_bytes(Gfx::Bitmap::minimum_pitch(size.width(), bitmap_format), size.height())));
    auto bitmap = TRY(Gfx::Bitmap::try_create_with_anonymous_buffer(bitmap_format, move(buffer), size, scale, palette));
    shareable_bitmap = Gfx::ShareableBitmap { move(bitmap), Gfx::ShareableBitmap::ConstructWithKnownGoodBitmap };
    return {};
}

}
