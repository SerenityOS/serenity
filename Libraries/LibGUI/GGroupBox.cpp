#include <LibGUI/GGroupBox.h>
#include <LibGUI/GPainter.h>
#include <LibDraw/StylePainter.h>

GGroupBox::GGroupBox(GWidget* parent)
    : GGroupBox({}, parent)
{
}

GGroupBox::GGroupBox(const StringView& title, GWidget* parent)
    : GWidget(parent)
    , m_title(title)
{
    set_fill_with_background_color(true);
    set_background_color(SystemColor::Window);
}

GGroupBox::~GGroupBox()
{
}

void GGroupBox::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    Rect frame_rect {
        0, font().glyph_height() / 2,
        width(), height() - font().glyph_height() / 2
    };
    StylePainter::paint_frame(painter, frame_rect, FrameShape::Box, FrameShadow::Sunken, 2);

    Rect text_rect { 4, 0, font().width(m_title) + 6, font().glyph_height() };
    painter.fill_rect(text_rect, background_color());
    painter.draw_text(text_rect, m_title, TextAlignment::Center, foreground_color());
}

void GGroupBox::set_title(const StringView& title)
{
    if (m_title == title)
        return;
    m_title = title;
    update();
}
