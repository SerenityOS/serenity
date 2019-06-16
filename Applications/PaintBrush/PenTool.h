#pragma once

#include "Tool.h"
#include <SharedGraphics/Point.h>

class PenTool final : public Tool {
public:
    PenTool();
    virtual ~PenTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;

private:
    virtual const char* class_name() const override { return "PenTool"; }

    Point m_last_drawing_event_position { -1, -1 };
};
