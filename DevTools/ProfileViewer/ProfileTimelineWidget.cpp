#include "ProfileTimelineWidget.h"
#include "Profile.h"
#include <LibGUI/GPainter.h>

ProfileTimelineWidget::ProfileTimelineWidget(Profile& profile, GWidget* parent)
    : GFrame(parent)
    , m_profile(profile)
{
    set_frame_thickness(2);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_shape(FrameShape::Container);
    set_background_color(Color::White);
    set_fill_with_background_color(true);
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 80);
}

ProfileTimelineWidget::~ProfileTimelineWidget()
{
}

void ProfileTimelineWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    float column_width = (float)frame_inner_rect().width() / (float)m_profile.length_in_ms();

    m_profile.for_each_sample([&](const JsonObject& sample) {
        u64 t = sample.get("timestamp").to_number<u64>() - m_profile.first_timestamp();
        int x = (int)((float)t * column_width);
        int cw = max(1, (int)column_width);

        bool in_kernel = sample.get("frames").as_array().at(1).as_object().get("address").to_number<u32>() < (8 * MB);
        Color color = in_kernel ? Color::from_rgb(0xc25e5a) : Color::from_rgb(0x5a65c2);
        for (int i = 0; i < cw; ++i)
            painter.draw_line({ x + i, frame_thickness() }, { x + i, height() - frame_thickness() * 2 }, color);
    });
}

void ProfileTimelineWidget::mousedown_event(GMouseEvent&)
{
}

void ProfileTimelineWidget::mousemove_event(GMouseEvent&)
{
}

void ProfileTimelineWidget::mouseup_event(GMouseEvent&)
{
}
