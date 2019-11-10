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

    Point m_drag_origin;
    HashMap<GWidget*, Point> m_positions_before_drag;
    bool m_dragging { false };
};
