#pragma once

#include "Color.h"
#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class Region;

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
    static RetainPtr<GraphicsBitmap> create(const Size&);
    static RetainPtr<GraphicsBitmap> create_wrapper(const Size&, RGBA32*);
    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }

private:
    explicit GraphicsBitmap(const Size&);
    GraphicsBitmap(const Size&, RGBA32*);

    Size m_size;
    RGBA32* m_data { nullptr };
    size_t m_pitch { 0 };
#ifdef SERENITY
    Region* m_region { nullptr };
#endif
    bool m_owned { false };
};
