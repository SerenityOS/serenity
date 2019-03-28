#include "GLabel.h"
#include <SharedGraphics/Painter.h>
#include <SharedGraphics/GraphicsBitmap.h>

GLabel::GLabel(GWidget* parent)
    : GFrame(parent)
{
}

GLabel::GLabel(const String& text, GWidget* parent)
    : GFrame(parent)
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
    GFrame::paint_event(event);

    Painter painter(*this);
    painter.set_clip_rect(event.rect());

    if (m_icon) {
        if (m_should_stretch_icon) {
            painter.draw_scaled_bitmap(frame_inner_rect(), *m_icon, m_icon->rect());
        } else {
            auto icon_location = frame_inner_rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
            painter.blit(icon_location, *m_icon, m_icon->rect());
        }
    }
    if (!text().is_empty()) {
        int indent = 0;
        if (frame_thickness() > 0)
            indent = font().glyph_width('x') / 2;
        auto text_rect = frame_inner_rect();
        text_rect.move_by(indent, 0);
        text_rect.set_width(text_rect.width() - indent * 2);
        painter.draw_text(text_rect, text(), m_text_alignment, foreground_color());
    }
}

void GLabel::size_to_fit()
{
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    set_preferred_size({ font().width(m_text), 0 });
}
