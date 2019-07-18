#pragma once

#include <LibDraw/Painter.h>

class GWidget;
class GraphicsBitmap;

class GPainter : public Painter {
public:
    explicit GPainter(GWidget&);
    explicit GPainter(GraphicsBitmap&);
};
