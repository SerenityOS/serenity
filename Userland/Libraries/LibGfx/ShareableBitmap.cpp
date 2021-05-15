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

ShareableBitmap::ShareableBitmap(const Bitmap& bitmap)
    : m_bitmap(bitmap.to_bitmap_backed_by_anon_fd())
{
}

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
    encoder << IPC::File(bitmap.anon_fd());
    encoder << bitmap.size();
    encoder << bitmap.scale();
    encoder << (u32)bitmap.format();
    if (bitmap.is_indexed()) {
        auto palette = bitmap.palette_to_vector();
        encoder << palette;
    }
    return true;
}

bool decode(Decoder& decoder, Gfx::ShareableBitmap& shareable_bitmap)
{
    bool valid = false;
    if (!decoder.decode(valid))
        return false;
    if (!valid) {
        shareable_bitmap = {};
        return true;
    }
    IPC::File anon_file;
    if (!decoder.decode(anon_file))
        return false;
    Gfx::IntSize size;
    if (!decoder.decode(size))
        return false;
    u32 scale;
    if (!decoder.decode(scale))
        return false;
    u32 raw_bitmap_format;
    if (!decoder.decode(raw_bitmap_format))
        return false;
    if (!Gfx::is_valid_bitmap_format(raw_bitmap_format))
        return false;
    auto bitmap_format = (Gfx::BitmapFormat)raw_bitmap_format;
    Vector<Gfx::RGBA32> palette;
    if (Gfx::Bitmap::is_indexed(bitmap_format)) {
        if (!decoder.decode(palette))
            return false;
    }
    auto bitmap = Gfx::Bitmap::create_with_anon_fd(bitmap_format, anon_file.take_fd(), size, scale, palette, Gfx::Bitmap::ShouldCloseAnonymousFile::Yes);
    if (!bitmap)
        return false;
    shareable_bitmap = Gfx::ShareableBitmap { bitmap.release_nonnull(), Gfx::ShareableBitmap::ConstructWithKnownGoodBitmap };
    return true;
}

}
