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
    BGRx8888,
    BGRA8888,
    RGBA8888,
};

inline bool is_valid_bitmap_format(unsigned format)
{
    switch (format) {
    case (unsigned)BitmapFormat::Invalid:
    case (unsigned)BitmapFormat::Indexed1:
    case (unsigned)BitmapFormat::Indexed2:
    case (unsigned)BitmapFormat::Indexed4:
    case (unsigned)BitmapFormat::Indexed8:
    case (unsigned)BitmapFormat::BGRx8888:
    case (unsigned)BitmapFormat::BGRA8888:
    case (unsigned)BitmapFormat::RGBA8888:
        return true;
    }
    return false;
}

enum class StorageFormat {
    Indexed8,
    BGRx8888,
    BGRA8888,
    RGBA8888,
};

static StorageFormat determine_storage_format(BitmapFormat format)
{
    switch (format) {
    case BitmapFormat::BGRx8888:
        return StorageFormat::BGRx8888;
    case BitmapFormat::BGRA8888:
        return StorageFormat::BGRA8888;
    case BitmapFormat::RGBA8888:
        return StorageFormat::RGBA8888;
    case BitmapFormat::Indexed1:
    case BitmapFormat::Indexed2:
    case BitmapFormat::Indexed4:
    case BitmapFormat::Indexed8:
        return StorageFormat::Indexed8;
    default:
        VERIFY_NOT_REACHED();
    }
}

struct BackingStore;

enum RotationDirection {
    Left,
    Right
};

class Bitmap : public RefCounted<Bitmap> {
public:
    enum class ShouldCloseAnonymousFile {
        No,
        Yes,
    };

    static RefPtr<Bitmap> create(BitmapFormat, const IntSize&, int intrinsic_scale = 1);
    static RefPtr<Bitmap> create_shareable(BitmapFormat, const IntSize&, int intrinsic_scale = 1);
    static RefPtr<Bitmap> create_purgeable(BitmapFormat, const IntSize&, int intrinsic_scale = 1);
    static RefPtr<Bitmap> create_wrapper(BitmapFormat, const IntSize&, int intrinsic_scale, size_t pitch, void*);
    static RefPtr<Bitmap> load_from_file(const StringView& path, int scale_factor = 1);
    static RefPtr<Bitmap> create_with_anon_fd(BitmapFormat, int anon_fd, const IntSize&, int intrinsic_scale, const Vector<RGBA32>& palette, ShouldCloseAnonymousFile);
    static RefPtr<Bitmap> create_from_serialized_byte_buffer(ByteBuffer&& buffer);
    static bool is_path_a_supported_image_format(const StringView& path)
    {
#define __ENUMERATE_IMAGE_FORMAT(Name, Ext)                    \
    if (path.ends_with(Ext, CaseSensitivity::CaseInsensitive)) \
        return true;
        ENUMERATE_IMAGE_FORMATS
#undef __ENUMERATE_IMAGE_FORMAT

        return false;
    }

    RefPtr<Gfx::Bitmap> clone() const;

    RefPtr<Gfx::Bitmap> rotated(Gfx::RotationDirection) const;
    RefPtr<Gfx::Bitmap> flipped(Gfx::Orientation) const;
    RefPtr<Bitmap> to_bitmap_backed_by_anon_fd() const;
    ByteBuffer serialize_to_byte_buffer() const;

    ShareableBitmap to_shareable_bitmap() const;

    ~Bitmap();

    u8* scanline_u8(int physical_y);
    const u8* scanline_u8(int physical_y) const;
    RGBA32* scanline(int physical_y);
    const RGBA32* scanline(int physical_y) const;

    IntRect rect() const { return { {}, m_size }; }
    IntSize size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    int scale() const { return m_scale; }

    IntRect physical_rect() const { return rect() * scale(); }
    IntSize physical_size() const { return size() * scale(); }
    int physical_width() const { return physical_size().width(); }
    int physical_height() const { return physical_size().height(); }
    size_t pitch() const { return m_pitch; }

    ALWAYS_INLINE bool is_indexed() const
    {
        return is_indexed(m_format);
    }

    ALWAYS_INLINE static bool is_indexed(BitmapFormat format)
    {
        return format == BitmapFormat::Indexed8 || format == BitmapFormat::Indexed4
            || format == BitmapFormat::Indexed2 || format == BitmapFormat::Indexed1;
    }

