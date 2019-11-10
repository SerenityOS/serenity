#include "CursorTool.h"
#include <AK/LogStream.h>

void CursorTool::on_mousedown(GMouseEvent& event)
{
    (void)event;
    dbg() << "CursorTool::on_mousedown";
}

void CursorTool::on_mouseup(GMouseEvent& event)
{
    (void)event;
    dbg() << "CursorTool::on_mouseup";
}

void CursorTool::on_mousemove(GMouseEvent& event)
{
    (void)event;
    dbg() << "CursorTool::on_mousemove";
}
