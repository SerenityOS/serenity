#include <LibGUI/GTreeView.h>
#include <LibGUI/GPainter.h>

GTreeView::GTreeView(GWidget* parent)
    : GAbstractView(parent)
{
}

GTreeView::~GTreeView()
{
}

void GTreeView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.set_clip_rect(frame_inner_rect());
    painter.set_clip_rect(event.rect());

    painter.fill_rect(event.rect(), Color::White);
}
