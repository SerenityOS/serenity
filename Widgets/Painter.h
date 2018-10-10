#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include <AK/String.h>

class Widget;

class Painter {
public:
    enum class TextAlignment { TopLeft, Center };
    explicit Painter(Widget&);
    ~Painter();
    void fillRect(const Rect&, Color);
    void drawText(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, const Color& = Color());

private:
    Widget& m_widget;
};
