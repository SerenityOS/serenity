#include <LibDraw/StylePainter.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GResizeCorner.h>
#include <LibGUI/GStatusBar.h>

GStatusBar::GStatusBar(GWidget* parent)
    : GStatusBar(1, parent)
{
}

GStatusBar::GStatusBar(int label_count, GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 20);
    set_layout(make<GBoxLayout>(Orientation::Horizontal));
    layout()->set_margins({ 2, 2, 2, 2 });
    layout()->set_spacing(2);

    if (label_count < 1)
        label_count = 1;

    for (auto i = 0; i < label_count; i++)
        m_labels.append(create_label());

    m_corner = GResizeCorner::construct(this);
}

GStatusBar::~GStatusBar()
{
}

NonnullRefPtr<GLabel> GStatusBar::create_label()
{
    auto label = GLabel::construct(this);
    label->set_frame_shadow(FrameShadow::Sunken);
    label->set_frame_shape(FrameShape::Panel);
    label->set_frame_thickness(1);
    label->set_text_alignment(TextAlignment::CenterLeft);
    return label;
}

void GStatusBar::set_text(const StringView& text)
{
    m_labels.first().set_text(text);
}

String GStatusBar::text() const
{
    return m_labels.first().text();
}

void GStatusBar::set_text(int index, const StringView& text)
{
    m_labels.at(index).set_text(text);
}

String GStatusBar::text(int index) const
{
    return m_labels.at(index).text();
}

void GStatusBar::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    StylePainter::paint_surface(painter, rect(), palette(), !spans_entire_window_horizontally());
}
