/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

static constexpr int ANIMATION_INTERVAL = 16;  // Milliseconds
static constexpr double ANIMATION_TIME = 0.18; // Seconds

REGISTER_WIDGET(GUI, Scrollbar)

namespace GUI {

static constexpr AK::Array<Gfx::IntPoint, 3> s_up_arrow_coords = {
    Gfx::IntPoint { 4, 2 },
    Gfx::IntPoint { 1, 5 },
    Gfx::IntPoint { 7, 5 },
};

static constexpr AK::Array<Gfx::IntPoint, 3> s_down_arrow_coords = {
    Gfx::IntPoint { 1, 3 },
    Gfx::IntPoint { 7, 3 },
    Gfx::IntPoint { 4, 6 },
};

static constexpr AK::Array<Gfx::IntPoint, 3> s_left_arrow_coords = {
    Gfx::IntPoint { 5, 1 },
    Gfx::IntPoint { 2, 4 },
    Gfx::IntPoint { 5, 7 },
};

static constexpr AK::Array<Gfx::IntPoint, 3> s_right_arrow_coords = {
    Gfx::IntPoint { 3, 1 },
    Gfx::IntPoint { 6, 4 },
    Gfx::IntPoint { 3, 7 },
};

Scrollbar::Scrollbar(Orientation orientation)
    : AbstractSlider(orientation)
{
    m_automatic_scrolling_timer = add<Core::Timer>();

    set_preferred_size({ SpecialDimension::Fit });

    m_automatic_scrolling_timer->set_interval(100);
    m_automatic_scrolling_timer->on_timeout = [this] {
        automatic_scrolling_timer_did_fire();
    };
}

Gfx::IntRect Scrollbar::decrement_button_rect() const
{
    return { 0, 0, button_width(), button_height() };
}

Gfx::IntRect Scrollbar::increment_button_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, height() - button_height(), button_width(), button_height() };
    else
        return { width() - button_width(), 0, button_width(), button_height() };
}

int Scrollbar::scrubbable_range_in_pixels() const
{
    if (orientation() == Orientation::Vertical)
        return height() - button_height() * 2 - visible_scrubber_size();
    else
        return width() - button_width() * 2 - visible_scrubber_size();
}

bool Scrollbar::has_scrubber() const
{
    return max() != min();
}

void Scrollbar::set_scroll_animation(Animation scroll_animation)
{
    m_scroll_animation = scroll_animation;
}

void Scrollbar::set_value(int value, AllowCallback allow_callback, DoClamp do_clamp)
{
    m_target_value = value;
    if (!(m_animated_scrolling_timer.is_null()))
        m_animated_scrolling_timer->stop();

    AbstractSlider::set_value(value, allow_callback, do_clamp);
}

void Scrollbar::set_target_value(int new_target_value)
{
    new_target_value = clamp(new_target_value, min(), max());

    // If we are already at or scrolling to the new target then don't touch anything
    if (m_target_value == new_target_value)
        return;

    if (m_scroll_animation == Animation::CoarseScroll || !Desktop::the().system_effects().smooth_scrolling())
        return set_value(new_target_value);

    m_animation_time_elapsed = 0;
    m_start_value = value();
    m_target_value = new_target_value;

    if (m_animated_scrolling_timer.is_null()) {
        m_animated_scrolling_timer = add<Core::Timer>();
        m_animated_scrolling_timer->set_interval(ANIMATION_INTERVAL);
        m_animated_scrolling_timer->on_timeout = [this]() {
            m_animation_time_elapsed += (double)ANIMATION_INTERVAL / 1'000; // ms -> sec
            update_animated_scroll();
        };
    }

    m_animated_scrolling_timer->start();
}

float Scrollbar::unclamped_scrubber_size() const
{
    float pixel_range = length(orientation()) - button_size() * 2;
    float value_range = max() - min();

    float scrubber_size { 0 };
    if (value_range > 0) {
        // Scrubber size should be proportional to the visible portion
        // (page) in relation to the content (value range + page)
        scrubber_size = (page_step() * pixel_range) / (value_range + page_step());
    }
    return scrubber_size;
}

