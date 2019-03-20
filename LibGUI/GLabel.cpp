#include "GLabel.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/GraphicsBitmap.h>

GLabel::GLabel(GWidget* parent)
    : GWidget(parent)
{
}

GLabel::GLabel(const String& text, GWidget* parent)
    : GWidget(parent)
    , m_text(text)
{
}

GLabel::~GLabel()
{
}

void GLabel::set_icon(RetainPtr<GraphicsBitmap>&& icon)
{
    m_icon = move(icon);
}

void GLabel::set_text(const String& text)
{
    if (text == m_text)
        return;
    m_text = move(text);
    update();
}

void GLabel::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    if (fill_with_background_color())
        painter.fill_rect({ 0, 0, width(), height() }, background_color());
    if (m_icon) {
        auto icon_location = rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
        painter.blit(icon_location, *m_icon, m_icon->rect());
    }
    if (!text().is_empty())
        painter.draw_text({ 0, 0, width(), height() }, text(), m_text_alignment, foreground_color());
}

void GLabel::size_to_fit()
{
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    set_preferred_size({ font().width(m_text), 0 });
}
