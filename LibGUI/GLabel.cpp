#include "GLabel.h"
#include <SharedGraphics/Painter.h>

GLabel::GLabel(GWidget* parent)
    : GWidget(parent)
{
}

GLabel::~GLabel()
{
}

void GLabel::set_text(String&& text)
{
    if (text == m_text)
        return;
    m_text = move(text);
    update();
}

void GLabel::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    if (fill_with_background_color())
        painter.fill_rect({ 0, 0, width(), height() }, background_color());
    if (!text().is_empty())
        painter.draw_text({ 4, 4, width(), height() }, text(), Painter::TextAlignment::TopLeft, foreground_color());
}
