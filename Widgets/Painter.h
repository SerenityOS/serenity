#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/AKString.h>

class CharacterBitmap;
class GraphicsBitmap;
class Font;
class Widget;
class Window;

class Painter {
public:
    enum class TextAlignment { TopLeft, CenterLeft, Center };
    explicit Painter(Widget&);
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void fillRect(const Rect&, Color);
    void drawRect(const Rect&, Color);
    void drawText(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, Color = Color());
    void drawBitmap(const Point&, const CharacterBitmap&, Color = Color());
    void drawPixel(const Point&, Color);
    void drawLine(const Point& p1, const Point& p2, Color);

    void drawFocusRect(const Rect&);

    void xorRect(const Rect&, Color);

    const Font& font() const { return *m_font; }

    enum class DrawOp { Copy, Xor };
    void set_draw_op(DrawOp op) { m_draw_op = op; }
    DrawOp draw_op() const { return m_draw_op; }

private:
    void set_pixel_with_draw_op(dword& pixel, const Color&);

    const Font* m_font;
    Point m_translation;
    Rect m_clipRect;
    RetainPtr<GraphicsBitmap> m_target;
    Window* m_window { nullptr };
    DrawOp m_draw_op { DrawOp::Copy };
};
