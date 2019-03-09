#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <SharedGraphics/TextAlignment.h>
#include <AK/AKString.h>

class CharacterBitmap;
class GlyphBitmap;
class GraphicsBitmap;
class Font;

#ifdef USERLAND
class GWidget;
class GWindow;
#endif

class Painter {
public:
#ifdef USERLAND
    explicit Painter(GWidget&);
#endif
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void fill_rect(const Rect&, Color);
    void fill_rect_with_gradient(const Rect&, Color gradient_start, Color gradient_end);
    void draw_rect(const Rect&, Color, bool rough = false);
    void draw_bitmap(const Point&, const CharacterBitmap&, Color = Color());
    void draw_bitmap(const Point&, const GlyphBitmap&, Color = Color());
    void set_pixel(const Point&, Color);
    void draw_line(const Point&, const Point&, Color);
    void draw_focus_rect(const Rect&);
    void blit(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void blit_with_opacity(const Point&, const GraphicsBitmap&, const Rect& src_rect, float opacity);

    void draw_text(const Rect&, const char* text, int length, TextAlignment = TextAlignment::TopLeft, Color = Color());
    void draw_text(const Rect&, const String&, TextAlignment = TextAlignment::TopLeft, Color = Color());
    void draw_glyph(const Point&, char, Color);

    const Font& font() const { return *state().font; }
    void set_font(const Font& font) { state().font = &font; }

    enum class DrawOp { Copy, Xor };
    void set_draw_op(DrawOp op) { state().draw_op = op; }
    DrawOp draw_op() const { return state().draw_op; }

    void set_clip_rect(const Rect& rect);
    void clear_clip_rect();
    Rect clip_rect() const { return state().clip_rect; }

    void translate(int dx, int dy) { state().translation.move_by(dx, dy); }
    void translate(const Point& delta) { state().translation.move_by(delta); }

    Point translation() const { return state().translation; }

    GraphicsBitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore() { ASSERT(m_state_stack.size() > 1); m_state_stack.take_last(); }

private:
    void set_pixel_with_draw_op(dword& pixel, const Color&);
    void fill_rect_with_draw_op(const Rect&, Color);
    void blit_with_alpha(const Point&, const GraphicsBitmap&, const Rect& src_rect);

    struct State {
        const Font* font;
        Point translation;
        Rect clip_rect;
        DrawOp draw_op;
    };

    State& state() { return m_state_stack.last(); }
    const State& state() const { return m_state_stack.last(); }

    Rect m_clip_origin;
    GWindow* m_window { nullptr };
    Retained<GraphicsBitmap> m_target;
    Vector<State> m_state_stack;
};
