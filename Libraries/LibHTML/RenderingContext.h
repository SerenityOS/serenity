#pragma once

class GPainter;

class RenderingContext {
public:
    explicit RenderingContext(GPainter& painter)
        : m_painter(painter)
    {
    }

    GPainter& painter() const { return m_painter; }

private:
    GPainter& m_painter;
};
