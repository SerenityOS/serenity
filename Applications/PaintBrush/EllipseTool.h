#pragma once

#include "Tool.h"
#include <LibDraw/Point.h>

class GMenu;
class Painter;

class EllipseTool final : public Tool {
public:
    EllipseTool();
    virtual ~EllipseTool() override;

    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_contextmenu(GContextMenuEvent&) override;
    virtual void on_second_paint(GPaintEvent&) override;
    virtual void on_keydown(GKeyEvent&) override;

private:
    enum class Mode {
        Outline,
        // FIXME: Add Mode::Fill
    };

    virtual const char* class_name() const override { return "EllipseTool"; }
    void draw_using(Painter& painter);

    GMouseButton m_drawing_button { GMouseButton::None };
    Point m_ellipse_start_position;
    Point m_ellipse_end_position;
    RefPtr<GMenu> m_context_menu;
    int m_thickness { 1 };
    Mode m_mode { Mode::Outline };
};
