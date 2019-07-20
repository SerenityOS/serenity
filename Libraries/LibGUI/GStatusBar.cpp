#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GResizeCorner.h>
#include <LibGUI/GStatusBar.h>
#include <LibDraw/StylePainter.h>

GStatusBar::GStatusBar(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 20);
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_margins({ 2, 2, 2, 2 });
    layout()->set_spacing(2);
    m_label = new GLabel(this);
    m_label->set_frame_shadow(FrameShadow::Sunken);
    m_label->set_frame_shape(FrameShape::Panel);
    m_label->set_frame_thickness(1);
    m_label->set_text_alignment(TextAlignment::CenterLeft);

    m_corner = new GResizeCorner(this);
}

GStatusBar::~GStatusBar()
{
}

void GStatusBar::set_text(const StringView& text)
{
    m_label->set_text(text);
}

String GStatusBar::text() const
{
    return m_label->text();
}

void GStatusBar::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    StylePainter::paint_surface(painter, rect(), !spans_entire_window_horizontally());
}
