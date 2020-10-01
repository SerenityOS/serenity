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
#include <AK/Optional.h>
#include <AK/SharedBuffer.h>
#include <AK/String.h>
#include <LibGfx/BMPLoader.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/GIFLoader.h>
#include <LibGfx/ICOLoader.h>
#include <LibGfx/JPGLoader.h>
#include <LibGfx/PBMLoader.h>
#include <LibGfx/PGMLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/PPMLoader.h>
#include <LibGfx/ShareableBitmap.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>

namespace Gfx {

struct BackingStore {
    void* data { nullptr };
    size_t pitch { 0 };
    size_t size_in_bytes { 0 };
};

size_t Bitmap::minimum_pitch(size_t width, BitmapFormat format)
{
    size_t element_size;
    switch (determine_storage_format(format)) {
    case StorageFormat::Indexed8:
        element_size = 1;
        break;
    case StorageFormat::RGB32:
    case StorageFormat::RGBA32:
        element_size = 4;
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    return width * element_size;
}

static bool size_would_overflow(BitmapFormat format, const IntSize& size)
{
    if (size.width() < 0 || size.height() < 0)
        return true;
    // This check is a bit arbitrary, but should protect us from most shenanigans:
    if (size.width() >= 32768 || size.height() >= 32768)
        return true;
    // In contrast, this check is absolutely necessary:
    size_t pitch = Bitmap::minimum_pitch(size.width(), format);
    return Checked<size_t>::multiplication_would_overflow(pitch, size.height());
}

RefPtr<Bitmap> Bitmap::create(BitmapFormat format, const IntSize& size)
{
    auto backing_store = Bitmap::allocate_backing_store(format, size, Purgeable::No);
    if (!backing_store.has_value())
        return nullptr;
    return adopt(*new Bitmap(format, size, Purgeable::No, backing_store.value()));
}

RefPtr<Bitmap> Bitmap::create_purgeable(BitmapFormat format, const IntSize& size)
{
    auto backing_store = Bitmap::allocate_backing_store(format, size, Purgeable::Yes);
    if (!backing_store.has_value())
        return nullptr;
    return adopt(*new Bitmap(format, size, Purgeable::Yes, backing_store.value()));
}

Bitmap::Bitmap(BitmapFormat format, const IntSize& size, Purgeable purgeable, const BackingStore& backing_store)
    : m_size(size)
    , m_data(backing_store.data)
    , m_pitch(backing_store.pitch)
    , m_format(format)
    , m_purgeable(purgeable == Purgeable::Yes)
{
    ASSERT(!m_size.is_empty());
    ASSERT(!size_would_overflow(format, size));
    ASSERT(m_data);
    ASSERT(backing_store.size_in_bytes == size_in_bytes());
    allocate_palette_from_format(format, {});
    m_needs_munmap = true;
}

RefPtr<Bitmap> Bitmap::create_wrapper(BitmapFormat format, const IntSize& size, size_t pitch, void* data)
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

Bitmap::Bitmap(BitmapFormat format, const IntSize& size, size_t pitch, void* data)
    : m_size(size)
    , m_data(data)
    , m_pitch(pitch)
    , m_format(format)
{
    ASSERT(pitch >= minimum_pitch(size.width(), format));
    ASSERT(!size_would_overflow(format, size));
    // FIXME: assert that `data` is actually long enough!

    allocate_palette_from_format(format, {});
}

RefPtr<Bitmap> Bitmap::create_with_shared_buffer(BitmapFormat format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const IntSize& size)
{
    return create_with_shared_buffer(format, move(shared_buffer), size, {});
}

RefPtr<Bitmap> Bitmap::create_with_shared_buffer(BitmapFormat format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const IntSize& size, const Vector<RGBA32>& palette)
{
    if (size_would_overflow(format, size))
        return nullptr;

    unsigned actual_size = shared_buffer->size();
    // FIXME: Code duplication of size_in_bytes() and m_pitch
    unsigned expected_size_min = minimum_pitch(size.width(), format) * size.height();
    unsigned expected_size_max = round_up_to_power_of_two(expected_size_min, PAGE_SIZE);
    if (expected_size_min > actual_size || actual_size > expected_size_max) {
        // Getting here is most likely an error.
        dbg() << "Constructing a shared bitmap for format " << (int)format << " and size " << size << ", which demands " << expected_size_min << " bytes, which rounds up to at most " << expected_size_max << ".";
        dbg() << "However, we were given " << actual_size << " bytes, which is outside this range?! Refusing cowardly.";
        return {};
    }

    return adopt(*new Bitmap(format, move(shared_buffer), size, palette));
}

Bitmap::Bitmap(BitmapFormat format, NonnullRefPtr<SharedBuffer>&& shared_buffer, const IntSize& size, const Vector<RGBA32>& palette)
    : m_size(size)
    , m_data(shared_buffer->data<void>())
    , m_pitch(minimum_pitch(size.width(), format))
    , m_format(format)
    , m_shared_buffer(move(shared_buffer))
{
    ASSERT(!is_indexed() || !palette.is_empty());
    ASSERT(!size_would_overflow(format, size));
    ASSERT(size_in_bytes() <= static_cast<size_t>(m_shared_buffer->size()));

    if (is_indexed(m_format))
        allocate_palette_from_format(m_format, palette);
}

RefPtr<Gfx::Bitmap> Bitmap::clone() const
{
    RefPtr<Gfx::Bitmap> new_bitmap {};
    if (m_purgeable) {
        new_bitmap = Bitmap::create_purgeable(format(), size());
    } else {
        new_bitmap = Bitmap::create(format(), size());
    }

    if (!new_bitmap) {
        return nullptr;
    }

    ASSERT(size_in_bytes() == new_bitmap->size_in_bytes());
    memcpy(new_bitmap->scanline(0), scanline(0), size_in_bytes());

    return new_bitmap;
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
    auto bitmap = Bitmap::create_with_shared_buffer(m_format, *buffer, m_size, palette_to_vector());
    if (!bitmap)
        return nullptr;
    memcpy(buffer->data<void>(), scanline(0), size_in_bytes());
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
#ifdef __serenity__
    ::set_mmap_name(m_data, size_in_bytes(), name.to_string().characters());
#else
    (void)name;
#endif
}

void Bitmap::fill(Color color)
{
    ASSERT(!is_indexed(m_format));
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
#ifdef __serenity__
    int rc = madvise(m_data, size_in_bytes(), MADV_SET_VOLATILE);
    if (rc < 0) {
        perror("madvise(MADV_SET_VOLATILE)");
        ASSERT_NOT_REACHED();
    }
#endif
    m_volatile = true;
}

[[nodiscard]] bool Bitmap::set_nonvolatile()
{
    ASSERT(m_purgeable);
    if (!m_volatile)
        return true;
#ifdef __serenity__
    int rc = madvise(m_data, size_in_bytes(), MADV_SET_NONVOLATILE);
    if (rc < 0) {
        perror("madvise(MADV_SET_NONVOLATILE)");
        ASSERT_NOT_REACHED();
    }
#else
    int rc = 0;
#endif
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

Optional<BackingStore> Bitmap::allocate_backing_store(BitmapFormat format, const IntSize& size, Purgeable purgeable)
{
    if (size_would_overflow(format, size))
        return {};

    const auto pitch = minimum_pitch(size.width(), format);
    const auto data_size_in_bytes = size_in_bytes(pitch, size.height());

    void* data = nullptr;
#ifdef __serenity__
    int map_flags = purgeable == Purgeable::Yes ? (MAP_PURGEABLE | MAP_PRIVATE) : (MAP_ANONYMOUS | MAP_PRIVATE);
    data = mmap_with_name(nullptr, data_size_in_bytes, PROT_READ | PROT_WRITE, map_flags, 0, 0, String::format("GraphicsBitmap [%dx%d]", size.width(), size.height()).characters());
#else
    UNUSED_PARAM(purgeable);
    int map_flags = (MAP_ANONYMOUS | MAP_PRIVATE);
    data = mmap(nullptr, data_size_in_bytes, PROT_READ | PROT_WRITE, map_flags, 0, 0);
#endif
    if (data == MAP_FAILED) {
        perror("mmap");
        return {};
    }
    return { { data, pitch, data_size_in_bytes } };
}

void Bitmap::allocate_palette_from_format(BitmapFormat format, const Vector<RGBA32>& source_palette)
{
    size_t size = palette_size(format);
    if (size == 0)
        return;
    m_palette = new RGBA32[size];
    if (!source_palette.is_empty()) {
        ASSERT(source_palette.size() == size);
        memcpy(m_palette, source_palette.data(), size * sizeof(RGBA32));
    }
}

Vector<RGBA32> Bitmap::palette_to_vector() const
{
    Vector<RGBA32> vector;
    auto size = palette_size(m_format);
    vector.ensure_capacity(size);
    for (size_t i = 0; i < size; ++i)
        vector.unchecked_append(palette_color(i).value());
    return vector;
}

}
