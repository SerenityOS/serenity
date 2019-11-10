#include "WidgetTool.h"
#include <AK/LogStream.h>

void WidgetTool::on_mousedown(GMouseEvent& event)
{
    (void)event;
    dbg() << "WidgetTool::on_mousedown";
}

void WidgetTool::on_mouseup(GMouseEvent& event)
{
    (void)event;
    dbg() << "WidgetTool::on_mouseup";
}

void WidgetTool::on_mousemove(GMouseEvent& event)
{
    (void)event;
    dbg() << "WidgetTool::on_mousemove";
}

void WidgetTool::on_keydown(GKeyEvent& event)
{
    (void)event;
    dbg() << "WidgetTool::on_keydown";
}
