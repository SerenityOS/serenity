#pragma once

#include "Color.h"
#include "Point.h"
#include "Rect.h"
#include "Size.h"
#include <AK/AKString.h>
#include <SharedGraphics/TextAlignment.h>
#include <SharedGraphics/TextElision.h>

class CharacterBitmap;
class GlyphBitmap;
class GraphicsBitmap;
class Font;

class Painter {
public:
    explicit Painter(GraphicsBitmap&);
    ~Painter();
    void fill_rect(const Rect&, Color);
    void fill_rect_with_gradient(const Rect&, Color gradient_start, Color gradient_end);
    void draw_rect(const Rect&, Color, bool rough = false);
    void draw_bitmap(const Point&, const CharacterBitmap&, Color = Color());
    void draw_bitmap(const Point&, const GlyphBitmap&, Color = Color());
    void set_pixel(const Point&, Color);
    void draw_line(const Point&, const Point&, Color);
    void draw_scaled_bitmap(const Rect& dst_rect, const GraphicsBitmap&, const Rect& src_rect);
    void blit(const Point&, const GraphicsBitmap&, const Rect& src_rect, float opacity = 1.0f);
    void blit_dimmed(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void blit_tiled(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void blit_offset(const Point&, const GraphicsBitmap&, const Rect& src_rect, const Point&);
    void blit_scaled(const Point&, const GraphicsBitmap&, const Rect&, float, float);
    void draw_text(const Rect&, const StringView&, const Font&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_text(const Rect&, const StringView&, TextAlignment = TextAlignment::TopLeft, Color = Color::Black, TextElision = TextElision::None);
    void draw_glyph(const Point&, char, Color);
    void draw_glyph(const Point&, char, const Font&, Color);

    const Font& font() const { return *state().font; }
    void set_font(const Font& font) { state().font = &font; }

    enum class DrawOp
    {
        Copy,
        Xor
    };
    void set_draw_op(DrawOp op) { state().draw_op = op; }
    DrawOp draw_op() const { return state().draw_op; }

    void add_clip_rect(const Rect& rect);
    void clear_clip_rect();
    Rect clip_rect() const { return state().clip_rect; }

    void translate(int dx, int dy) { state().translation.move_by(dx, dy); }
    void translate(const Point& delta) { state().translation.move_by(delta); }

    Point translation() const { return state().translation; }

    GraphicsBitmap* target() { return m_target.ptr(); }

    void save() { m_state_stack.append(m_state_stack.last()); }
    void restore()
    {
        ASSERT(m_state_stack.size() > 1);
        m_state_stack.take_last();
    }

protected:
    void set_pixel_with_draw_op(dword& pixel, const Color&);
    void fill_rect_with_draw_op(const Rect&, Color);
    void blit_with_alpha(const Point&, const GraphicsBitmap&, const Rect& src_rect);
    void blit_with_opacity(const Point&, const GraphicsBitmap&, const Rect& src_rect, float opacity);

    struct State {
        const Font* font;
        Point translation;
        Rect clip_rect;
        DrawOp draw_op;
    };

    State& state() { return m_state_stack.last(); }
    const State& state() const { return m_state_stack.last(); }

    Rect m_clip_origin;
    Retained<GraphicsBitmap> m_target;
    Vector<State, 4> m_state_stack;
};

class PainterStateSaver {
public:
    explicit PainterStateSaver(Painter&);
    ~PainterStateSaver();

private:
    Painter& m_painter;
};
