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

#pragma once

#include <AK/Forward.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

#define ENUMERATE_IMAGE_FORMATS           \
    __ENUMERATE_IMAGE_FORMAT(pbm, ".pbm") \
    __ENUMERATE_IMAGE_FORMAT(pgm, ".pgm") \
    __ENUMERATE_IMAGE_FORMAT(png, ".png") \
    __ENUMERATE_IMAGE_FORMAT(ppm, ".ppm") \
    __ENUMERATE_IMAGE_FORMAT(gif, ".gif") \
    __ENUMERATE_IMAGE_FORMAT(bmp, ".bmp") \
    __ENUMERATE_IMAGE_FORMAT(ico, ".ico") \
    __ENUMERATE_IMAGE_FORMAT(jpg, ".jpg") \
    __ENUMERATE_IMAGE_FORMAT(jpg, ".jpeg")

namespace Gfx {

enum class BitmapFormat {
    Invalid,
    Indexed1,
    Indexed2,
    Indexed4,
    Indexed8,
    RGB32,
    RGBA32,
};

enum class StorageFormat {
    Indexed8,
    RGB32,
    RGBA32,
};

static StorageFormat determine_storage_format(BitmapFormat format)
{
    switch (format) {
    case BitmapFormat::RGB32:
        return StorageFormat::RGB32;
    case BitmapFormat::RGBA32:
        return StorageFormat::RGBA32;
    case BitmapFormat::Indexed1:
    case BitmapFormat::Indexed2:
    case BitmapFormat::Indexed4:
    case BitmapFormat::Indexed8:
        return StorageFormat::Indexed8;
    default:
        ASSERT_NOT_REACHED();
    }
}

struct BackingStore;

enum RotationDirection {
    Left,
    Right
};

class Bitmap : public RefCounted<Bitmap> {
public:
    static RefPtr<Bitmap> create(BitmapFormat, const IntSize&);
    static RefPtr<Bitmap> create_purgeable(BitmapFormat, const IntSize&);
    static RefPtr<Bitmap> create_wrapper(BitmapFormat, const IntSize&, size_t pitch, void*);
    static RefPtr<Bitmap> load_from_file(const StringView& path);
    static RefPtr<Bitmap> create_with_shared_buffer(BitmapFormat, NonnullRefPtr<SharedBuffer>&&, const IntSize&);
    static RefPtr<Bitmap> create_with_shared_buffer(BitmapFormat, NonnullRefPtr<SharedBuffer>&&, const IntSize&, const Vector<RGBA32>& palette);
    static bool is_path_a_supported_image_format(const StringView& path)
    {
#define __ENUMERATE_IMAGE_FORMAT(Name, Ext) \
    if (path.ends_with(Ext))                \
        return true;
        ENUMERATE_IMAGE_FORMATS
#undef __ENUMERATE_IMAGE_FORMAT

        return false;
    }

    RefPtr<Gfx::Bitmap> clone() const;

    RefPtr<Gfx::Bitmap> rotated(Gfx::RotationDirection) const;
    RefPtr<Gfx::Bitmap> flipped(Gfx::Orientation) const;
    RefPtr<Bitmap> to_bitmap_backed_by_shared_buffer() const;

    ShareableBitmap to_shareable_bitmap(pid_t peer_pid = -1) const;

    ~Bitmap();

    u8* scanline_u8(int y);
    const u8* scanline_u8(int y) const;
    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    IntRect rect() const { return { {}, m_size }; }
    IntSize size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }
    int shbuf_id() const;

    SharedBuffer* shared_buffer() { return m_shared_buffer.ptr(); }
    const SharedBuffer* shared_buffer() const { return m_shared_buffer.ptr(); }

    ALWAYS_INLINE bool is_indexed() const
    {
        return is_indexed(m_format);
    }

    ALWAYS_INLINE static bool is_indexed(BitmapFormat format)
    {
        return format == BitmapFormat::Indexed8 || format == BitmapFormat::Indexed4
            || format == BitmapFormat::Indexed2 || format == BitmapFormat::Indexed1;
    }

    size_t palette_size(BitmapFormat format) const
    {
        switch (format) {
        case BitmapFormat::Indexed1:
            return 2;
        case BitmapFormat::Indexed2:
            return 4;
        case BitmapFormat::Indexed4:
            return 16;
        case BitmapFormat::Indexed8:
            return 256;
        default:
            return 0;
        }
    }

    Vector<RGBA32> palette_to_vector() const;

