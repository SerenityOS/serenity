#pragma once

#include "Tool.h"
#include <LibDraw/Point.h>

class GMenu;

class EraseTool final : public Tool {
public:
    EraseTool();
    virtual ~EraseTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;

private:
    virtual const char* class_name() const override { return "EraseTool"; }
    Rect build_rect(const Point& pos, const Rect& widget_rect);
    OwnPtr<GMenu> m_context_menu;
    int m_thickness { 1 };
};
