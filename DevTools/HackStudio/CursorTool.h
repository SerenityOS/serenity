#pragma once

#include "Tool.h"
#include <AK/HashMap.h>
#include <LibDraw/Point.h>

class GWidget;

class CursorTool final : public Tool {
public:
    explicit CursorTool(FormEditorWidget& editor)
        : Tool(editor)
    {
    }
    virtual ~CursorTool() override {}

private:
    virtual const char* class_name() const override { return "CursorTool"; }
    virtual void on_mousedown(GMouseEvent&) override;
    virtual void on_mouseup(GMouseEvent&) override;
    virtual void on_mousemove(GMouseEvent&) override;
    virtual void on_keydown(GKeyEvent&) override;
    virtual void on_second_paint(GPainter&, GPaintEvent&) override;

    void set_rubber_band_position(const Point&);
    Rect rubber_band_rect() const;

    Point m_drag_origin;
    HashMap<GWidget*, Point> m_positions_before_drag;
    bool m_dragging { false };

    bool m_rubber_banding { false };
    Point m_rubber_band_origin;
    Point m_rubber_band_position;
};
