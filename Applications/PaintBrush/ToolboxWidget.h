#pragma once

#include <LibGUI/GFrame.h>

class ToolboxWidget final : public GFrame {
public:
    explicit ToolboxWidget(GWidget* parent);
    virtual ~ToolboxWidget() override;

    virtual const char* class_name() const override { return "ToolboxWidget"; }

private:
};
