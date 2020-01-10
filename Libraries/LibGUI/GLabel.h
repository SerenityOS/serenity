#pragma once

#include <LibDraw/TextAlignment.h>
#include <LibGUI/GFrame.h>

class GraphicsBitmap;

class GLabel : public GFrame {
    C_OBJECT(GLabel)
public:
    virtual ~GLabel() override;

    String text() const { return m_text; }
    void set_text(const StringView&);

    void set_icon(GraphicsBitmap*);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    bool should_stretch_icon() const { return m_should_stretch_icon; }
    void set_should_stretch_icon(bool b) { m_should_stretch_icon = b; }

    void size_to_fit();

protected:
    explicit GLabel(GWidget* parent = nullptr);
    GLabel(const StringView& text, GWidget* parent = nullptr);

    virtual void paint_event(GPaintEvent&) override;

private:
    String m_text;
    RefPtr<GraphicsBitmap> m_icon;
    TextAlignment m_text_alignment { TextAlignment::Center };
    bool m_should_stretch_icon { false };
};
