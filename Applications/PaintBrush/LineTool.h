#pragma once

#include "Tool.h"
#include <LibDraw/Point.h>

class GMenu;

class LineTool final : public Tool {
public:
    LineTool();
    virtual ~LineTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;
    virtual void on_second_paint(GPaintEvent&) override;
    virtual void on_keydown(GKeyEvent&) override;

private:
    virtual const char* class_name() const override { return "LineTool"; }

    GMouseButton m_drawing_button { GMouseButton::None };
    Point m_line_start_position;
    Point m_line_end_position;
    OwnPtr<GMenu> m_context_menu;
    int m_thickness { 1 };
};
