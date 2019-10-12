#pragma once

class GPainter;

class RenderingContext {
public:
    explicit RenderingContext(GPainter& painter)
        : m_painter(painter)
    {
    }

    GPainter& painter() const { return m_painter; }

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

private:
    GPainter& m_painter;
    bool m_should_show_line_box_borders { false };
};
