/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Window.h>

REGISTER_WIDGET(GUI, StackWidget);

namespace GUI {

void StackWidget::set_active_widget(Widget* widget)
{
    if (widget == m_active_widget)
        return;

    bool active_widget_had_focus = m_active_widget && m_active_widget->has_focus_within();

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(rect());
        if (active_widget_had_focus)
            m_active_widget->set_focus(true);
        m_active_widget->set_visible(true);
    }

    set_focus_proxy(m_active_widget);

    if (on_active_widget_change)
        on_active_widget_change(m_active_widget);
}

void StackWidget::resize_event(ResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect({ {}, event.size() });
}

void StackWidget::child_event(Core::ChildEvent& event)
{
    if (!event.child() || !is<Widget>(*event.child()))
        return Widget::child_event(event);
    auto& child = verify_cast<Widget>(*event.child());
    if (event.type() == Event::ChildAdded) {
        if (!m_active_widget)
            set_active_widget(&child);
        else if (m_active_widget != &child)
            child.set_visible(false);
    } else if (event.type() == Event::ChildRemoved) {
        if (m_active_widget == &child) {
            Widget* new_active_widget = nullptr;
            for_each_child_widget([&](auto& new_child) {
                new_active_widget = &new_child;
                return IterationDecision::Break;
            });
            set_active_widget(new_active_widget);
        }
    }
    Widget::child_event(event);
}

Optional<UISize> StackWidget::calculated_min_size() const
{
    return m_active_widget->calculated_min_size();
}

}
