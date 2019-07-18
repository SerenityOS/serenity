#include <LibGUI/GPainter.h>
#include <LibGUI/GSlider.h>
#include <LibDraw/StylePainter.h>

GSlider::GSlider(GWidget* parent)
    : GWidget(parent)
{
}

GSlider::~GSlider()
{
}

void GSlider::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;
    m_min = min;
    m_max = max;

    if (m_value > max)
        m_value = max;
    if (m_value < min)
        m_value = min;
    update();
}

void GSlider::set_value(int value)
{
    if (value > m_max)
        value = m_max;
    if (value < m_min)
        value = m_min;
    if (m_value == value)
        return;
    m_value = value;
    update();

    if (on_value_changed)
        on_value_changed(m_value);
}

void GSlider::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    Rect track_rect { inner_rect().x(), 0, inner_rect().width(), track_height() };
    track_rect.center_vertically_within(inner_rect());

    StylePainter::paint_frame(painter, track_rect, FrameShape::Panel, FrameShadow::Sunken, 1);
    StylePainter::paint_button(painter, knob_rect(), ButtonStyle::Normal, false, m_knob_hovered);
}

Rect GSlider::knob_rect() const
{
    auto inner_rect = this->inner_rect();
    Rect rect;
    rect.set_y(0);
    rect.set_height(knob_height());

    if (knob_size_mode() == KnobSizeMode::Fixed) {
        if (m_max - m_min)
        {
            float scale = (float)inner_rect.width() / (float)(m_max - m_min);
            rect.set_x(inner_rect.x() + ((int)(m_value * scale)) - (knob_fixed_width() / 2));
        }
        else
            rect.set_x(0);
        rect.set_width(knob_fixed_width());
    }
    else {
        float scale = (float)inner_rect.width() / (float)(m_max - m_min + 1);
        rect.set_x(inner_rect.x() + ((int)(m_value * scale)));
        if (m_max - m_min)
            rect.set_width(::max((int)(scale), knob_fixed_width()));
        else
            rect.set_width(inner_rect.width());
    }
    rect.center_vertically_within(inner_rect);
    return rect;
}

void GSlider::mousedown_event(GMouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == GMouseButton::Left) {
        if (knob_rect().contains(event.position())) {
            m_dragging = true;
            m_drag_origin = event.position();
            m_drag_origin_value = m_value;
            return;
        }
        else {
            if (event.position().x() > knob_rect().right())
                set_value(m_value + 1);
            else if (event.position().x() < knob_rect().left())
                set_value(m_value - 1);
        }
    }
    return GWidget::mousedown_event(event);
}

void GSlider::mousemove_event(GMouseEvent& event)
{
    if (!is_enabled())
        return;
    set_knob_hovered(knob_rect().contains(event.position()));
    if (m_dragging) {
        float delta = event.position().x() - m_drag_origin.x();
        float scrubbable_range = inner_rect().width();
        float value_steps_per_scrubbed_pixel = (m_max - m_min) / scrubbable_range;
        float new_value = m_drag_origin_value + (value_steps_per_scrubbed_pixel * delta);
        set_value((int)new_value);
        return;
    }
    return GWidget::mousemove_event(event);
}

void GSlider::mouseup_event(GMouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == GMouseButton::Left) {
        m_dragging = false;
        return;
    }

    return GWidget::mouseup_event(event);
}

void GSlider::leave_event(CEvent& event)
{
    if (!is_enabled())
        return;
    set_knob_hovered(false);
    GWidget::leave_event(event);
}

void GSlider::change_event(GEvent& event)
{
    if (event.type() == GEvent::Type::EnabledChange) {
        if (!is_enabled())
            m_dragging = false;
    }
    GWidget::change_event(event);
}

void GSlider::set_knob_hovered(bool hovered)
{
    if (m_knob_hovered == hovered)
        return;
    m_knob_hovered = hovered;
    update(knob_rect());
}
