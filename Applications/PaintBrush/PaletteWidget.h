#pragma once

#include <LibGUI/GFrame.h>

class PaintableWidget;

class PaletteWidget final : public GFrame {
public:
    explicit PaletteWidget(PaintableWidget&, GWidget* parent);
    virtual ~PaletteWidget() override;

    virtual const char* class_name() const override { return "PaletteWidget"; }
};
