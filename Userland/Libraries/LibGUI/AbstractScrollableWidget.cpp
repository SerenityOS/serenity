/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/Scrollbar.h>

namespace GUI {

AbstractScrollableWidget::AbstractScrollableWidget()
{
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
        on_automatic_scrolling_timer_fired();
    };
}

AbstractScrollableWidget::~AbstractScrollableWidget()
{
}

void AbstractScrollableWidget::handle_wheel_event(MouseEvent& event, Widget& event_source)
{
    if (!m_scrollbars_enabled) {
        event.ignore();
        return;
    }
    // FIXME: The wheel delta multiplier should probably come from... somewhere?
    if (event.shift() || &event_source == m_horizontal_scrollbar.ptr()) {
        horizontal_scrollbar().set_value(horizontal_scrollbar().value() + event.wheel_delta() * 60);
    } else {
        vertical_scrollbar().set_value(vertical_scrollbar().value() + event.wheel_delta() * 20);
    }
}

void AbstractScrollableWidget::mousewheel_event(MouseEvent& event)
{
    handle_wheel_event(event, *this);
}

void AbstractScrollableWidget::custom_layout()
{
    auto inner_rect = frame_inner_rect_for_size(size());
    int height_wanted_by_horizontal_scrollbar = m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->min_height() : 0;
    int width_wanted_by_vertical_scrollbar = m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->min_width() : 0;

    m_vertical_scrollbar->set_relative_rect(
        inner_rect.right() + 1 - m_vertical_scrollbar->min_width(),
        inner_rect.top(),
        m_vertical_scrollbar->min_width(),
        inner_rect.height() - height_wanted_by_horizontal_scrollbar);

    m_horizontal_scrollbar->set_relative_rect(
        inner_rect.left(),
        inner_rect.bottom() + 1 - m_horizontal_scrollbar->min_height(),
        inner_rect.width() - width_wanted_by_vertical_scrollbar,
        m_horizontal_scrollbar->min_height());

    m_corner_widget->set_visible(m_vertical_scrollbar->is_visible() && m_horizontal_scrollbar->is_visible());
    if (m_corner_widget->is_visible()) {
        Gfx::IntRect corner_rect { m_horizontal_scrollbar->relative_rect().right() + 1, m_vertical_scrollbar->relative_rect().bottom() + 1, width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar() };
        m_corner_widget->set_relative_rect(corner_rect);
    }
}

void AbstractScrollableWidget::resize_event(ResizeEvent& event)
{
    Frame::resize_event(event);
    update_scrollbar_ranges();
}

Gfx::IntSize AbstractScrollableWidget::available_size() const
{
    auto inner_size = Widget::content_size();
    unsigned available_width = max(inner_size.width() - m_size_occupied_by_fixed_elements.width(), 0);
    unsigned available_height = max(inner_size.height() - m_size_occupied_by_fixed_elements.height(), 0);
    return { available_width, available_height };
}

Gfx::IntSize AbstractScrollableWidget::excess_size() const
{
    auto available_size = this->available_size();
    int excess_height = max(0, m_content_size.height() - available_size.height());
    int excess_width = max(0, m_content_size.width() - available_size.width());
    return { excess_width, excess_height };
}

void AbstractScrollableWidget::update_scrollbar_ranges()
{
    if (should_hide_unnecessary_scrollbars()) {
        if (excess_size().height() - height_occupied_by_horizontal_scrollbar() <= 0 && excess_size().width() - width_occupied_by_vertical_scrollbar() <= 0) {
            m_horizontal_scrollbar->set_visible(false);
            m_vertical_scrollbar->set_visible(false);
        } else {
            auto vertical_initial_visibility = m_vertical_scrollbar->is_visible();
            auto horizontal_initial_visibility = m_horizontal_scrollbar->is_visible();

            m_vertical_scrollbar->set_visible(excess_size().height() > 0);
            m_horizontal_scrollbar->set_visible(excess_size().width() > 0);

            if (m_vertical_scrollbar->is_visible() != vertical_initial_visibility)
                m_horizontal_scrollbar->set_visible(excess_size().width() > 0);
            if (m_horizontal_scrollbar->is_visible() != horizontal_initial_visibility)
                m_vertical_scrollbar->set_visible(excess_size().height() > 0);
        }
    }

    m_horizontal_scrollbar->set_range(0, excess_size().width());
    m_horizontal_scrollbar->set_page_step(visible_content_rect().width() - m_horizontal_scrollbar->step());

    m_vertical_scrollbar->set_range(0, excess_size().height());
    m_vertical_scrollbar->set_page_step(visible_content_rect().height() - m_vertical_scrollbar->step());
}

