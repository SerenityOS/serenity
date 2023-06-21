/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Scrollbar.h>

namespace GUI {

AbstractScrollableWidget::AbstractScrollableWidget()
{
    REGISTER_READONLY_SIZE_PROPERTY("min_content_size", min_content_size);

    m_vertical_scrollbar = add<AbstractScrollableWidgetScrollbar>(*this, Orientation::Vertical);
    m_vertical_scrollbar->set_step(4);
    m_vertical_scrollbar->on_change = [this](int) {
        did_scroll();
        update();
    };

    m_horizontal_scrollbar = add<AbstractScrollableWidgetScrollbar>(*this, Orientation::Horizontal);
    m_horizontal_scrollbar->set_step(4);
    m_horizontal_scrollbar->set_page_step(30);
    m_horizontal_scrollbar->on_change = [this](int) {
        did_scroll();
        update();
    };

    m_corner_widget = add<Widget>();
    m_corner_widget->set_fill_with_background_color(true);

    m_automatic_scrolling_timer = add<Core::Timer>();
    m_automatic_scrolling_timer->set_interval(50);
    m_automatic_scrolling_timer->on_timeout = [this] {
        automatic_scrolling_timer_did_fire();
    };
}

void AbstractScrollableWidget::set_banner_widget(Widget* widget)
{
    if (m_banner_widget == widget)
        return;
    if (m_banner_widget)
        remove_child(*m_banner_widget);
    if (!widget)
        return;

    m_banner_widget = widget;
    add_child(*m_banner_widget);
}

void AbstractScrollableWidget::handle_wheel_event(MouseEvent& event, Widget& event_source)
{
    if (!m_scrollbars_enabled) {
        event.ignore();
        return;
    }

    int wheel_delta_x { 0 };
    bool vertical_scroll_hijacked { false };

    if (event.shift() || &event_source == m_horizontal_scrollbar.ptr()) {
        wheel_delta_x = event.wheel_delta_y();
        vertical_scroll_hijacked = true;
    }

    if (event.wheel_delta_x() != 0) {
        wheel_delta_x = event.wheel_delta_x();
    }

    if (wheel_delta_x != 0) {
        // FIXME: The wheel delta multiplier should probably come from... somewhere?
        horizontal_scrollbar().increase_slider_by(wheel_delta_x * 60);
    }

    if (!vertical_scroll_hijacked && event.wheel_delta_y() != 0) {
        vertical_scrollbar().increase_slider_by(event.wheel_delta_y() * 20);
    }
}

void AbstractScrollableWidget::mousewheel_event(MouseEvent& event)
{
    handle_wheel_event(event, *this);
}

void AbstractScrollableWidget::custom_layout()
{
    auto inner_rect = frame_inner_rect_for_size(size());
    int height_wanted_by_banner_widget = m_banner_widget && m_banner_widget->is_visible() ? m_banner_widget->effective_min_size().height().as_int() : 0;
    int height_wanted_by_horizontal_scrollbar = m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->effective_min_size().height().as_int() : 0;
    int width_wanted_by_vertical_scrollbar = m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->effective_min_size().width().as_int() : 0;

    if (m_banner_widget && m_banner_widget->is_visible()) {
        m_banner_widget->set_relative_rect(
            inner_rect.left(),
            inner_rect.top(),
            inner_rect.width(),
            height_wanted_by_banner_widget);
    }

    {
        int vertical_scrollbar_width = m_vertical_scrollbar->effective_min_size().width().as_int();
        m_vertical_scrollbar->set_relative_rect(
            inner_rect.right() - vertical_scrollbar_width,
            inner_rect.top() + height_wanted_by_banner_widget,
            vertical_scrollbar_width,
            inner_rect.height() - height_wanted_by_horizontal_scrollbar - height_wanted_by_banner_widget);
    }

    {
        int horizontal_scrollbar_height = m_horizontal_scrollbar->effective_min_size().height().as_int();
        m_horizontal_scrollbar->set_relative_rect(
            inner_rect.left(),
            inner_rect.bottom() - horizontal_scrollbar_height,
            inner_rect.width() - width_wanted_by_vertical_scrollbar,
            horizontal_scrollbar_height);
    }

    m_corner_widget->set_visible(m_vertical_scrollbar->is_visible() && m_horizontal_scrollbar->is_visible());
    if (m_corner_widget->is_visible()) {
        Gfx::IntRect corner_rect { m_horizontal_scrollbar->relative_rect().right(), m_vertical_scrollbar->relative_rect().bottom(), width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar() };
        m_corner_widget->set_relative_rect(corner_rect);
    }
}

void AbstractScrollableWidget::resize_event(ResizeEvent& event)
{
    Frame::resize_event(event);
    update_scrollbar_visibility();
    update_scrollbar_ranges();
}

Gfx::IntSize AbstractScrollableWidget::available_size() const
{
    auto inner_size = Widget::content_size();
    int available_width = max(inner_size.width() - m_size_occupied_by_fixed_elements.width(), 0);
    int available_height = max(inner_size.height() - m_size_occupied_by_fixed_elements.height(), 0);
    return { available_width, available_height };
}

Gfx::IntSize AbstractScrollableWidget::excess_size() const
{
    auto available_size = this->available_size();
    int excess_height = max(0, m_content_size.height() - available_size.height());
    int excess_width = max(0, m_content_size.width() - available_size.width());
    return { excess_width, excess_height };
}

void AbstractScrollableWidget::set_should_hide_unnecessary_scrollbars(bool should_hide_unnecessary_scrollbars)
{
    if (m_should_hide_unnecessary_scrollbars == should_hide_unnecessary_scrollbars)
        return;

    m_should_hide_unnecessary_scrollbars = should_hide_unnecessary_scrollbars;
    if (should_hide_unnecessary_scrollbars) {
        update_scrollbar_ranges();
    } else {
        m_horizontal_scrollbar->set_visible(m_scrollbars_enabled);
        m_vertical_scrollbar->set_visible(m_scrollbars_enabled);
    }
}

void AbstractScrollableWidget::update_scrollbar_ranges()
{
    m_horizontal_scrollbar->set_range(0, excess_size().width());
    m_horizontal_scrollbar->set_page_step(visible_content_rect().width() - m_horizontal_scrollbar->step());

    m_vertical_scrollbar->set_range(0, excess_size().height());
    m_vertical_scrollbar->set_page_step(visible_content_rect().height() - m_vertical_scrollbar->step());
    update_scrollbar_visibility();
}

void AbstractScrollableWidget::update_scrollbar_visibility()
{
    if (!m_scrollbars_enabled) {
        m_horizontal_scrollbar->set_visible(false);
        m_vertical_scrollbar->set_visible(false);
        return;
    }
    if (should_hide_unnecessary_scrollbars()) {
        // If there has not been a min_size set, the content_size can be used as a substitute
        auto effective_min_content_size = m_min_content_size;
        if (m_min_content_size == Gfx::IntSize {})
            effective_min_content_size = m_content_size;
        int horizontal_buffer = rect().width() - 2 * frame_thickness() - effective_min_content_size.width();
        int vertical_buffer = rect().height() - 2 * frame_thickness() - effective_min_content_size.height() - height_occupied_by_banner_widget();
        bool horizontal_scrollbar_should_be_visible = false, vertical_scrollbar_should_be_visible = false;
        vertical_scrollbar_should_be_visible = vertical_buffer < 0;
        if (vertical_scrollbar_should_be_visible)
            horizontal_buffer -= m_vertical_scrollbar->width();
        horizontal_scrollbar_should_be_visible = horizontal_buffer < 0;
        if (horizontal_scrollbar_should_be_visible)
            vertical_buffer -= m_horizontal_scrollbar->height();
        vertical_scrollbar_should_be_visible = vertical_buffer < 0;
        m_horizontal_scrollbar->set_visible(horizontal_scrollbar_should_be_visible);
        m_vertical_scrollbar->set_visible(vertical_scrollbar_should_be_visible);
    }
}

void AbstractScrollableWidget::set_content_size(Gfx::IntSize size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    update_scrollbar_ranges();
}

void AbstractScrollableWidget::set_min_content_size(Gfx::IntSize min_size)
{
    if (m_min_content_size == min_size)
        return;
    m_min_content_size = min_size;
    update_scrollbar_ranges();
}

void AbstractScrollableWidget::set_size_occupied_by_fixed_elements(Gfx::IntSize size)
{
    if (m_size_occupied_by_fixed_elements == size)
        return;
    m_size_occupied_by_fixed_elements = size;
    update_scrollbar_ranges();
}

int AbstractScrollableWidget::height_occupied_by_banner_widget() const
{
    return m_banner_widget && m_banner_widget->is_visible() ? m_banner_widget->height() : 0;
}

int AbstractScrollableWidget::height_occupied_by_horizontal_scrollbar() const
{
    return m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->height() : 0;
}

int AbstractScrollableWidget::width_occupied_by_vertical_scrollbar() const
{
    return m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->width() : 0;
}

Margins AbstractScrollableWidget::content_margins() const
{
    return Frame::content_margins() + Margins { height_occupied_by_banner_widget(), width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar(), 0 };
}

Gfx::IntRect AbstractScrollableWidget::visible_content_rect() const
{
    auto inner_size = Widget::content_size();
    Gfx::IntRect rect {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        min(m_content_size.width(), inner_size.width() - m_size_occupied_by_fixed_elements.width()),
        min(m_content_size.height(), inner_size.height() - m_size_occupied_by_fixed_elements.height())
    };
    if (rect.is_empty())
        return {};
    return rect;
}

void AbstractScrollableWidget::scroll_into_view(Gfx::IntRect const& rect, Orientation orientation)
{
    if (orientation == Orientation::Vertical)
        return scroll_into_view(rect, false, true);
    return scroll_into_view(rect, true, false);
}

void AbstractScrollableWidget::scroll_into_view(Gfx::IntRect const& rect, bool scroll_horizontally, bool scroll_vertically)
{
    auto visible_content_rect = this->visible_content_rect();
    if (visible_content_rect.contains(rect))
        return;

    if (scroll_vertically) {
        if (rect.top() < visible_content_rect.top())
            m_vertical_scrollbar->set_value(rect.top());
        else if (rect.top() > visible_content_rect.top() && rect.bottom() > visible_content_rect.bottom())
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());
    }
    if (scroll_horizontally) {
        if (rect.left() < visible_content_rect.left())
            m_horizontal_scrollbar->set_value(rect.left());
        else if (rect.left() > visible_content_rect.left() && rect.right() > visible_content_rect.right())
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());
    }
}

