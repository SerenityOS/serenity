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
    explicit Painter(Widget&);
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void fill_rect(const Rect&, Color);
    void draw_rect(const Rect&, Color);
    void draw_bitmap(const Point&, const CharacterBitmap&, Color = Color());
    void set_pixel(const Point&, Color);
    void draw_line(const Point&, const Point&, Color);
    void draw_focus_rect(const Rect&);
    void blit(const Point&, const GraphicsBitmap&);

    enum class TextAlignment { TopLeft, CenterLeft, Center };
    void draw_text(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, Color = Color());

    const Font& font() const { return *m_font; }

    enum class DrawOp { Copy, Xor };
    void set_draw_op(DrawOp op) { m_draw_op = op; }
    DrawOp draw_op() const { return m_draw_op; }

private:
    void set_pixel_with_draw_op(dword& pixel, const Color&);

    const Font* m_font;
    Point m_translation;
    Rect m_clip_rect;
    RetainPtr<GraphicsBitmap> m_target;
    Window* m_window { nullptr };
    DrawOp m_draw_op { DrawOp::Copy };
};
