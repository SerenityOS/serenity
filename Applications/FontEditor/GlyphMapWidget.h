#pragma once

#include <AK/Function.h>
#include <LibGUI/GFrame.h>

class GlyphMapWidget final : public GFrame {
public:
    GlyphMapWidget(Font&, GWidget* parent);
    virtual ~GlyphMapWidget() override;

    u8 selected_glyph() const { return m_selected_glyph; }
    void set_selected_glyph(u8);

    int rows() const { return m_rows; }
    int columns() const { return 256 / m_rows; }

    int preferred_width() const;
    int preferred_height() const;

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

    void update_glyph(u8);

    Function<void(u8)> on_glyph_selected;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

    Rect get_outer_rect(u8 glyph) const;

    RefPtr<Font> m_font;
    int m_rows { 8 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    u8 m_selected_glyph { 0 };
};
