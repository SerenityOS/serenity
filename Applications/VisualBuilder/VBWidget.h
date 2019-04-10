#pragma once

#include <SharedGraphics/Rect.h>

class VBWidget {
public:
    VBWidget();
    virtual ~VBWidget();

    Rect rect() const { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

private:
    Rect m_rect;
};
