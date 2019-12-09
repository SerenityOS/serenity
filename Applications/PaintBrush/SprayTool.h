#pragma once

#include "Tool.h"
#include <LibGUI/GPainter.h>
#include <LibCore/CTimer.h>

class GMenu;

class SprayTool final : public Tool {
public:
    SprayTool();
    virtual ~SprayTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;

private:
    virtual const char* class_name() const override { return "SprayTool"; }
    void paint_it();
    RefPtr<CTimer> m_timer;
    Point m_last_pos;
    Color m_color;
    RefPtr<GMenu> m_context_menu;
    int m_thickness { 1 };
};
