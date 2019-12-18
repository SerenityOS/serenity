#pragma once

#include "Color.h"
#include "Rect.h"
#include "Size.h"
#include <AK/MappedFile.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <SharedBuffer.h>

class GraphicsBitmap : public RefCounted<GraphicsBitmap> {
public:
    enum class Format {
        Invalid,
        RGB32,
        RGBA32,
        Indexed8
    };

    static NonnullRefPtr<GraphicsBitmap> create(Format, const Size&);
    static NonnullRefPtr<GraphicsBitmap> create_purgeable(Format, const Size&);
    static NonnullRefPtr<GraphicsBitmap> create_wrapper(Format, const Size&, size_t pitch, RGBA32*);
    static RefPtr<GraphicsBitmap> load_from_file(const StringView& path);
    static RefPtr<GraphicsBitmap> load_from_file(Format, const StringView& path, const Size&);
    static NonnullRefPtr<GraphicsBitmap> create_with_shared_buffer(Format, NonnullRefPtr<SharedBuffer>&&, const Size&);

    NonnullRefPtr<GraphicsBitmap> to_shareable_bitmap() const;

    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    u8* bits(int y);
    const u8* bits(int y) const;

    Rect rect() const { return { {}, m_size }; }
    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }
    int shared_buffer_id() const { return m_shared_buffer ? m_shared_buffer->shared_buffer_id() : -1; }

    SharedBuffer* shared_buffer() { return m_shared_buffer.ptr(); }
    const SharedBuffer* shared_buffer() const { return m_shared_buffer.ptr(); }

    unsigned bpp() const
    {
        switch (m_format) {
        case Format::Indexed8:
            return 8;
        case Format::RGB32:
        case Format::RGBA32:
            return 32;
        default:
            ASSERT_NOT_REACHED();
        case Format::Invalid:
            return 0;
        }
    }

    void fill(Color);

    bool has_alpha_channel() const { return m_format == Format::RGBA32; }
    Format format() const { return m_format; }

    void set_mmap_name(const StringView&);

    size_t size_in_bytes() const { return m_pitch * m_size.height(); }

    Color palette_color(u8 index) const { return Color::from_rgba(m_palette[index]); }
    void set_palette_color(u8 index, Color color) { m_palette[index] = color.value(); }

    template<Format>
    Color get_pixel(int x, int y) const
    {
        ASSERT_NOT_REACHED();
    }

    Color get_pixel(int x, int y) const;

    Color get_pixel(const Point& position) const
    {
        return get_pixel(position.x(), position.y());
    }

    template<Format>
    void set_pixel(int x, int y, Color)
    {
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
    enum class Purgeable { No, Yes };
    GraphicsBitmap(Format, const Size&, Purgeable);
    GraphicsBitmap(Format, const Size&, size_t pitch, RGBA32*);
    GraphicsBitmap(Format, const Size&, MappedFile&&);
    GraphicsBitmap(Format, NonnullRefPtr<SharedBuffer>&&, const Size&);

    Size m_size;
    RGBA32* m_data { nullptr };
    RGBA32* m_palette { nullptr };
    size_t m_pitch { 0 };
    Format m_format { Format::Invalid };
    bool m_needs_munmap { false };
    bool m_purgeable { false };
    bool m_volatile { false };
    MappedFile m_mapped_file;
    RefPtr<SharedBuffer> m_shared_buffer;
};

inline RGBA32* GraphicsBitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((u8*)m_data) + (y * m_pitch)));
}

inline const RGBA32* GraphicsBitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const u8*)m_data) + (y * m_pitch)));
}

inline const u8* GraphicsBitmap::bits(int y) const
{
    return reinterpret_cast<const u8*>(scanline(y));
}

inline u8* GraphicsBitmap::bits(int y)
{
    return reinterpret_cast<u8*>(scanline(y));
}

template<>
inline Color GraphicsBitmap::get_pixel<GraphicsBitmap::Format::RGB32>(int x, int y) const
{
    return Color::from_rgb(scanline(y)[x]);
}

template<>
inline Color GraphicsBitmap::get_pixel<GraphicsBitmap::Format::RGBA32>(int x, int y) const
{
    return Color::from_rgba(scanline(y)[x]);
}

template<>
inline Color GraphicsBitmap::get_pixel<GraphicsBitmap::Format::Indexed8>(int x, int y) const
{
    return Color::from_rgba(m_palette[bits(y)[x]]);
}

inline Color GraphicsBitmap::get_pixel(int x, int y) const
{
    switch (m_format) {
    case Format::RGB32:
        return get_pixel<Format::RGB32>(x, y);
    case Format::RGBA32:
        return get_pixel<Format::RGBA32>(x, y);
    case Format::Indexed8:
        return get_pixel<Format::Indexed8>(x, y);
    default:
        ASSERT_NOT_REACHED();
        return {};
    }
}

template<>
inline void GraphicsBitmap::set_pixel<GraphicsBitmap::Format::RGB32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value();
}

template<>
inline void GraphicsBitmap::set_pixel<GraphicsBitmap::Format::RGBA32>(int x, int y, Color color)
{
    scanline(y)[x] = color.value();
}

inline void GraphicsBitmap::set_pixel(int x, int y, Color color)
{
    switch (m_format) {
    case Format::RGB32:
        set_pixel<Format::RGB32>(x, y, color);
        break;
    case Format::RGBA32:
        set_pixel<Format::RGBA32>(x, y, color);
        break;
    case Format::Indexed8:
        ASSERT_NOT_REACHED();
    default:
        ASSERT_NOT_REACHED();
    }
}
