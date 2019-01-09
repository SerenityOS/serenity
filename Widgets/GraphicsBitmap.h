#pragma once

#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
    static RetainPtr<GraphicsBitmap> create(const Size&);
    static RetainPtr<GraphicsBitmap> create_wrapper(const Size&, byte*);
    ~GraphicsBitmap();

    dword* scanline(int y);

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

private:
    explicit GraphicsBitmap(const Size&);
    GraphicsBitmap(const Size&, byte*);

    Size m_size;
    byte* m_data { nullptr };
    bool m_owned { false };
};
