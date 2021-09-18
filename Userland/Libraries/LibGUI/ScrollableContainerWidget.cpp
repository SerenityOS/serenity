/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Layout.h>
#include <LibGUI/ScrollableContainerWidget.h>

namespace GUI {

ScrollableContainerWidget::ScrollableContainerWidget()
{
}

ScrollableContainerWidget::~ScrollableContainerWidget()
{
}

void ScrollableContainerWidget::did_scroll()
{
    AbstractScrollableWidget::did_scroll();
    update_widget_position();
}

void ScrollableContainerWidget::update_widget_position()
{
    if (!m_widget)
        return;
    m_widget->move_to(-horizontal_scrollbar().value() + content_margins().left(), -vertical_scrollbar().value() + content_margins().top());
}

void ScrollableContainerWidget::update_widget_size()
{
    if (!m_widget)
        return;
    m_widget->do_layout();
    if (m_widget->is_shrink_to_fit() && m_widget->layout()) {
        auto new_size = Widget::content_size();
        auto preferred_size = m_widget->layout()->preferred_size();
        if (preferred_size.width() != -1)
            new_size.set_width(preferred_size.width());
        if (preferred_size.height() != -1)
            new_size.set_height(preferred_size.height());
        m_widget->resize(new_size);
        set_content_size(new_size);
    } else {
        auto inner_size = Widget::content_size();
        auto min_size = m_widget->min_size();
        auto new_size = Gfx::Size {
            max(inner_size.width(), min_size.width()),
            max(inner_size.height(), min_size.height())
        };
        m_widget->resize(new_size);
        set_content_size(new_size);
    }
}

void ScrollableContainerWidget::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    update_widget_position();
    update_widget_size();
}

void ScrollableContainerWidget::set_widget(GUI::Widget* widget)
{
    if (m_widget == widget)
        return;

    if (m_widget)
        remove_child(*m_widget);

    m_widget = widget;

    if (m_widget) {
        add_child(*m_widget);
        m_widget->move_to_back();
    }
    update_widget_size();
    update_widget_position();
}

}
