#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

GPainter::GPainter(GraphicsBitmap& bitmap)
    : Painter(bitmap)
{
}

GPainter::GPainter(GWidget& widget)
    : Painter(*widget.window()->back_bitmap())
{
    state().font = &widget.font();
    auto origin_rect = widget.window_relative_rect();
    state().translation = origin_rect.location();
    state().clip_rect = origin_rect;
    m_clip_origin = origin_rect;
    state().clip_rect.intersect(m_target->rect());
}
