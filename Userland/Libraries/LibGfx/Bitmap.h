/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/Forward.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

#define ENUMERATE_IMAGE_FORMATS             \
    __ENUMERATE_IMAGE_FORMAT(pbm, ".pbm")   \
    __ENUMERATE_IMAGE_FORMAT(pgm, ".pgm")   \
    __ENUMERATE_IMAGE_FORMAT(png, ".png")   \
    __ENUMERATE_IMAGE_FORMAT(ppm, ".ppm")   \
    __ENUMERATE_IMAGE_FORMAT(gif, ".gif")   \
    __ENUMERATE_IMAGE_FORMAT(bmp, ".bmp")   \
    __ENUMERATE_IMAGE_FORMAT(ico, ".ico")   \
    __ENUMERATE_IMAGE_FORMAT(iff, ".iff")   \
    __ENUMERATE_IMAGE_FORMAT(jpeg, ".jpg")  \
    __ENUMERATE_IMAGE_FORMAT(jpeg, ".jpeg") \
    __ENUMERATE_IMAGE_FORMAT(jxl, ".jxl")   \
    __ENUMERATE_IMAGE_FORMAT(dds, ".dds")   \
    __ENUMERATE_IMAGE_FORMAT(qoi, ".qoi")   \
    __ENUMERATE_IMAGE_FORMAT(tga, ".tga")   \
    __ENUMERATE_IMAGE_FORMAT(tvg, ".tvg")   \
    __ENUMERATE_IMAGE_FORMAT(tvg, ".webp")

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

inline StorageFormat determine_storage_format(BitmapFormat format)
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
    CounterClockwise,
    Clockwise
};

class Bitmap : public RefCounted<Bitmap> {
public:
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> create(BitmapFormat, IntSize, int intrinsic_scale = 1);
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> create_shareable(BitmapFormat, IntSize, int intrinsic_scale = 1);
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> create_wrapper(BitmapFormat, IntSize, int intrinsic_scale, size_t pitch, void*);
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> load_from_file(StringView path, int scale_factor = 1, Optional<IntSize> ideal_size = {});
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> load_from_file(NonnullOwnPtr<Core::File>, StringView path, Optional<IntSize> ideal_size = {});
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> load_from_bytes(ReadonlyBytes, Optional<IntSize> ideal_size = {}, Optional<DeprecatedString> mine_type = {});
    [[nodiscard]] static ErrorOr<NonnullRefPtr<Bitmap>> create_with_anonymous_buffer(BitmapFormat, Core::AnonymousBuffer, IntSize, int intrinsic_scale, Vector<ARGB32> const& palette);
    static ErrorOr<NonnullRefPtr<Bitmap>> create_from_serialized_bytes(ReadonlyBytes);
    static ErrorOr<NonnullRefPtr<Bitmap>> create_from_serialized_byte_buffer(ByteBuffer&&);