int Scrollbar::visible_scrubber_size() const
{
    return ::max(unclamped_scrubber_size(), button_size());
}

Gfx::IntRect Scrollbar::scrubber_rect() const
{
    if (!has_scrubber() || length(orientation()) <= (button_size() * 2) + visible_scrubber_size())
        return {};
    float x_or_y;
    if (value() == min())
        x_or_y = button_size();
    else if (value() == max())
        x_or_y = length(orientation()) - button_size() - visible_scrubber_size();
    else {
        float range_size = max() - min();
        float available = scrubbable_range_in_pixels();
        float step = available / range_size;
        x_or_y = (button_size() + (step * value()));
    }

    if (orientation() == Orientation::Vertical)
        return { 0, (int)x_or_y, button_width(), visible_scrubber_size() };
    else
        return { (int)x_or_y, 0, visible_scrubber_size(), button_height() };
}

void Scrollbar::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Component hovered_component_for_painting = m_hovered_component;
    if (!has_scrubber() || (m_pressed_component != Component::None && m_hovered_component != m_pressed_component))
        hovered_component_for_painting = Component::None;

    painter.fill_rect_with_dither_pattern(rect(), palette().button().lightened(1.3f), palette().button());
    if (m_gutter_click_state != GutterClickState::NotPressed && has_scrubber() && !scrubber_rect().is_empty() && hovered_component_for_painting == Component::Gutter) {
        Gfx::IntRect rect_to_fill = rect();
        if (orientation() == Orientation::Vertical) {
            if (m_gutter_click_state == GutterClickState::BeforeScrubber) {
                rect_to_fill.set_top(decrement_button_rect().bottom() - 1);
                rect_to_fill.set_bottom(scrubber_rect().top() + 1);
            } else {
                VERIFY(m_gutter_click_state == GutterClickState::AfterScrubber);
                rect_to_fill.set_top(scrubber_rect().bottom() - 1);
                rect_to_fill.set_bottom(increment_button_rect().top() + 1);
            }
        } else {
            if (m_gutter_click_state == GutterClickState::BeforeScrubber) {
                rect_to_fill.set_left(decrement_button_rect().right() - 1);
                rect_to_fill.set_right(scrubber_rect().left() + 1);
            } else {
                VERIFY(m_gutter_click_state == GutterClickState::AfterScrubber);
                rect_to_fill.set_left(scrubber_rect().right() - 1);
                rect_to_fill.set_right(increment_button_rect().left() + 1);
            }
        }
        painter.fill_rect_with_dither_pattern(rect_to_fill, palette().button(), palette().button().lightened(0.77f));
    }

    bool decrement_pressed = (m_pressed_component == Component::DecrementButton) && (m_pressed_component == m_hovered_component) && !is_min();
    bool increment_pressed = (m_pressed_component == Component::IncrementButton) && (m_pressed_component == m_hovered_component) && !is_max();

    Gfx::StylePainter::paint_button(painter, decrement_button_rect(), palette(), Gfx::ButtonStyle::ThickCap, decrement_pressed, hovered_component_for_painting == Component::DecrementButton && !is_min());
    Gfx::StylePainter::paint_button(painter, increment_button_rect(), palette(), Gfx::ButtonStyle::ThickCap, increment_pressed, hovered_component_for_painting == Component::IncrementButton && !is_max());

    if (length(orientation()) >= default_button_size() * 2) {
        auto decrement_location = decrement_button_rect().location().translated(3, 3);
        if (decrement_pressed)
            decrement_location.translate_by(1, 1);
        if (!has_scrubber() || !is_enabled() || is_min())
            painter.draw_triangle(decrement_location + Gfx::IntPoint { 1, 1 }, orientation() == Orientation::Vertical ? s_up_arrow_coords : s_left_arrow_coords, palette().threed_highlight());
        painter.draw_triangle(decrement_location, orientation() == Orientation::Vertical ? s_up_arrow_coords : s_left_arrow_coords, (has_scrubber() && is_enabled() && !is_min()) ? palette().button_text() : palette().threed_shadow1());

        auto increment_location = increment_button_rect().location().translated(3, 3);
        if (increment_pressed)
            increment_location.translate_by(1, 1);
        if (!has_scrubber() || !is_enabled() || is_max())
            painter.draw_triangle(increment_location + Gfx::IntPoint { 1, 1 }, orientation() == Orientation::Vertical ? s_down_arrow_coords : s_right_arrow_coords, palette().threed_highlight());
        painter.draw_triangle(increment_location, orientation() == Orientation::Vertical ? s_down_arrow_coords : s_right_arrow_coords, (has_scrubber() && is_enabled() && !is_max()) ? palette().button_text() : palette().threed_shadow1());
    }

    if (has_scrubber() && !scrubber_rect().is_empty())
        Gfx::StylePainter::paint_button(painter, scrubber_rect(), palette(), Gfx::ButtonStyle::ThickCap, false, hovered_component_for_painting == Component::Scrubber || m_pressed_component == Component::Scrubber);
}