    static unsigned bpp_for_format(BitmapFormat format)
    {
        switch (format) {
        case BitmapFormat::Indexed1:
            return 1;
        case BitmapFormat::Indexed2:
            return 2;
        case BitmapFormat::Indexed4:
            return 4;
        case BitmapFormat::Indexed8:
            return 8;
        case BitmapFormat::RGB32:
        case BitmapFormat::RGBA32:
            return 32;
        default:
            ASSERT_NOT_REACHED();
        case BitmapFormat::Invalid:
            return 0;
        }
    }

    static size_t minimum_pitch(size_t width, BitmapFormat);

    unsigned bpp() const
    {
        return bpp_for_format(m_format);
    }

    void fill(Color);

    bool has_alpha_channel() const { return m_format == BitmapFormat::RGBA32; }
    BitmapFormat format() const { return m_format; }

    void set_mmap_name(const StringView&);

    static constexpr size_t size_in_bytes(size_t pitch, int height) { return pitch * height; }
    size_t size_in_bytes() const { return size_in_bytes(m_pitch, height()); }

    Color palette_color(u8 index) const { return Color::from_rgba(m_palette[index]); }
    void set_palette_color(u8 index, Color color) { m_palette[index] = color.value(); }

    template<StorageFormat>
    Color get_pixel(int x, int y) const;
    Color get_pixel(int x, int y) const;
    Color get_pixel(const IntPoint& position) const
    {
        return get_pixel(position.x(), position.y());
    }

    template<StorageFormat>
    void set_pixel(int x, int y, Color);
    void set_pixel(int x, int y, Color);
    void set_pixel(const IntPoint& position, Color color)
    {
        set_pixel(position.x(), position.y(), color);
    }

    bool is_purgeable() const { return m_purgeable; }
    bool is_volatile() const { return m_volatile; }
    void set_volatile();
    [[nodiscard]] bool set_nonvolatile();

private:
    enum class Purgeable {
        No,
        Yes
    };
    Bitmap(BitmapFormat, const IntSize&, Purgeable, const BackingStore&);
    Bitmap(BitmapFormat, const IntSize&, size_t pitch, void*);
    Bitmap(BitmapFormat, NonnullRefPtr<SharedBuffer>&&, const IntSize&, const Vector<RGBA32>& palette);

    static Optional<BackingStore> allocate_backing_store(BitmapFormat, const IntSize&, Purgeable);

    void allocate_palette_from_format(BitmapFormat, const Vector<RGBA32>& source_palette);

    IntSize m_size;
    void* m_data { nullptr };
    RGBA32* m_palette { nullptr };
    size_t m_pitch { 0 };
    BitmapFormat m_format { BitmapFormat::Invalid };
    bool m_needs_munmap { false };
    bool m_purgeable { false };
    bool m_volatile { false };
    RefPtr<SharedBuffer> m_shared_buffer;
};

inline u8* Bitmap::scanline_u8(int y)
{
    return reinterpret_cast<u8*>(m_data) + (y * m_pitch);
}

inline const u8* Bitmap::scanline_u8(int y) const
{
    return reinterpret_cast<const u8*>(m_data) + (y * m_pitch);
}

inline RGBA32* Bitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>(scanline_u8(y));
}

inline const RGBA32* Bitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>(scanline_u8(y));
}

template<>
inline Color Bitmap::get_pixel<StorageFormat::RGB32>(int x, int y) const
{
    return Color::from_rgb(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<StorageFormat::RGBA32>(int x, int y) const
{
    return Color::from_rgba(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<StorageFormat::Indexed8>(int x, int y) const
{
    return Color::from_rgb(m_palette[scanline_u8(y)[x]]);
}

inline Color Bitmap::get_pixel(int x, int y) const
{
    switch (determine_storage_format(m_format)) {
    case StorageFormat::RGB32:
        return get_pixel<StorageFormat::RGB32>(x, y);
    case StorageFormat::RGBA32:
        return get_pixel<StorageFormat::RGBA32>(x, y);
    case StorageFormat::Indexed8:
        return get_pixel<StorageFormat::Indexed8>(x, y);
    default:
        ASSERT_NOT_REACHED();
    }
}

template<>
inline void Bitmap::set_pixel<StorageFormat::RGB32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value();
}
template<>
inline void Bitmap::set_pixel<StorageFormat::RGBA32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value(); // drop alpha
}
inline void Bitmap::set_pixel(int x, int y, Color color)
{
    switch (determine_storage_format(m_format)) {
    case StorageFormat::RGB32:
        set_pixel<StorageFormat::RGB32>(x, y, color);
        break;
    case StorageFormat::RGBA32:
        set_pixel<StorageFormat::RGBA32>(x, y, color);
        break;
    case StorageFormat::Indexed8:
        ASSERT_NOT_REACHED();
    default:
        ASSERT_NOT_REACHED();
    }
}

}