    static bool is_path_a_supported_image_format(StringView path)
    {
#define __ENUMERATE_IMAGE_FORMAT(Name, Ext)                        \
    if (path.ends_with(Ext##sv, CaseSensitivity::CaseInsensitive)) \
        return true;
        ENUMERATE_IMAGE_FORMATS
#undef __ENUMERATE_IMAGE_FORMAT

        return false;
    }

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> clone() const;

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> rotated(Gfx::RotationDirection) const;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> flipped(Gfx::Orientation) const;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> scaled(int sx, int sy) const;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> scaled(float sx, float sy) const;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> cropped(Gfx::IntRect, Optional<BitmapFormat> new_bitmap_format = {}) const;
    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> to_bitmap_backed_by_anonymous_buffer() const;
    [[nodiscard]] ErrorOr<ByteBuffer> serialize_to_byte_buffer() const;

    [[nodiscard]] ShareableBitmap to_shareable_bitmap() const;

    ErrorOr<NonnullRefPtr<Gfx::Bitmap>> inverted() const;

    ~Bitmap();

    [[nodiscard]] u8* scanline_u8(int physical_y);
    [[nodiscard]] u8 const* scanline_u8(int physical_y) const;
    [[nodiscard]] ARGB32* scanline(int physical_y);
    [[nodiscard]] ARGB32 const* scanline(int physical_y) const;

    [[nodiscard]] ARGB32* begin();
    [[nodiscard]] ARGB32* end();

    [[nodiscard]] IntRect rect() const { return { {}, m_size }; }
    [[nodiscard]] IntSize size() const { return m_size; }
    [[nodiscard]] int width() const { return m_size.width(); }
    [[nodiscard]] int height() const { return m_size.height(); }
    [[nodiscard]] int scale() const { return m_scale; }

    [[nodiscard]] IntRect physical_rect() const { return rect() * scale(); }
    [[nodiscard]] IntSize physical_size() const { return size() * scale(); }
    [[nodiscard]] int physical_width() const { return physical_size().width(); }
    [[nodiscard]] int physical_height() const { return physical_size().height(); }
    [[nodiscard]] size_t pitch() const { return m_pitch; }

    [[nodiscard]] ALWAYS_INLINE bool is_indexed() const
    {
        return is_indexed(m_format);
    }

    [[nodiscard]] ALWAYS_INLINE static bool is_indexed(BitmapFormat format)
    {
        return format == BitmapFormat::Indexed8 || format == BitmapFormat::Indexed4
            || format == BitmapFormat::Indexed2 || format == BitmapFormat::Indexed1;
    }

    [[nodiscard]] static size_t palette_size(BitmapFormat format)
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

    [[nodiscard]] Vector<ARGB32> palette_to_vector() const;

    [[nodiscard]] static unsigned bpp_for_format(BitmapFormat format)
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

    [[nodiscard]] static size_t minimum_pitch(size_t physical_width, BitmapFormat);

    [[nodiscard]] unsigned bpp() const
    {
        return bpp_for_format(m_format);
    }

    void fill(Color);

    [[nodiscard]] bool has_alpha_channel() const { return m_format == BitmapFormat::BGRA8888 || m_format == BitmapFormat::RGBA8888; }
    [[nodiscard]] BitmapFormat format() const { return m_format; }

    // Call only for BGRx8888 and BGRA8888 bitmaps.
    void strip_alpha_channel();

    void set_mmap_name(DeprecatedString const&);

    [[nodiscard]] static constexpr size_t size_in_bytes(size_t pitch, int physical_height) { return pitch * physical_height; }
    [[nodiscard]] size_t size_in_bytes() const { return size_in_bytes(m_pitch, physical_height()); }

    [[nodiscard]] Color palette_color(u8 index) const { return Color::from_argb(m_palette[index]); }
    void set_palette_color(u8 index, Color color) { m_palette[index] = color.value(); }

    template<StorageFormat>
    [[nodiscard]] Color get_pixel(int physical_x, int physical_y) const;
    [[nodiscard]] Color get_pixel(int physical_x, int physical_y) const;
    [[nodiscard]] Color get_pixel(IntPoint physical_position) const
    {
        return get_pixel(physical_position.x(), physical_position.y());
    }

    template<StorageFormat>
    void set_pixel(int physical_x, int physical_y, Color);
    void set_pixel(int physical_x, int physical_y, Color);
    void set_pixel(IntPoint physical_position, Color color)
    {
        set_pixel(physical_position.x(), physical_position.y(), color);
    }

    [[nodiscard]] bool is_volatile() const { return m_volatile; }
    void set_volatile();

    // Returns true if making the bitmap non-volatile succeeded. `was_purged` indicates status of contents.
    // Returns false if there was not enough memory.
    [[nodiscard]] bool set_nonvolatile(bool& was_purged);

    [[nodiscard]] Core::AnonymousBuffer& anonymous_buffer() { return m_buffer; }
    [[nodiscard]] Core::AnonymousBuffer const& anonymous_buffer() const { return m_buffer; }

    [[nodiscard]] bool visually_equals(Bitmap const&) const;

    [[nodiscard]] Optional<Color> solid_color(u8 alpha_threshold = 0) const;

    void flood_visit_from_point(Gfx::IntPoint start_point, int threshold, Function<void(Gfx::IntPoint location)> pixel_reached);

private:
    Bitmap(BitmapFormat, IntSize, int, BackingStore const&);
    Bitmap(BitmapFormat, IntSize, int, size_t pitch, void*);
    Bitmap(BitmapFormat, Core::AnonymousBuffer, IntSize, int, Vector<ARGB32> const& palette);

    static ErrorOr<BackingStore> allocate_backing_store(BitmapFormat format, IntSize size, int scale_factor);

    void allocate_palette_from_format(BitmapFormat, Vector<ARGB32> const& source_palette);

    IntSize m_size;
    int m_scale;
    void* m_data { nullptr };
    ARGB32* m_palette { nullptr };
    size_t m_pitch { 0 };
    BitmapFormat m_format { BitmapFormat::Invalid };
    bool m_needs_munmap { false };
    bool m_volatile { false };
    Core::AnonymousBuffer m_buffer;
};

ALWAYS_INLINE u8* Bitmap::scanline_u8(int y)
{
    VERIFY(y >= 0);
    VERIFY(y < physical_height());
    return reinterpret_cast<u8*>(m_data) + (y * m_pitch);
}

ALWAYS_INLINE u8 const* Bitmap::scanline_u8(int y) const
{
    VERIFY(y >= 0);
    VERIFY(y < physical_height());
    return reinterpret_cast<u8 const*>(m_data) + (y * m_pitch);
}

ALWAYS_INLINE ARGB32* Bitmap::scanline(int y)
{
    return reinterpret_cast<ARGB32*>(scanline_u8(y));
}

ALWAYS_INLINE ARGB32 const* Bitmap::scanline(int y) const
{
    return reinterpret_cast<ARGB32 const*>(scanline_u8(y));
}

ALWAYS_INLINE ARGB32* Bitmap::begin()
{
    return scanline(0);
}

ALWAYS_INLINE ARGB32* Bitmap::end()
{
    return reinterpret_cast<ARGB32*>(reinterpret_cast<u8*>(m_data) + (m_size.height() * m_pitch));
}

template<>
ALWAYS_INLINE Color Bitmap::get_pixel<StorageFormat::BGRx8888>(int x, int y) const
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    return Color::from_rgb(scanline(y)[x]);
}

template<>
ALWAYS_INLINE Color Bitmap::get_pixel<StorageFormat::BGRA8888>(int x, int y) const
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    return Color::from_argb(scanline(y)[x]);
}