void AbstractScrollableWidget::set_scrollbars_enabled(bool scrollbars_enabled)
{
    if (m_scrollbars_enabled == scrollbars_enabled)
        return;
    m_scrollbars_enabled = scrollbars_enabled;
    m_vertical_scrollbar->set_visible(m_scrollbars_enabled);
    m_horizontal_scrollbar->set_visible(m_scrollbars_enabled);
    m_corner_widget->set_visible(m_scrollbars_enabled);
}

void AbstractScrollableWidget::scroll_to_top()
{
    scroll_into_view({}, Orientation::Vertical);
}

void AbstractScrollableWidget::scroll_to_bottom()
{
    scroll_into_view({ 0, content_height(), 0, 0 }, Orientation::Vertical);
}

void AbstractScrollableWidget::scroll_to_right()
{
    scroll_into_view({ content_width(), 0, 0, 0 }, Orientation::Horizontal);
}

void AbstractScrollableWidget::set_automatic_scrolling_timer_active(bool active)
{
    if (active == m_active_scrolling_enabled)
        return;

    m_active_scrolling_enabled = active;

    if (active) {
        automatic_scrolling_timer_did_fire();
        m_automatic_scrolling_timer->start();
    } else {
        m_automatic_scrolling_timer->stop();
    }
}

