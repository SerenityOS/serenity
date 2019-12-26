#pragma once

#include "Tool.h"
#include <LibDraw/Point.h>

class GMenu;
class Painter;

class RectangleTool final : public Tool {
public:
    RectangleTool();
    virtual ~RectangleTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;
    virtual void on_second_paint(GPaintEvent&) override;
    virtual void on_keydown(GKeyEvent&) override;

private:
    enum class Mode {
        Outline,
        Fill,
        Gradient,
    };

    virtual const char* class_name() const override { return "RectangleTool"; }
    void draw_using(Painter& painter);

    GMouseButton m_drawing_button { GMouseButton::None };
    Point m_rectangle_start_position;
    Point m_rectangle_end_position;
    RefPtr<GMenu> m_context_menu;
    Mode m_mode { Mode::Outline };
};
