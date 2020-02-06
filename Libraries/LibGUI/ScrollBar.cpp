/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>

namespace GUI {

static const char* s_up_arrow_bitmap_data = {
    "         "
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "   ###   "
    "   ###   "
    "   ###   "
    "         "
};

static const char* s_down_arrow_bitmap_data = {
    "         "
    "   ###   "
    "   ###   "
    "   ###   "
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "
    "         "
};

static const char* s_left_arrow_bitmap_data = {
    "         "
    "    #    "
    "   ##    "
    "  ###### "
    " ####### "
    "  ###### "
    "   ##    "
    "    #    "
    "         "
};

static const char* s_right_arrow_bitmap_data = {
    "         "
    "    #    "
    "    ##   "
    " ######  "
    " ####### "
    " ######  "
    "    ##   "
    "    #    "
    "         "
};

static Gfx::CharacterBitmap* s_up_arrow_bitmap;
static Gfx::CharacterBitmap* s_down_arrow_bitmap;
static Gfx::CharacterBitmap* s_left_arrow_bitmap;
static Gfx::CharacterBitmap* s_right_arrow_bitmap;

ScrollBar::ScrollBar(Widget* parent)
    : ScrollBar(Orientation::Vertical, parent)
{
}

ScrollBar::ScrollBar(Orientation orientation, Widget* parent)
    : Widget(parent)
    , m_orientation(orientation)
{
    m_automatic_scrolling_timer = Core::Timer::construct(this);
    if (!s_up_arrow_bitmap)
        s_up_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_down_arrow_bitmap)
        s_down_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_left_arrow_bitmap)
        s_left_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_left_arrow_bitmap_data, 9, 9).leak_ref();
    if (!s_right_arrow_bitmap)
        s_right_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_right_arrow_bitmap_data, 9, 9).leak_ref();

    if (m_orientation == Orientation::Vertical) {
        set_preferred_size(15, 0);
    } else {
        set_preferred_size(0, 15);
    }

    m_automatic_scrolling_timer->set_interval(100);
    m_automatic_scrolling_timer->on_timeout = [this] {
        on_automatic_scrolling_timer_fired();
    };
}

ScrollBar::~ScrollBar()
{
}

void ScrollBar::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;

    m_min = min;
    m_max = max;

    int old_value = m_value;
    m_value = clamp(m_value, m_min, m_max);
    if (on_change && m_value != old_value)
        on_change(m_value);

    update();
}

void ScrollBar::set_value(int value)
{
    value = clamp(value, m_min, m_max);
    if (value == m_value)
        return;
    m_value = value;
    if (on_change)
        on_change(value);
    update();
}

Gfx::Rect ScrollBar::decrement_button_rect() const
{
    return { 0, 0, button_width(), button_height() };
}

Gfx::Rect ScrollBar::increment_button_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, height() - button_height(), button_width(), button_height() };
    else
        return { width() - button_width(), 0, button_width(), button_height() };
}

Gfx::Rect ScrollBar::decrement_gutter_rect() const
{
    if (orientation() == Orientation::Vertical)
        return { 0, button_height(), button_width(), scrubber_rect().top() - button_height() };
    else
        return { button_width(), 0, scrubber_rect().x() - button_width(), button_height() };
}

Gfx::Rect ScrollBar::increment_gutter_rect() const
{
    auto scrubber_rect = this->scrubber_rect();
    if (orientation() == Orientation::Vertical)
        return { 0, scrubber_rect.bottom() + 1, button_width(), height() - button_height() - scrubber_rect.bottom() - 1 };
    else
        return { scrubber_rect.right() + 1, 0, width() - button_width() - scrubber_rect.right() - 1, button_width() };
}

int ScrollBar::scrubbable_range_in_pixels() const
{
    if (orientation() == Orientation::Vertical)
        return height() - button_height() * 2 - scrubber_size();
    else
        return width() - button_width() * 2 - scrubber_size();
}

bool ScrollBar::has_scrubber() const
{
    return m_max != m_min;
}

int ScrollBar::scrubber_size() const
{
    int pixel_range = length(orientation()) - button_size() * 2;
    int value_range = m_max - m_min;
    return ::max(pixel_range - value_range, button_size());
}

Gfx::Rect ScrollBar::scrubber_rect() const
{
    if (!has_scrubber() || length(orientation()) <= (button_size() * 2) + scrubber_size())
        return {};
    float x_or_y;
    if (m_value == m_min)
        x_or_y = button_size();
    else if (m_value == m_max)
        x_or_y = (length(orientation()) - button_size() - scrubber_size()) + 1;
    else {
        float range_size = m_max - m_min;
        float available = scrubbable_range_in_pixels();
        float step = available / range_size;
        x_or_y = (button_size() + (step * m_value));
    }

    if (orientation() == Orientation::Vertical)
        return { 0, (int)x_or_y, button_width(), scrubber_size() };
    else
        return { (int)x_or_y, 0, scrubber_size(), button_height() };
}

