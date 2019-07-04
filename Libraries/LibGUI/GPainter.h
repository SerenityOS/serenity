#pragma once

#include <SharedGraphics/Painter.h>

class GWidget;
class GraphicsBitmap;

class GPainter : public Painter {
public:
    explicit GPainter(GWidget&);
    explicit GPainter(GraphicsBitmap&);
};
