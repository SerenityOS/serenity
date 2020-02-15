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
#include <AK/MappedFile.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace Gfx {

enum class BitmapFormat {
    Invalid,
    RGB32,
    RGBA32,
    Indexed8
};

class Bitmap : public RefCounted<Bitmap> {
public:
    static NonnullRefPtr<Bitmap> create(BitmapFormat, const Size&);
    static NonnullRefPtr<Bitmap> create_purgeable(BitmapFormat, const Size&);
    static NonnullRefPtr<Bitmap> create_wrapper(BitmapFormat, const Size&, size_t pitch, RGBA32*);
    static RefPtr<Bitmap> load_from_file(const StringView& path);
    static RefPtr<Bitmap> load_from_file(BitmapFormat, const StringView& path, const Size&);
    static NonnullRefPtr<Bitmap> create_with_shared_buffer(BitmapFormat, NonnullRefPtr<SharedBuffer>&&, const Size&);

    NonnullRefPtr<Bitmap> to_shareable_bitmap() const;

    ~Bitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    u8* bits(int y);
    const u8* bits(int y) const;

    Rect rect() const { return { {}, m_size }; }
    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }
    int shared_buffer_id() const;

    SharedBuffer* shared_buffer() { return m_shared_buffer.ptr(); }
    const SharedBuffer* shared_buffer() const { return m_shared_buffer.ptr(); }

    unsigned bpp() const
    {
        switch (m_format) {
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

    void fill(Color);

    bool has_alpha_channel() const { return m_format == BitmapFormat::RGBA32; }
    BitmapFormat format() const { return m_format; }

    void set_mmap_name(const StringView&);

    size_t size_in_bytes() const { return m_pitch * m_size.height(); }

    Color palette_color(u8 index) const { return Color::from_rgba(m_palette[index]); }
    void set_palette_color(u8 index, Color color) { m_palette[index] = color.value(); }

    template<BitmapFormat>
    Color get_pixel(int x, int y) const
    {
        (void)x;
        (void)y;
        ASSERT_NOT_REACHED();
    }

    Color get_pixel(int x, int y) const;

    Color get_pixel(const Point& position) const
    {
        return get_pixel(position.x(), position.y());
    }

    template<BitmapFormat>
    void set_pixel(int x, int y, Color)
    {
        (void)x;
        (void)y;
        ASSERT_NOT_REACHED();
    }

    void set_pixel(int x, int y, Color);

    void set_pixel(const Point& position, Color color)
    {
        set_pixel(position.x(), position.y(), color);
    }

    bool is_purgeable() const { return m_purgeable; }
    bool is_volatile() const { return m_volatile; }
    void set_volatile();
    [[nodiscard]] bool set_nonvolatile();

private:
    enum class Purgeable { No,
        Yes };
    Bitmap(BitmapFormat, const Size&, Purgeable);
    Bitmap(BitmapFormat, const Size&, size_t pitch, RGBA32*);
    Bitmap(BitmapFormat, const Size&, MappedFile&&);
    Bitmap(BitmapFormat, NonnullRefPtr<SharedBuffer>&&, const Size&);

    Size m_size;
    RGBA32* m_data { nullptr };
    RGBA32* m_palette { nullptr };
    size_t m_pitch { 0 };
    BitmapFormat m_format { BitmapFormat::Invalid };
    bool m_needs_munmap { false };
    bool m_purgeable { false };
    bool m_volatile { false };
    MappedFile m_mapped_file;
    RefPtr<SharedBuffer> m_shared_buffer;
};

inline RGBA32* Bitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((u8*)m_data) + (y * m_pitch)));
}

inline const RGBA32* Bitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const u8*)m_data) + (y * m_pitch)));
}

inline const u8* Bitmap::bits(int y) const
{
    return reinterpret_cast<const u8*>(scanline(y));
}

inline u8* Bitmap::bits(int y)
{
    return reinterpret_cast<u8*>(scanline(y));
}

template<>
inline Color Bitmap::get_pixel<BitmapFormat::RGB32>(int x, int y) const
{
    return Color::from_rgb(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<BitmapFormat::RGBA32>(int x, int y) const
{
    return Color::from_rgba(scanline(y)[x]);
}

template<>
inline Color Bitmap::get_pixel<BitmapFormat::Indexed8>(int x, int y) const
{
    return Color::from_rgba(m_palette[bits(y)[x]]);
}

inline Color Bitmap::get_pixel(int x, int y) const
{
    switch (m_format) {
    case BitmapFormat::RGB32:
        return get_pixel<BitmapFormat::RGB32>(x, y);
    case BitmapFormat::RGBA32:
        return get_pixel<BitmapFormat::RGBA32>(x, y);
    case BitmapFormat::Indexed8:
        return get_pixel<BitmapFormat::Indexed8>(x, y);
    default:
        ASSERT_NOT_REACHED();
        return {};
    }
}

template<>
inline void Bitmap::set_pixel<BitmapFormat::RGB32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value();
}

template<>
inline void Bitmap::set_pixel<BitmapFormat::RGBA32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value();
}

inline void Bitmap::set_pixel(int x, int y, Color color)
{
    switch (m_format) {
    case BitmapFormat::RGB32:
        set_pixel<BitmapFormat::RGB32>(x, y, color);
        break;
    case BitmapFormat::RGBA32:
        set_pixel<BitmapFormat::RGBA32>(x, y, color);
        break;
    case BitmapFormat::Indexed8:
        ASSERT_NOT_REACHED();
    default:
        ASSERT_NOT_REACHED();
    }
}

}
