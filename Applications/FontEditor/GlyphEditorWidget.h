#include <AK/Function.h>
#include <LibGUI/GFrame.h>

class GlyphEditorWidget final : public GFrame {
public:
    GlyphEditorWidget(Font&, GWidget* parent);
    virtual ~GlyphEditorWidget() override;

    byte glyph() const { return m_glyph; }
    void set_glyph(byte);

    int preferred_width() const;
    int preferred_height() const;

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

    Function<void(byte)> on_glyph_altered;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;

    void draw_at_mouse(const GMouseEvent&);

    RefPtr<Font> m_font;
    byte m_glyph { 0 };
    int m_scale { 10 };
};
