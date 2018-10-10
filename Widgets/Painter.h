#pragma once

#include "Color.h"
#include "Rect.h"

class Widget;

class Painter {
public:
    explicit Painter(Widget&);
    ~Painter();
    void fillRect(Rect, Color);

private:
    Widget& m_widget;
};