template<>
ALWAYS_INLINE Color Bitmap::get_pixel<StorageFormat::Indexed8>(int x, int y) const
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    return Color::from_rgb(m_palette[scanline_u8(y)[x]]);
}

ALWAYS_INLINE Color Bitmap::get_pixel(int x, int y) const
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
ALWAYS_INLINE void Bitmap::set_pixel<StorageFormat::BGRx8888>(int x, int y, Color color)
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    scanline(y)[x] = color.value();
}

template<>
ALWAYS_INLINE void Bitmap::set_pixel<StorageFormat::BGRA8888>(int x, int y, Color color)
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    scanline(y)[x] = color.value(); // drop alpha
}

template<>
ALWAYS_INLINE void Bitmap::set_pixel<StorageFormat::RGBA8888>(int x, int y, Color color)
{
    VERIFY(x >= 0);
    VERIFY(x < physical_width());
    // FIXME: There's a lot of inaccurately named functions in the Color class right now (RGBA vs BGRA),
    //        clear those up and then make this more convenient.
    auto rgba = (color.alpha() << 24) | (color.blue() << 16) | (color.green() << 8) | color.red();
    scanline(y)[x] = rgba;
}

ALWAYS_INLINE void Bitmap::set_pixel(int x, int y, Color color)
{
    switch (determine_storage_format(m_format)) {
    case StorageFormat::BGRx8888:
        set_pixel<StorageFormat::BGRx8888>(x, y, color);
        break;
    case StorageFormat::BGRA8888:
        set_pixel<StorageFormat::BGRA8888>(x, y, color);
        break;
    case StorageFormat::RGBA8888:
        set_pixel<StorageFormat::RGBA8888>(x, y, color);
        break;
    case StorageFormat::Indexed8:
        VERIFY_NOT_REACHED();
    default:
        VERIFY_NOT_REACHED();
    }
}

}