void AbstractScrollableWidget::set_content_size(const Gfx::IntSize& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    update_scrollbar_ranges();
}

void AbstractScrollableWidget::set_size_occupied_by_fixed_elements(const Gfx::IntSize& size)
{
    if (m_size_occupied_by_fixed_elements == size)
        return;
    m_size_occupied_by_fixed_elements = size;
    update_scrollbar_ranges();
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
    return Frame::content_margins() + Margins { 0, width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar(), 0 };
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

void AbstractScrollableWidget::scroll_into_view(const Gfx::IntRect& rect, Orientation orientation)
{
    if (orientation == Orientation::Vertical)
        return scroll_into_view(rect, false, true);
    return scroll_into_view(rect, true, false);
}

void AbstractScrollableWidget::scroll_into_view(const Gfx::IntRect& rect, bool scroll_horizontally, bool scroll_vertically)
{
    auto visible_content_rect = this->visible_content_rect();
    if (visible_content_rect.contains(rect))
        return;

    if (scroll_vertically) {
        if (rect.top() < visible_content_rect.top()) {
            m_vertical_scrollbar->set_value(rect.top());
        } else if (rect.top() > visible_content_rect.top() && rect.bottom() > visible_content_rect.bottom()) {
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height() + 1);
        }
    }
    if (scroll_horizontally) {
        if (rect.left() < visible_content_rect.left()) {
            m_horizontal_scrollbar->set_value(rect.left());
        } else if (rect.left() > visible_content_rect.left() && rect.right() > visible_content_rect.right()) {
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width() + 1);
        }
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

void AbstractScrollableWidget::set_automatic_scrolling_timer(bool active)
{
    if (active == m_active_scrolling_enabled)
        return;

    m_active_scrolling_enabled = active;

    if (active) {
        on_automatic_scrolling_timer_fired();
        m_automatic_scrolling_timer->start();
    } else {
        m_automatic_scrolling_timer->stop();
    }
}

Gfx::IntPoint AbstractScrollableWidget::automatic_scroll_delta_from_position(const Gfx::IntPoint& pos) const
{
    Gfx::IntPoint delta { 0, 0 };

    if (pos.y() < m_autoscroll_threshold)
        delta.set_y(clamp(-(m_autoscroll_threshold - pos.y()), -m_autoscroll_threshold, 0));
    else if (pos.y() > widget_inner_rect().height() - m_autoscroll_threshold)
        delta.set_y(clamp(m_autoscroll_threshold - (widget_inner_rect().height() - pos.y()), 0, m_autoscroll_threshold));

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
    rect.set_height(rect.height() - height_occupied_by_horizontal_scrollbar());
    return rect;
}

Gfx::IntPoint AbstractScrollableWidget::to_content_position(const Gfx::IntPoint& widget_position) const
{
    auto content_position = widget_position;
    content_position.translate_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    content_position.translate_by(-frame_thickness(), -frame_thickness());
    return content_position;
}

Gfx::IntPoint AbstractScrollableWidget::to_widget_position(const Gfx::IntPoint& content_position) const
{
    auto widget_position = content_position;
    widget_position.translate_by(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    widget_position.translate_by(frame_thickness(), frame_thickness());
    return widget_position;
}

}
