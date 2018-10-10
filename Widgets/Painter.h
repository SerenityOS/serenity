#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include <AK/String.h>

class Widget;

class Painter {
public:
    explicit Painter(Widget&);
    ~Painter();
    void fillRect(const Rect&, Color);
    void drawText(const Point&, const String&, const Color& = Color());

private:
    Widget& m_widget;
};
