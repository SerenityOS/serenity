#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>

GLabel::GLabel(GWidget* parent)
    : GFrame(parent)
{
}

GLabel::GLabel(const StringView& text, GWidget* parent)
    : GFrame(parent)
    , m_text(text)
{
}

GLabel::~GLabel()
{
}

void GLabel::set_icon(GraphicsBitmap* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    update();
}

void GLabel::set_text(const StringView& text)
{
    if (text == m_text)
        return;
    m_text = text;
    update();
}

void GLabel::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_icon) {
        if (m_should_stretch_icon) {
            painter.draw_scaled_bitmap(frame_inner_rect(), *m_icon, m_icon->rect());
        } else {
            auto icon_location = frame_inner_rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
            painter.blit(icon_location, *m_icon, m_icon->rect());
        }
    }
    if (text().is_empty())
        return;
    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;
    auto text_rect = frame_inner_rect();
    text_rect.move_by(indent, 0);
    text_rect.set_width(text_rect.width() - indent * 2);

    if (is_enabled()) {
        painter.draw_text(text_rect, text(), m_text_alignment, palette().window_text(), TextElision::Right);
    } else {
        painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), Color::White, TextElision::Right);
        painter.draw_text(text_rect, text(), font(), text_alignment(), Color::from_rgb(0x808080), TextElision::Right);
    }
}

void GLabel::size_to_fit()
{
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    set_preferred_size(font().width(m_text), 0);
}
