#pragma once

#include <LibGUI/GWidget.h>
class Tool;

class PaintableWidget final : public GWidget {
    C_OBJECT(PaintableWidget)
public:
    static PaintableWidget& the();

    explicit PaintableWidget(GWidget* parent);
    virtual ~PaintableWidget() override;

    Color primary_color() const { return m_primary_color; }
    Color secondary_color() const { return m_secondary_color; }

    void set_primary_color(Color);
    void set_secondary_color(Color);

    void set_tool(Tool* tool);
    Tool* tool();

    Color color_for(const GMouseEvent&) const;
    Color color_for(GMouseButton) const;

    void set_bitmap(const GraphicsBitmap&);

    GraphicsBitmap& bitmap() { return *m_bitmap; }
    const GraphicsBitmap& bitmap() const { return *m_bitmap; }

    Function<void(Color)> on_primary_color_change;
    Function<void(Color)> on_secondary_color_change;

private:
    virtual bool accepts_focus() const override { return true; }
    virtual void paint_event(GPaintEvent&) override;
    virtual void second_paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void keyup_event(GKeyEvent&) override;

    RefPtr<GraphicsBitmap> m_bitmap;

    Color m_primary_color { Color::Black };
    Color m_secondary_color { Color::White };

    Tool* m_tool { nullptr };
};
