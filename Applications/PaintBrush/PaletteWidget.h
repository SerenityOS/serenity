#pragma once

#include <LibGUI/GFrame.h>

class PaintableWidget;

class PaletteWidget final : public GFrame {
    C_OBJECT(PaletteWidget)
public:
    explicit PaletteWidget(PaintableWidget&, GWidget* parent);
    virtual ~PaletteWidget() override;

    void set_primary_color(Color);
    void set_secondary_color(Color);

private:
    PaintableWidget& m_paintable_widget;
    GFrame* m_primary_color_widget { nullptr };
    GFrame* m_secondary_color_widget { nullptr };
};
