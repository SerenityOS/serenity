#pragma once

#include <LibGUI/GDialog.h>

class ColorDialog final : public GDialog {
public:
    explicit ColorDialog(Color, CObject* parent = nullptr);
    virtual ~ColorDialog() override;
    virtual const char* class_name() const override { return "ColorDialog"; }

    Color color() const { return m_color; }

private:
    void build();

    Color m_color;
};