void Scrollbar::automatic_scrolling_timer_did_fire()
{
    if (m_pressed_component == Component::DecrementButton && component_at_position(m_last_mouse_position) == Component::DecrementButton) {
        decrease_slider_by_steps(1);
        return;
    }
    if (m_pressed_component == Component::IncrementButton && component_at_position(m_last_mouse_position) == Component::IncrementButton) {
        increase_slider_by_steps(1);
        return;
    }
    if (m_pressed_component == Component::Gutter && component_at_position(m_last_mouse_position) == Component::Gutter) {
        scroll_by_page(m_last_mouse_position);
        if (m_hovered_component != component_at_position(m_last_mouse_position)) {
            m_hovered_component = component_at_position(m_last_mouse_position);
            if (m_hovered_component != Component::Gutter)
                m_gutter_click_state = GutterClickState::NotPressed;
            update();
        }
        return;
    }
    if (m_gutter_click_state != GutterClickState::NotPressed) {
        m_gutter_click_state = GutterClickState::NotPressed;
        update();
        return;
    }
}

void Scrollbar::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;
    if (!has_scrubber())
        return;

    m_last_mouse_position = event.position();
    m_pressed_component = component_at_position(m_last_mouse_position);

    if (m_pressed_component == Component::DecrementButton) {
        if (is_min())
            return;
        set_automatic_scrolling_timer_active(true, Component::DecrementButton);
        update();
        return;
    }
    if (m_pressed_component == Component::IncrementButton) {
        if (is_max())
            return;
        set_automatic_scrolling_timer_active(true, Component::IncrementButton);
        update();
        return;
    }

    if (event.shift()) {
        scroll_to_position(event.position());
        m_pressed_component = component_at_position(event.position());
        return;
    }
    if (m_pressed_component == Component::Scrubber) {
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
        update();
        return;
    }
    VERIFY(!event.shift());

    VERIFY(m_pressed_component == Component::Gutter);
    set_automatic_scrolling_timer_active(true, Component::Gutter);
    update();
}

void Scrollbar::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;
    set_automatic_scrolling_timer_active(false, Component::None);
    update();
}

void Scrollbar::mousewheel_event(MouseEvent& event)
{
    if (!is_scrollable())
        return;
    increase_slider_by_steps(event.wheel_delta_y());
    Widget::mousewheel_event(event);
}

void Scrollbar::set_automatic_scrolling_timer_active(bool active, Component pressed_component)
{
    m_pressed_component = pressed_component;
    if (m_pressed_component == Component::Gutter)
        m_automatic_scrolling_timer->set_interval(200);
    else
        m_automatic_scrolling_timer->set_interval(100);

    if (active) {
        automatic_scrolling_timer_did_fire();
        m_automatic_scrolling_timer->start();
    } else {
        m_automatic_scrolling_timer->stop();
        m_gutter_click_state = GutterClickState::NotPressed;
    }
}

