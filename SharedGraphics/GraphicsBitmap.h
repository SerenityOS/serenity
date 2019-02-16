#pragma once

#include "Color.h"
#include "Rect.h"
#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
    static RetainPtr<GraphicsBitmap> create(const Size&);
    static RetainPtr<GraphicsBitmap> create_wrapper(const Size&, RGBA32*);
    static RetainPtr<GraphicsBitmap> load_from_file(const String& path, const Size&);
    static RetainPtr<GraphicsBitmap> create_with_shared_buffer(int shared_buffer_id, const Size&, RGBA32* buffer = nullptr);
    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    Rect rect() const { return { {}, m_size }; }
    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }
    int shared_buffer_id() const { return m_shared_buffer_id; }

private:
    GraphicsBitmap(const Size&);
    GraphicsBitmap(const Size&, RGBA32*);
    GraphicsBitmap(int shared_buffer_id, const Size&, RGBA32*);

    Size m_size;
    RGBA32* m_data { nullptr };
    size_t m_pitch { 0 };
    bool m_mmaped { false };
    int m_shared_buffer_id { -1 };
};

inline RGBA32* GraphicsBitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * m_pitch)));
}

inline const RGBA32* GraphicsBitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const byte*)m_data) + (y * m_pitch)));
}
