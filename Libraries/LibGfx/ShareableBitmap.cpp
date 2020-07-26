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

#include <AK/SharedBuffer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/Size.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

ShareableBitmap::ShareableBitmap(const Bitmap& bitmap)
    : m_bitmap(bitmap.to_bitmap_backed_by_shared_buffer())
{
}

}

namespace IPC {

bool encode(Encoder& encoder, const Gfx::ShareableBitmap& shareable_bitmap)
{
    encoder << shareable_bitmap.shbuf_id();
    encoder << shareable_bitmap.width();
    encoder << shareable_bitmap.height();
    return true;
}

bool decode(Decoder& decoder, Gfx::ShareableBitmap& shareable_bitmap)
{
    i32 shbuf_id = 0;
    Gfx::IntSize size;
    if (!decoder.decode(shbuf_id))
        return false;
    if (!decoder.decode(size))
        return false;

    if (shbuf_id == -1)
        return true;

    dbg() << "Decoding a ShareableBitmap with shbuf_id=" << shbuf_id << ", size=" << size;

    auto shared_buffer = SharedBuffer::create_from_shbuf_id(shbuf_id);
    if (!shared_buffer)
        return false;

    auto bitmap = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, shared_buffer.release_nonnull(), size);
    shareable_bitmap = bitmap->to_shareable_bitmap();
    return true;
}

}
