#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>

class GlyphMapWidget final : public GWidget {
public:
    GlyphMapWidget(Font&, GWidget* parent);
    virtual ~GlyphMapWidget() override;

    byte selected_glyph() const { return m_selected_glyph; }
    void set_selected_glyph(byte);

    int rows() const { return m_rows; }
    int columns() const { return 256 / m_rows; }

    int preferred_width() const;
    int preferred_height() const;

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

    void update_glyph(byte);

    Function<void(byte)> on_glyph_selected;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    Rect get_outer_rect(byte glyph) const;

    RetainPtr<Font> m_font;
    int m_rows { 8 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    byte m_selected_glyph { 0 };
};
