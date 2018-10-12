#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/String.h>

class Font;
class Widget;

class Painter {
public:
    enum class TextAlignment { TopLeft, Center };
    explicit Painter(Widget&);
    ~Painter();
    void fillRect(const Rect&, Color);
    void drawRect(const Rect&, Color);
    void drawText(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, Color = Color());
    void drawBitmap(const Point&, const char* bitmap, const Size& bitmapSize, Color = Color());

    void xorRect(const Rect&, Color);

    const Font& font() const;

private:
    Widget& m_widget;
    Font& m_font;
    Point m_translation;
};
