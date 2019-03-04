#include <LibGUI/GStatusBar.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GStyle.h>
#include <SharedGraphics/Painter.h>

GStatusBar::GStatusBar(GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size({ 0, 20 });
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_margins({ 4, 2, 4, 2 });
    m_label = new GLabel(this);
    m_label->set_text_alignment(TextAlignment::CenterLeft);
    m_label->set_fill_with_background_color(false);
}

GStatusBar::~GStatusBar()
{
}

void GStatusBar::set_text(String&& text)
{
    m_label->set_text(move(text));
}

String GStatusBar::text() const
{
    return m_label->text();
}

void GStatusBar::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    GStyle::the().paint_surface(painter, rect());
}