void Scrollbar::scroll_by_page(Gfx::IntPoint click_position)
{
    float range_size = max() - min();
    float available = scrubbable_range_in_pixels();
    float rel_scrubber_size = unclamped_scrubber_size() / available;
    float page_increment = range_size * rel_scrubber_size;

    if (click_position.primary_offset_for_orientation(orientation()) < scrubber_rect().primary_offset_for_orientation(orientation())) {
        m_gutter_click_state = GutterClickState::BeforeScrubber;
        decrease_slider_by(page_increment);
    } else {
        m_gutter_click_state = GutterClickState::AfterScrubber;
        increase_slider_by(page_increment);
    }
}

void Scrollbar::scroll_to_position(Gfx::IntPoint click_position)
{
    float range_size = max() - min();
    float available = scrubbable_range_in_pixels();

    float x_or_y = ::max(0, click_position.primary_offset_for_orientation(orientation()) - button_width() - button_width() / 2);
    float rel_x_or_y = x_or_y / available;
    set_target_value(min() + rel_x_or_y * range_size);
}

Scrollbar::Component Scrollbar::component_at_position(Gfx::IntPoint position)
{
    if (scrubber_rect().contains(position))
        return Component::Scrubber;
    if (decrement_button_rect().contains(position))
        return Component::DecrementButton;
    if (increment_button_rect().contains(position))
        return Component::IncrementButton;
    if (rect().contains(position))
        return Component::Gutter;
    return Component::None;
}

void Scrollbar::mousemove_event(MouseEvent& event)
{
    if (!is_scrollable())
        return;

    m_last_mouse_position = event.position();

    auto old_hovered_component = m_hovered_component;
    m_hovered_component = component_at_position(m_last_mouse_position);
    if (old_hovered_component != m_hovered_component) {
        if (is_enabled())
            update();
    }
    if (m_pressed_component != Component::Scrubber)
        return;
    float delta = orientation() == Orientation::Vertical ? (event.y() - m_scrub_origin.y()) : (event.x() - m_scrub_origin.x());
    float scrubbable_range = scrubbable_range_in_pixels();
    float value_steps_per_scrubbed_pixel = (max() - min()) / scrubbable_range;
    float new_value = m_scrub_start_value + (value_steps_per_scrubbed_pixel * delta);
    set_value(new_value);
}

void Scrollbar::leave_event(Core::Event&)
{
    if (m_hovered_component != Component::None) {
        m_hovered_component = Component::None;
        if (is_enabled())
            update();
    }
}

void Scrollbar::change_event(Event& event)
{
    if (event.type() == Event::Type::EnabledChange) {
        if (!is_enabled())
            set_automatic_scrolling_timer_active(false, Component::None);
    }
    return Widget::change_event(event);
}

void Scrollbar::update_animated_scroll()
{
    if (value() == m_target_value) {
        m_animated_scrolling_timer->stop();
        return;
    }

    double time_percent = m_animation_time_elapsed / ANIMATION_TIME;
    double ease_percent = 1.0 - pow(1.0 - time_percent, 5.0); // Ease out quint
    double initial_distance = m_target_value - m_start_value;
    double new_distance = initial_distance * ease_percent;
    int new_value = m_start_value + (int)round(new_distance);
    AbstractSlider::set_value(new_value);
}

Optional<UISize> Scrollbar::calculated_min_size() const
{
    auto scrubber_and_gutter = default_button_size() + 1;
    if (orientation() == Gfx::Orientation::Vertical)
        return { { default_button_size(), 2 * default_button_size() + scrubber_and_gutter } };
    else
        return { { 2 * default_button_size() + scrubber_and_gutter, default_button_size() } };
}

Optional<UISize> Scrollbar::calculated_preferred_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { SpecialDimension::Shrink, SpecialDimension::Grow } };
    else
        return { { SpecialDimension::Grow, SpecialDimension::Shrink } };
}

}
