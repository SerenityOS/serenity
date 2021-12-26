/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Checked.h>
#include <AK/Memory.h>
#include <AK/SharedBuffer.h>
#include <AK/String.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/ShareableBitmap.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
#include <unistd.h>

namespace Gfx {

static bool size_would_overflow(BitmapFormat format, const IntSize& size)
{
    if (size.width() < 0 || size.height() < 0)
        return true;
    return Checked<size_t>::multiplication_would_overflow(size.width(), size.height(), Bitmap::bpp_for_format(format));
}

RefPtr<Bitmap> Bitmap::create(BitmapFormat format, const IntSize& size)
{
    if (size_would_overflow(format, size))
        return nullptr;
    return adopt(*new Bitmap(format, size, Purgeable::No));
}

RefPtr<Bitmap> Bitmap::create_purgeable(BitmapFormat format, const IntSize& size)
{
    if (size_would_overflow(format, size))
        return nullptr;
    return adopt(*new Bitmap(format, size, Purgeable::Yes));
}

Bitmap::Bitmap(BitmapFormat format, const IntSize& size, Purgeable purgeable)
    : m_size(size)
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
    , m_purgeable(purgeable == Purgeable::Yes)
{
    ASSERT(!m_size.is_empty());
    ASSERT(!size_would_overflow(format, size));
    if (format == BitmapFormat::Indexed8)
        m_palette = new RGBA32[256];
    int map_flags = purgeable == Purgeable::Yes ? (MAP_PURGEABLE | MAP_PRIVATE) : (MAP_ANONYMOUS | MAP_PRIVATE);
    m_data = (RGBA32*)mmap_with_name(nullptr, size_in_bytes(), PROT_READ | PROT_WRITE, map_flags, 0, 0, String::format("GraphicsBitmap [%dx%d]", width(), height()).characters());
    ASSERT(m_data && m_data != (void*)-1);
    m_needs_munmap = true;
}

RefPtr<Bitmap> Bitmap::create_wrapper(BitmapFormat format, const IntSize& size, size_t pitch, RGBA32* data)
{
    if (size_would_overflow(format, size))
        return nullptr;
    return adopt(*new Bitmap(format, size, pitch, data));
}

RefPtr<Bitmap> Bitmap::load_from_file(const StringView& path)
{
#define __ENUMERATE_IMAGE_FORMAT(Name, Ext) \
    if (path.ends_with(Ext))                \
        return load_##Name(path);
    ENUMERATE_IMAGE_FORMATS
#undef __ENUMERATE_IMAGE_FORMAT

    return nullptr;
}

Bitmap::Bitmap(BitmapFormat format, const IntSize& size, size_t pitch, RGBA32* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(pitch)
    , m_format(format)
{
    ASSERT(!size_would_overflow(format, size));
    if (format == BitmapFormat::Indexed8)
        m_palette = new RGBA32[256];
}

RefPtr<Bitmap> Bitmap::create_with_shared_buffer(BitmapFormat format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const IntSize& size)
{
    if (size_would_overflow(format, size))
        return nullptr;
    return adopt(*new Bitmap(format, move(shared_buffer), size));
}

Bitmap::Bitmap(BitmapFormat format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const IntSize& size)
    : m_size(size)
    , m_data((RGBA32*)shared_buffer->data())
    , m_pitch(round_up_to_power_of_two(size.width() * sizeof(RGBA32), 16))
    , m_format(format)
    , m_shared_buffer(move(shared_buffer))
{
    ASSERT(format != BitmapFormat::Indexed8);
    ASSERT(!size_would_overflow(format, size));
}

RefPtr<Gfx::Bitmap> Bitmap::rotated(Gfx::RotationDirection rotation_direction) const
{
    auto w = this->width();
    auto h = this->height();

    auto new_bitmap = Gfx::Bitmap::create(this->format(), { h, w });
    if (!new_bitmap)
        return nullptr;

    for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
            Color color;
            if (rotation_direction == Gfx::RotationDirection::Left)
                color = this->get_pixel(w - i - 1, j);
            else
                color = this->get_pixel(i, h - j - 1);

            new_bitmap->set_pixel(j, i, color);
        }
    }

    return new_bitmap;
}

RefPtr<Gfx::Bitmap> Bitmap::flipped(Gfx::Orientation orientation) const
{
    auto w = this->width();
    auto h = this->height();

    auto new_bitmap = Gfx::Bitmap::create(this->format(), { w, h });
    if (!new_bitmap)
        return nullptr;

    for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
            Color color = this->get_pixel(i, j);
            if (orientation == Orientation::Vertical)
                new_bitmap->set_pixel(i, h - j - 1, color);
            else
                new_bitmap->set_pixel(w - i - 1, j, color);
        }
    }

    return new_bitmap;
}

RefPtr<Bitmap> Bitmap::to_bitmap_backed_by_shared_buffer() const
{
    if (m_shared_buffer)
        return *this;
    auto buffer = SharedBuffer::create_with_size(size_in_bytes());
    auto bitmap = Bitmap::create_with_shared_buffer(m_format, *buffer, m_size);
    if (!bitmap)
        return nullptr;
    memcpy(buffer->data(), scanline(0), size_in_bytes());
    return bitmap;
}

Bitmap::~Bitmap()
{
    if (m_needs_munmap) {
        int rc = munmap(m_data, size_in_bytes());
        ASSERT(rc == 0);
    }
    m_data = nullptr;
    delete[] m_palette;
}

void Bitmap::set_mmap_name(const StringView& name)
{
    ASSERT(m_needs_munmap);
    ::set_mmap_name(m_data, size_in_bytes(), name.to_string().characters());
}

void Bitmap::fill(Color color)
{
    ASSERT(m_format == BitmapFormat::RGB32 || m_format == BitmapFormat::RGBA32);
    for (int y = 0; y < height(); ++y) {
        auto* scanline = this->scanline(y);
        fast_u32_fill(scanline, color.value(), width());
    }
}

void Bitmap::set_volatile()
{
    ASSERT(m_purgeable);
    if (m_volatile)
        return;
    int rc = madvise(m_data, size_in_bytes(), MADV_SET_VOLATILE);
    if (rc < 0) {
        perror("madvise(MADV_SET_VOLATILE)");
        ASSERT_NOT_REACHED();
    }
    m_volatile = true;
}

[[nodiscard]] bool Bitmap::set_nonvolatile()
{
    ASSERT(m_purgeable);
    if (!m_volatile)
        return true;
    int rc = madvise(m_data, size_in_bytes(), MADV_SET_NONVOLATILE);
    if (rc < 0) {
        perror("madvise(MADV_SET_NONVOLATILE)");
        ASSERT_NOT_REACHED();
    }
    m_volatile = false;
    return rc == 0;
}

int Bitmap::shbuf_id() const
{
    return m_shared_buffer ? m_shared_buffer->shbuf_id() : -1;
}

ShareableBitmap Bitmap::to_shareable_bitmap(pid_t peer_pid) const
{
    auto bitmap = to_bitmap_backed_by_shared_buffer();
    if (!bitmap)
        return {};
    if (peer_pid > 0)
        bitmap->shared_buffer()->share_with(peer_pid);
    return ShareableBitmap(*bitmap);
}

}
