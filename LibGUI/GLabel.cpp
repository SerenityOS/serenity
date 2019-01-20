#include "GLabel.h"
#include <SharedGraphics/Painter.h>

GLabel::GLabel(GWidget* parent)
    : GWidget(parent)
{
}

GLabel::~GLabel()
{
}

void GLabel::setText(String&& text)
{
    if (text == m_text)
        return;
    m_text = move(text);
    update();
}

void GLabel::paintEvent(GPaintEvent&)
{
    Painter painter(*this);
    if (fillWithBackgroundColor())
        painter.fill_rect({ 0, 0, width(), height() }, backgroundColor());
    if (!text().is_empty())
        painter.draw_text({ 4, 4, width(), height() }, text(), Painter::TextAlignment::TopLeft, foregroundColor());
}
