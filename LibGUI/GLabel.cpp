#include "GLabel.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/GraphicsBitmap.h>

GLabel::GLabel(GWidget* parent)
    : GWidget(parent)
{
}

GLabel::~GLabel()
{
}

void GLabel::set_icon(RetainPtr<GraphicsBitmap>&& icon)
{
    m_icon = move(icon);
}

void GLabel::set_text(String&& text)
{
    if (text == m_text)
        return;
    m_text = move(text);
    update();
}

void GLabel::paint_event(GPaintEvent&)
{
    Painter painter(*this);
    if (fill_with_background_color())
        painter.fill_rect({ 0, 0, width(), height() }, background_color());
    if (m_icon) {
        auto icon_location = rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
        painter.blit_with_alpha(icon_location, *m_icon, m_icon->rect());
    }
    if (!text().is_empty())
        painter.draw_text({ 0, 0, width(), height() }, text(), m_text_alignment, foreground_color());
}
