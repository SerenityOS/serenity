#pragma once

#include "GWidget.h"
#include <AK/AKString.h>
#include <SharedGraphics/Painter.h>

class GraphicsBitmap;

class GLabel final : public GWidget {
public:
    explicit GLabel(GWidget* parent);
    virtual ~GLabel() override;

    String text() const { return m_text; }
    void set_text(String&&);

    void set_icon(RetainPtr<GraphicsBitmap>&&);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }

private:
    virtual void paint_event(GPaintEvent&) override;

    virtual const char* class_name() const override { return "GLabel"; }

    String m_text;
    RetainPtr<GraphicsBitmap> m_icon;
    TextAlignment m_text_alignment { TextAlignment::Center };
};

