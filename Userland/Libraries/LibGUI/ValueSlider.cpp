/*
 * Copyright (c) 2021, Marcus Nilsson <brainbomb@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, ValueSlider)

namespace GUI {

ValueSlider::ValueSlider(Gfx::Orientation orientation, String suffix)
    : AbstractSlider(orientation)
    , m_suffix(move(suffix))
{
    // FIXME: Implement vertical mode
    VERIFY(orientation == Orientation::Horizontal);

    set_preferred_size(SpecialDimension::Fit);

    m_textbox = add<GUI::TextBox>();
    m_textbox->set_relative_rect({ 0, 0, 34, 20 });
    m_textbox->set_font_fixed_width(true);
    m_textbox->set_font_size(8);

    m_textbox->on_change = [&]() {
        ByteString value = m_textbox->text();
        if (value.ends_with(m_suffix, AK::CaseSensitivity::CaseInsensitive))
            value = value.substring_view(0, value.length() - m_suffix.bytes_as_string_view().length());
        auto integer_value = value.to_number<int>();
        if (integer_value.has_value())
            AbstractSlider::set_value(integer_value.value());
    };

    m_textbox->on_return_pressed = [&]() {
        m_textbox->on_change();
        m_textbox->set_text(formatted_value());
    };

    m_textbox->on_up_pressed = [&]() {
        if (value() < max())
            AbstractSlider::increase_slider_by(1);
        m_textbox->set_text(formatted_value());
    };

    m_textbox->on_down_pressed = [&]() {
        if (value() > min())
            AbstractSlider::decrease_slider_by(1);
        m_textbox->set_text(formatted_value());
    };

    m_textbox->on_focusout = [&]() {
        m_textbox->on_return_pressed();
    };

    m_textbox->on_escape_pressed = [&]() {
        m_textbox->clear_selection();
        m_textbox->set_text(formatted_value());
        parent_widget()->set_focus(true);
    };
}

ByteString ValueSlider::formatted_value() const
{
    return ByteString::formatted("{:2}{}", value(), m_suffix);
}

void ValueSlider::paint_event(PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (is_enabled())
        painter.fill_rect_with_gradient(m_orientation, bar_rect(), palette().active_window_border1(), palette().active_window_border2());
    else
        painter.fill_rect_with_gradient(m_orientation, bar_rect(), palette().inactive_window_border1(), palette().inactive_window_border2());

    auto unfilled_rect = bar_rect();
    unfilled_rect.set_left(knob_rect().right() - 1);
    painter.fill_rect(unfilled_rect, palette().base());

    Gfx::StylePainter::paint_frame(painter, bar_rect(), palette(), Gfx::FrameStyle::SunkenContainer);
    Gfx::StylePainter::paint_button(painter, knob_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_hovered);

    auto paint_knurl = [&](int x, int y) {
        painter.set_pixel(x, y, palette().threed_shadow1());
        painter.set_pixel(x + 1, y, palette().threed_shadow1());
        painter.set_pixel(x, y + 1, palette().threed_shadow1());
        painter.set_pixel(x + 1, y + 1, palette().threed_highlight());
    };

    auto knurl_rect = knob_rect().shrunken(4, 8);

    if (m_knob_style == KnobStyle::Wide) {
        for (int i = 0; i < 4; ++i) {
            paint_knurl(knurl_rect.x(), knurl_rect.y() + (i * 3));
            paint_knurl(knurl_rect.x() + 3, knurl_rect.y() + (i * 3));
            paint_knurl(knurl_rect.x() + 6, knurl_rect.y() + (i * 3));
        }
    } else {
        for (int i = 0; i < 4; ++i)
            paint_knurl(knurl_rect.x(), knurl_rect.y() + (i * 3));
    }
}

Gfx::IntRect ValueSlider::bar_rect() const
{
    auto bar_rect = rect();
    bar_rect.set_width(rect().width() - m_textbox->width());
    bar_rect.set_x(m_textbox->width());
    return bar_rect;
}

int ValueSlider::knob_length() const
{
    return m_knob_style == KnobStyle::Wide ? 13 : 7;
}

Gfx::IntRect ValueSlider::knob_rect() const
{
    int knob_thickness = knob_length();

    Gfx::IntRect knob_rect = bar_rect();
    knob_rect.set_width(knob_thickness);

    int knob_offset = (int)((float)bar_rect().left() + (float)(value() - min()) / (float)(max() - min()) * (float)(bar_rect().width() - knob_thickness));
    knob_rect.set_left(knob_offset);
    knob_rect.center_vertically_within(bar_rect());
    return knob_rect;
}

int ValueSlider::value_at(Gfx::IntPoint position) const
{
    int knob_thickness = knob_length();
    float leftmost_knob_center = (float)bar_rect().left() + (float)knob_thickness / 2;
    if (position.x() < leftmost_knob_center)
        return min();
    float rightmost_knob_center = (float)(bar_rect().right() - 1) - (float)knob_thickness / 2;
    if (position.x() > rightmost_knob_center)
        return max();
    float relative_offset = (float)(position.x() - leftmost_knob_center) / (rightmost_knob_center - leftmost_knob_center);

    int range = max() - min();
    return min() + (int)roundf(relative_offset * (float)range);
}

void ValueSlider::set_value(int value, AllowCallback allow_callback, DoClamp do_clamp)
{
    AbstractSlider::set_value(value, allow_callback, do_clamp);
    m_textbox->set_text(formatted_value());
}

void ValueSlider::leave_event(Core::Event&)
{
    if (!m_hovered)
        return;

    m_hovered = false;
    update(knob_rect());
}

void ValueSlider::mousewheel_event(MouseEvent& event)
{
    if (event.wheel_delta_y() < 0)
        increase_slider_by(1);
    else
        decrease_slider_by(1);
}

void ValueSlider::mousemove_event(MouseEvent& event)
{
    bool is_hovered = knob_rect().contains(event.position());
    if (is_hovered != m_hovered) {
        m_hovered = is_hovered;
        update(knob_rect());
    }

    if (!m_dragging)
        return;

    set_value(value_at(event.position()));
}

void ValueSlider::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    m_textbox->set_focus(true);

    if (bar_rect().contains(event.position())) {
        m_dragging = true;
        set_value(value_at(event.position()));
    }
}

void ValueSlider::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    m_dragging = false;
}

Optional<UISize> ValueSlider::calculated_min_size() const
{
    auto content_min_size = m_textbox->effective_min_size();

    if (orientation() == Gfx::Orientation::Vertical)
        return { { content_min_size.width(), content_min_size.height().as_int() + knob_length() } };
    return { { content_min_size.width().as_int() + knob_length(), content_min_size.height() } };
}

Optional<UISize> ValueSlider::calculated_preferred_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { SpecialDimension::Shrink, SpecialDimension::OpportunisticGrow } };
    return { { SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink } };
}

}