void ScrollBar::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(rect(), palette().button().lightened());

    Gfx::StylePainter::paint_button(painter, decrement_button_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_hovered_component == Component::DecrementButton);
    Gfx::StylePainter::paint_button(painter, increment_button_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_hovered_component == Component::IncrementButton);

    if (length(orientation()) > default_button_size()) {
        painter.draw_bitmap(decrement_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_up_arrow_bitmap : *s_left_arrow_bitmap, has_scrubber() ? palette().button_text() : palette().threed_shadow1());
        painter.draw_bitmap(increment_button_rect().location().translated(3, 3), orientation() == Orientation::Vertical ? *s_down_arrow_bitmap : *s_right_arrow_bitmap, has_scrubber() ? palette().button_text() : palette().threed_shadow1());
    }

    if (has_scrubber())
        Gfx::StylePainter::paint_button(painter, scrubber_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_hovered_component == Component::Scrubber || m_scrubber_in_use);
}

void ScrollBar::on_automatic_scrolling_timer_fired()
{
    if (m_automatic_scrolling_direction == AutomaticScrollingDirection::Decrement) {
        set_value(value() - m_step);
        return;
    }
    if (m_automatic_scrolling_direction == AutomaticScrollingDirection::Increment) {
        set_value(value() + m_step);
        return;
    }
}

void ScrollBar::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;
    if (decrement_button_rect().contains(event.position())) {
        m_automatic_scrolling_direction = AutomaticScrollingDirection::Decrement;
        set_automatic_scrolling_active(true);
        return;
    }
    if (increment_button_rect().contains(event.position())) {
        m_automatic_scrolling_direction = AutomaticScrollingDirection::Increment;
        set_automatic_scrolling_active(true);
        return;
    }
    if (has_scrubber() && scrubber_rect().contains(event.position())) {
        m_scrubber_in_use = true;
        m_scrubbing = true;
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
        update();
        return;
    }

    if (has_scrubber()) {
        float range_size = m_max - m_min;
        float available = scrubbable_range_in_pixels();

        float x = ::max(0, event.position().x() - button_width() - button_width() / 2);
        float y = ::max(0, event.position().y() - button_height() - button_height() / 2);

        float rel_x = x / available;
        float rel_y = y / available;

        if (orientation() == Orientation::Vertical)
            set_value(m_min + rel_y * range_size);
        else
            set_value(m_min + rel_x * range_size);

        m_scrubbing = true;
        m_scrub_start_value = value();
        m_scrub_origin = event.position();
    }
}

void ScrollBar::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;
    m_scrubber_in_use = false;
    m_automatic_scrolling_direction = AutomaticScrollingDirection::None;
    set_automatic_scrolling_active(false);
    if (!m_scrubbing)
        return;
    m_scrubbing = false;
    update();
}

void ScrollBar::mousewheel_event(MouseEvent& event)
{
    if (!is_scrollable())
        return;
    set_value(value() + event.wheel_delta() * m_step);
    Widget::mousewheel_event(event);
}

void ScrollBar::set_automatic_scrolling_active(bool active)
{
    if (active) {
        on_automatic_scrolling_timer_fired();
        m_automatic_scrolling_timer->start();
    } else {
        m_automatic_scrolling_timer->stop();
    }
}

void ScrollBar::mousemove_event(MouseEvent& event)
{
    auto old_hovered_component = m_hovered_component;
    if (scrubber_rect().contains(event.position()))
        m_hovered_component = Component::Scrubber;
    else if (decrement_button_rect().contains(event.position()))
        m_hovered_component = Component::DecrementButton;
    else if (increment_button_rect().contains(event.position()))
        m_hovered_component = Component::IncrementButton;
    else if (rect().contains(event.position()))
        m_hovered_component = Component::Gutter;
    else
        m_hovered_component = Component::Invalid;
    if (old_hovered_component != m_hovered_component) {
        update();

        if (m_automatic_scrolling_direction == AutomaticScrollingDirection::Decrement)
            set_automatic_scrolling_active(m_hovered_component == Component::DecrementButton);
        else if (m_automatic_scrolling_direction == AutomaticScrollingDirection::Increment)
            set_automatic_scrolling_active(m_hovered_component == Component::IncrementButton);
    }
    if (!m_scrubbing)
        return;
    float delta = orientation() == Orientation::Vertical ? (event.y() - m_scrub_origin.y()) : (event.x() - m_scrub_origin.x());
    float scrubbable_range = scrubbable_range_in_pixels();
    float value_steps_per_scrubbed_pixel = (m_max - m_min) / scrubbable_range;
    float new_value = m_scrub_start_value + (value_steps_per_scrubbed_pixel * delta);
    set_value(new_value);
}

void ScrollBar::leave_event(Core::Event&)
{
    if (m_hovered_component != Component::Invalid) {
        m_hovered_component = Component::Invalid;
        update();
    }
}

void ScrollBar::change_event(Event& event)
{
    if (event.type() == Event::Type::EnabledChange) {
        if (!is_enabled())
            m_scrubbing = false;
    }
    return Widget::change_event(event);
}

}
