/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Scrollbar)

namespace GUI {

static const char* s_up_arrow_bitmap_data = {
    "         "
    "         "
    "         "
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "         "
    "         "
};

static const char* s_down_arrow_bitmap_data = {
    "         "
    "         "
    "         "
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "
    "         "
    "         "
};

static const char* s_left_arrow_bitmap_data = {
    "         "
    "     #   "
    "    ##   "
    "   ###   "
    "  ####   "
    "   ###   "
    "    ##   "
    "     #   "
    "         "
};

static const char* s_right_arrow_bitmap_data = {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "
};

static Gfx::CharacterBitmap* s_up_arrow_bitmap;
static Gfx::CharacterBitmap* s_down_arrow_bitmap;
static Gfx::CharacterBitmap* s_left_arrow_bitmap;
static Gfx::CharacterBitmap* s_right_arrow_bitmap;

Scrollbar::Scrollbar(Orientation orientation)
    : AbstractSlider(orientation)
{
    m_automatic_scrolling_timer = add<Core::Timer>();
    if (!s_up_arrow_bitmap)
        s_up_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_down_arrow_bitmap)
        s_down_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_left_arrow_bitmap)
        s_left_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_left_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_right_arrow_bitmap)
        s_right_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_right_arrow_bitmap_data, 9, 9).leak_ref();

    if (orientation == Orientation::Vertical) {
        set_fixed_width(16);
    } else {
        set_fixed_height(16);
    }

    m_automatic_scrolling_timer->set_interval(100);
    m_automatic_scrolling_timer->on_timeout = [this] {
        on_automatic_scrolling_timer_fired();
    };
}

Scrollbar::~Scrollbar()
{
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

    bool decrement_pressed = (m_pressed_component == Component::DecrementButton) && (m_pressed_component == m_hovered_component);
    bool increment_pressed = (m_pressed_component == Component::IncrementButton) && (m_pressed_component == m_hovered_component);

    Gfx::StylePainter::paint_button(painter, decrement_button_rect(), palette(), Gfx::ButtonStyle::ThickCap, decrement_pressed, hovered_component_for_painting == Component::DecrementButton);
    Gfx::StylePainter::paint_button(painter, increment_button_rect(), palette(), Gfx::ButtonStyle::ThickCap, increment_pressed, hovered_component_for_painting == Component::IncrementButton);

    if (length(orientation()) > default_button_size()) {
        auto decrement_location = decrement_button_rect().location().translated(3, 3);
        if (decrement_pressed)
            decrement_location.translate_by(1, 1);
        if (!has_scrubber() || !is_enabled())
            painter.draw_bitmap(decrement_location.translated(1, 1), orientation() == Orientation::Vertical ? *s_up_arrow_bitmap : *s_left_arrow_bitmap, palette().threed_highlight());
        painter.draw_bitmap(decrement_location, orientation() == Orientation::Vertical ? *s_up_arrow_bitmap : *s_left_arrow_bitmap, (has_scrubber() && is_enabled()) ? palette().button_text() : palette().threed_shadow1());

        auto increment_location = increment_button_rect().location().translated(3, 3);
        if (increment_pressed)
            increment_location.translate_by(1, 1);
        if (!has_scrubber() || !is_enabled())
            painter.draw_bitmap(increment_location.translated(1, 1), orientation() == Orientation::Vertical ? *s_down_arrow_bitmap : *s_right_arrow_bitmap, palette().threed_highlight());
        painter.draw_bitmap(increment_location, orientation() == Orientation::Vertical ? *s_down_arrow_bitmap : *s_right_arrow_bitmap, (has_scrubber() && is_enabled()) ? palette().button_text() : palette().threed_shadow1());
    }

    if (has_scrubber() && !scrubber_rect().is_null())
        Gfx::StylePainter::paint_button(painter, scrubber_rect(), palette(), Gfx::ButtonStyle::ThickCap, false, hovered_component_for_painting == Component::Scrubber || m_pressed_component == Component::Scrubber);
}

void Scrollbar::on_automatic_scrolling_timer_fired()
{
    if (m_pressed_component == Component::DecrementButton && component_at_position(m_last_mouse_position) == Component::DecrementButton) {
        set_value(value() - step());
        return;
    }
    if (m_pressed_component == Component::IncrementButton && component_at_position(m_last_mouse_position) == Component::IncrementButton) {
        set_value(value() + step());
        return;
    }
    if (m_pressed_component == Component::Gutter && component_at_position(m_last_mouse_position) == Component::Gutter) {
        scroll_by_page(m_last_mouse_position);
        m_hovered_component = component_at_position(m_last_mouse_position);
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
        set_automatic_scrolling_active(true, Component::DecrementButton);
        update();
        return;
    }
    if (m_pressed_component == Component::IncrementButton) {
        set_automatic_scrolling_active(true, Component::IncrementButton);
        update();
        return;
    }

    if (event.shift()) {
        scroll_to_position(event.position());
        m_pressed_component = component_at_position(event.position());
        VERIFY(m_pressed_component == Component::Scrubber);
    }
    if (m_pressed_component == Component::Scrubber) {
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
        update();
        return;
    }
    VERIFY(!event.shift());

    VERIFY(m_pressed_component == Component::Gutter);
    set_automatic_scrolling_active(true, Component::Gutter);
    update();
}

void Scrollbar::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;
    set_automatic_scrolling_active(false, Component::None);
    update();
}

void Scrollbar::mousewheel_event(MouseEvent& event)
{
    if (!is_scrollable())
        return;
    set_value(value() + event.wheel_delta() * step());
    Widget::mousewheel_event(event);
}

void Scrollbar::set_automatic_scrolling_active(bool active, Component pressed_component)
{
    m_pressed_component = pressed_component;
    if (m_pressed_component == Component::Gutter)
        m_automatic_scrolling_timer->set_interval(200);
    else
        m_automatic_scrolling_timer->set_interval(100);

    if (active) {
        on_automatic_scrolling_timer_fired();
        m_automatic_scrolling_timer->start();
    } else {
        m_automatic_scrolling_timer->stop();
    }
}

void Scrollbar::scroll_by_page(const Gfx::IntPoint& click_position)
{
    float range_size = max() - min();
    float available = scrubbable_range_in_pixels();
    float rel_scrubber_size = unclamped_scrubber_size() / available;
    float page_increment = range_size * rel_scrubber_size;

    if (click_position.primary_offset_for_orientation(orientation()) < scrubber_rect().primary_offset_for_orientation(orientation()))
        set_value(value() - page_increment);
    else
        set_value(value() + page_increment);
}

void Scrollbar::scroll_to_position(const Gfx::IntPoint& click_position)
{
    float range_size = max() - min();
    float available = scrubbable_range_in_pixels();

    float x_or_y = ::max(0, click_position.primary_offset_for_orientation(orientation()) - button_width() - button_width() / 2);
    float rel_x_or_y = x_or_y / available;
    set_value(min() + rel_x_or_y * range_size);
}

Scrollbar::Component Scrollbar::component_at_position(const Gfx::IntPoint& position)
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
            set_automatic_scrolling_active(false, Component::None);
    }
    return Widget::change_event(event);
}

}
