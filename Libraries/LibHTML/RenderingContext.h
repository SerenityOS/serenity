#pragma once

#include <LibDraw/Palette.h>
#include <LibDraw/Rect.h>

class GPainter;

class RenderingContext {
public:
    explicit RenderingContext(GPainter& painter, const Palette& palette)
        : m_painter(painter)
        , m_palette(palette)
    {
    }

    GPainter& painter() const { return m_painter; }
    const Palette& palette() const { return m_palette; }

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Rect viewport_rect() const { return m_viewport_rect; }
    void set_viewport_rect(const Rect& rect) { m_viewport_rect = rect; }

private:
    GPainter& m_painter;
    const Palette& m_palette;
    Rect m_viewport_rect;
    bool m_should_show_line_box_borders { false };
};
