#pragma once

#include <LibGUI/GWidget.h>
#include <AK/Function.h>

class GlyphEditorWidget;
class GlyphMapWidget;
class GTextBox;

class FontEditorWidget final : public GWidget {
public:
    FontEditorWidget(const String& path, RetainPtr<Font>&&, GWidget* parent = nullptr);
    virtual ~FontEditorWidget() override;

private:
    RetainPtr<Font> m_edited_font;

    GlyphMapWidget* m_glyph_map_widget { nullptr };
    GlyphEditorWidget* m_glyph_editor_widget { nullptr };
    GTextBox* m_name_textbox { nullptr };
    GTextBox* m_path_textbox { nullptr };

    String m_path;
};

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

class GlyphEditorWidget final : public GWidget {
public:
    GlyphEditorWidget(Font&, GWidget* parent);
    virtual ~GlyphEditorWidget() override;

    byte glyph() const { return m_glyph; }
    void set_glyph(byte);

    int preferred_width() const;
    int preferred_height() const;

    Font& font() { return *m_font; }
    const Font& font() const { return *m_font; }

    Function<void()> on_glyph_altered;

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual bool accepts_focus() const override { return true; }

    void draw_at_mouse(const GMouseEvent&);

    RetainPtr<Font> m_font;
    byte m_glyph { 0 };
    int m_scale { 10 };
};