Gfx::IntPoint AbstractScrollableWidget::automatic_scroll_delta_from_position(Gfx::IntPoint pos) const
{
    Gfx::IntPoint delta { 0, 0 };

    if (pos.y() < m_autoscroll_threshold)
        delta.set_y(AK::min(pos.y() - m_autoscroll_threshold, 0));
    else if (pos.y() > widget_inner_rect().height() - m_autoscroll_threshold)
        delta.set_y(AK::max(pos.y() + m_autoscroll_threshold - widget_inner_rect().height(), 0));

    if (pos.x() < m_autoscroll_threshold)
        delta.set_x(clamp(-(m_autoscroll_threshold - pos.x()), -m_autoscroll_threshold, 0));
    else if (pos.x() > widget_inner_rect().width() - m_autoscroll_threshold)
        delta.set_x(clamp(m_autoscroll_threshold - (widget_inner_rect().width() - pos.x()), 0, m_autoscroll_threshold));

    return delta;
}

Gfx::IntRect AbstractScrollableWidget::widget_inner_rect() const
{
    auto rect = frame_inner_rect();
    rect.set_width(rect.width() - width_occupied_by_vertical_scrollbar());
    rect.set_height(rect.height() - height_occupied_by_horizontal_scrollbar() - height_occupied_by_banner_widget());
    rect.set_top(rect.top() + height_occupied_by_banner_widget());
    return rect;
}

Gfx::IntPoint AbstractScrollableWidget::to_content_position(Gfx::IntPoint widget_position) const
{
    auto content_position = widget_position;
    content_position.translate_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    content_position.translate_by(-frame_thickness(), -frame_thickness());
    return content_position;
}

Gfx::IntPoint AbstractScrollableWidget::to_widget_position(Gfx::IntPoint content_position) const
{
    auto widget_position = content_position;
    widget_position.translate_by(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    widget_position.translate_by(frame_thickness(), frame_thickness());
    return widget_position;
}

Optional<UISize> AbstractScrollableWidget::calculated_min_size() const
{
    auto vertical_scrollbar = m_vertical_scrollbar->effective_min_size().height().as_int();
    auto horizontal_scrollbar = m_horizontal_scrollbar->effective_min_size().width().as_int();
    auto banner = m_banner_widget && m_banner_widget->is_visible() ? m_banner_widget->effective_min_size().width().as_int() : 0;
    auto max_width = max(banner, horizontal_scrollbar + corner_widget().width() + frame_thickness() * 2);
    return { { max_width, vertical_scrollbar + corner_widget().height() + frame_thickness() * 2 + height_occupied_by_banner_widget() } };
}

}
