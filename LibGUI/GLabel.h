#pragma once

#include <LibGUI/GFrame.h>
#include <SharedGraphics/TextAlignment.h>

class GraphicsBitmap;

class GLabel : public GFrame {
public:
    explicit GLabel(GWidget* parent = nullptr);
    GLabel(const StringView& text, GWidget* parent = nullptr);
    virtual ~GLabel() override;

    String text() const { return m_text; }
    void set_text(const StringView&);

    void set_icon(RetainPtr<GraphicsBitmap>&&);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    bool should_stretch_icon() const { return m_should_stretch_icon; }
    void set_should_stretch_icon(bool b) { m_should_stretch_icon = b; }

    void size_to_fit();

    virtual const char* class_name() const override { return "GLabel"; }

private:
    virtual void paint_event(GPaintEvent&) override;

    String m_text;
    RetainPtr<GraphicsBitmap> m_icon;
    TextAlignment m_text_alignment { TextAlignment::Center };
    bool m_should_stretch_icon { false };
};
