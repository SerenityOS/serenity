#pragma once

#include <LibGUI/GFrame.h>

class ToolboxWidget final : public GFrame {
    C_OBJECT(ToolboxWidget)
public:
    explicit ToolboxWidget(GWidget* parent);
    virtual ~ToolboxWidget() override;
};
