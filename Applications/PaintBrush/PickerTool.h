#pragma once

#include "Tool.h"

class PickerTool final : public Tool {
public:
    PickerTool();
    virtual ~PickerTool() override;

    virtual void on_mousedown(GMouseEvent&) override;

private:
    virtual const char* class_name() const override { return "PickerTool"; }

};
