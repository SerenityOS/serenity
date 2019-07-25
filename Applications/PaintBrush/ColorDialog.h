#pragma once

#include <LibGUI/GDialog.h>

class ColorDialog final : public GDialog {
    C_OBJECT(ColorDialog)
public:
    explicit ColorDialog(Color, CObject* parent = nullptr);
    virtual ~ColorDialog() override;

    Color color() const { return m_color; }

private:
    void build();

    Color m_color;
};
