#pragma once

#include <LibGUI/GDialog.h>

class GFrame;

class ColorDialog final : public GDialog {
    C_OBJECT(ColorDialog)
public:
    virtual ~ColorDialog() override;

    Color color() const { return m_color; }

private:
    explicit ColorDialog(Color, CObject* parent = nullptr);

    void build();

    Color m_color;
    ObjectPtr<GFrame> m_preview_widget;
};
