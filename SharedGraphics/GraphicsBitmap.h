#pragma once

#include "Color.h"
#include "Rect.h"
#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>
#include <SharedBuffer.h>

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
    enum class Format { Invalid, RGB32, RGBA32 };

    static Retained<GraphicsBitmap> create(Format, const Size&);
    static Retained<GraphicsBitmap> create_wrapper(Format, const Size&, RGBA32*);
    static RetainPtr<GraphicsBitmap> load_from_file(Format, const String& path, const Size&);
    static Retained<GraphicsBitmap> create_with_shared_buffer(Format, Retained<SharedBuffer>&&, const Size&);
    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    Rect rect() const { return { {}, m_size }; }
    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }
    int shared_buffer_id() const { return m_shared_buffer ? m_shared_buffer->shared_buffer_id() : -1; }

    bool has_alpha_channel() const { return m_format == Format::RGBA32; }

private:
    GraphicsBitmap(Format, const Size&);
    GraphicsBitmap(Format, const Size&, RGBA32*);
    GraphicsBitmap(Format, Retained<SharedBuffer>&&, const Size&);

    Size m_size;
    RGBA32* m_data { nullptr };
    size_t m_pitch { 0 };
    Format m_format { Format::Invalid };
    bool m_mmaped { false };
    RetainPtr<SharedBuffer> m_shared_buffer;
};

inline RGBA32* GraphicsBitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * m_pitch)));
}

inline const RGBA32* GraphicsBitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const byte*)m_data) + (y * m_pitch)));
}