    static size_t palette_size(BitmapFormat format)
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
        case BitmapFormat::BGRx8888:
        case BitmapFormat::BGRA8888:
            return 32;
        default:
            VERIFY_NOT_REACHED();
        case BitmapFormat::Invalid:
            return 0;
        }
    }

    static size_t minimum_pitch(size_t physical_width, BitmapFormat);

    unsigned bpp() const
    {
        return bpp_for_format(m_format);
    }

    void fill(Color);

    bool has_alpha_channel() const { return m_format == BitmapFormat::BGRA8888; }
    BitmapFormat format() const { return m_format; }

    void set_mmap_name(const StringView&);

    static constexpr size_t size_in_bytes(size_t pitch, int physical_height) { return pitch * physical_height; }
    size_t size_in_bytes() const { return size_in_bytes(m_pitch, physical_height()); }

    Color palette_color(u8 index) const { return Color::from_rgba(m_palette[index]); }
    void set_palette_color(u8 index, Color color) { m_palette[index] = color.value(); }

    template<StorageFormat>
    Color get_pixel(int physical_x, int physical_y) const;
    Color get_pixel(int physical_x, int physical_y) const;
    Color get_pixel(const IntPoint& physical_position) const
    {
        return get_pixel(physical_position.x(), physical_position.y());
    }

    template<StorageFormat>
    void set_pixel(int physical_x, int physical_y, Color);
    void set_pixel(int physical_x, int physical_y, Color);
    void set_pixel(const IntPoint& physical_position, Color color)
    {
        set_pixel(physical_position.x(), physical_position.y(), color);
    }

    bool is_purgeable() const { return m_purgeable; }
    bool is_volatile() const { return m_volatile; }
    void set_volatile();
    [[nodiscard]] bool set_nonvolatile();

    int anon_fd() const { return m_anon_fd; }

private:
    enum class Purgeable {
        No,
        Yes
    };
    Bitmap(BitmapFormat, const IntSize&, int, Purgeable, const BackingStore&);
    Bitmap(BitmapFormat, const IntSize&, int, size_t pitch, void*);
    Bitmap(BitmapFormat, int anon_fd, const IntSize&, int, void*, const Vector<RGBA32>& palette);

    static Optional<BackingStore> allocate_backing_store(BitmapFormat, const IntSize&, int, Purgeable);

    void allocate_palette_from_format(BitmapFormat, const Vector<RGBA32>& source_palette);

    IntSize m_size;
    int m_scale;
    void* m_data { nullptr };
    RGBA32* m_palette { nullptr };
    size_t m_pitch { 0 };
    BitmapFormat m_format { BitmapFormat::Invalid };
    bool m_needs_munmap { false };
    bool m_purgeable { false };
    bool m_volatile { false };
    int m_anon_fd { -1 };
};

inline u8* Bitmap::scanline_u8(int y)
{
    VERIFY(y >= 0 && y < physical_height());
    return reinterpret_cast<u8*>(m_data) + (y * m_pitch);
}

inline const u8* Bitmap::scanline_u8(int y) const
{
    VERIFY(y >= 0 && y < physical_height());
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
inline Color Bitmap::get_pixel<StorageFormat::BGRx8888>(int x, int y) const
{
    VERIFY(x >= 0 && x < physical_width());
    return Color::from_rgb(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<StorageFormat::BGRA8888>(int x, int y) const
{
    VERIFY(x >= 0 && x < physical_width());
    return Color::from_rgba(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<StorageFormat::Indexed8>(int x, int y) const
{
    VERIFY(x >= 0 && x < physical_width());
    return Color::from_rgb(m_palette[scanline_u8(y)[x]]);
}

inline Color Bitmap::get_pixel(int x, int y) const
{
    switch (determine_storage_format(m_format)) {
    case StorageFormat::BGRx8888:
        return get_pixel<StorageFormat::BGRx8888>(x, y);
    case StorageFormat::BGRA8888:
        return get_pixel<StorageFormat::BGRA8888>(x, y);
    case StorageFormat::Indexed8:
        return get_pixel<StorageFormat::Indexed8>(x, y);
    default:
        VERIFY_NOT_REACHED();
    }
}

template<>
inline void Bitmap::set_pixel<StorageFormat::BGRx8888>(int x, int y, Color color)
{
    VERIFY(x >= 0 && x < physical_width());
    scanline(y)[x] = color.value();
}
template<>
inline void Bitmap::set_pixel<StorageFormat::BGRA8888>(int x, int y, Color color)
{
    VERIFY(x >= 0 && x < physical_width());
    scanline(y)[x] = color.value(); // drop alpha
}
inline void Bitmap::set_pixel(int x, int y, Color color)
{
    switch (determine_storage_format(m_format)) {
    case StorageFormat::BGRx8888:
        set_pixel<StorageFormat::BGRx8888>(x, y, color);
        break;
    case StorageFormat::BGRA8888:
        set_pixel<StorageFormat::BGRA8888>(x, y, color);
        break;
    case StorageFormat::Indexed8:
        VERIFY_NOT_REACHED();
    default:
        VERIFY_NOT_REACHED();
    }
}

}
