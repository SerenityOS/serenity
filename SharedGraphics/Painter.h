#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/AKString.h>

class CharacterBitmap;
class GraphicsBitmap;
class Font;

#ifdef LIBGUI
class GWidget;
class GWindow;
#endif

class Painter {
public:
#ifdef LIBGUI
    explicit Painter(GWidget&);
#endif
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
    void draw_glyph(const Point&, char, Color);

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
#ifdef LIBGUI
    GWindow* m_window { nullptr };
#endif
    DrawOp m_draw_op { DrawOp::Copy };
};
