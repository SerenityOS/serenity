#pragma once

#include "Tool.h"
#include <LibDraw/Point.h>

class GMenu;

class PenTool final : public Tool {
public:
    PenTool();
    virtual ~PenTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;

private:
    virtual const char* class_name() const override { return "PenTool"; }

    Point m_last_drawing_event_position { -1, -1 };
    RefPtr<GMenu> m_context_menu;
    int m_thickness { 1 };
};
