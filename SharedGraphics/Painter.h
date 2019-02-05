#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/AKString.h>

class CharacterBitmap;
class GlyphBitmap;
class GraphicsBitmap;
class Font;

#ifndef KERNEL
class GWidget;
class GWindow;
#endif

class Painter {
public:
#ifndef KERNEL
    explicit Painter(GWidget&);
#endif
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void fill_rect(const Rect&, Color);
    void fill_rect_with_gradient(const Rect&, Color gradient_start, Color gradient_end);
    void draw_rect(const Rect&, Color);
    void draw_bitmap(const Point&, const CharacterBitmap&, Color = Color());
    void draw_bitmap(const Point&, const GlyphBitmap&, Color = Color());
    void set_pixel(const Point&, Color);
    void draw_line(const Point&, const Point&, Color);
    void draw_focus_rect(const Rect&);
    void blit(const Point&, const GraphicsBitmap&, const Rect& src_rect);

    enum class TextAlignment { TopLeft, CenterLeft, Center, CenterRight };
    void draw_text(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, Color = Color());
    void draw_glyph(const Point&, char, Color);

    const Font& font() const { return *m_font; }
    void set_font(Font& font) { m_font = &font; }

    enum class DrawOp { Copy, Xor };
    void set_draw_op(DrawOp op) { m_draw_op = op; }
    DrawOp draw_op() const { return m_draw_op; }

    void set_clip_rect(const Rect& rect);
    void clear_clip_rect();
    Rect clip_rect() const { return m_clip_rect; }

    void translate(int dx, int dy) { m_translation.move_by(dx, dy); }

private:
    void set_pixel_with_draw_op(dword& pixel, const Color&);
    void fill_rect_with_draw_op(const Rect&, Color);

    const Font* m_font;
    Point m_translation;
    Rect m_clip_rect;
    RetainPtr<GraphicsBitmap> m_target;
#ifndef KERNEL
    GWindow* m_window { nullptr };
    void* m_backing_store_id { nullptr };
#endif
    DrawOp m_draw_op { DrawOp::Copy };
};
